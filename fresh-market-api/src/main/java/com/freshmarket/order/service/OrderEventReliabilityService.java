package com.freshmarket.order.service;

import com.freshmarket.order.entity.Order;
import com.freshmarket.order.enums.OrderStatus;
import com.freshmarket.order.repository.OrderRepository;
import com.freshmarket.payment.event.PaymentSuccessEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * 订单事件可靠性服务
 * 处理支付成功事件与订单状态更新的最终一致性问题
 */
@Service
public class OrderEventReliabilityService {

    private static final Logger logger = LoggerFactory.getLogger(OrderEventReliabilityService.class);

    private final OrderRepository orderRepository;

    public OrderEventReliabilityService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * 可靠的支付成功处理
     * 使用重试机制确保订单状态最终能被正确更新
     */
    @Retryable(
        value = {Exception.class}, 
        maxAttempts = 5,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    @Transactional
    public void processPaymentSuccessReliably(PaymentSuccessEvent event) {
        logger.info("Processing payment success event for order: {} with retry support", event.getOrderId());

        Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new IllegalStateException("Order not found: " + event.getOrderId()));

        // 检查订单状态是否需要更新
        if (order.getStatus() != OrderStatus.PENDING) {
            logger.info("Order {} status is already {}, skipping update", 
                    event.getOrderId(), order.getStatus());
            return;
        }

        // 原子性更新订单状态
        int updatedRows = orderRepository.updateOrderStatusById(
                event.getOrderId(), 
                OrderStatus.PAID, 
                OrderStatus.PENDING
        );

        if (updatedRows == 0) {
            throw new IllegalStateException(
                    String.format("Failed to update order %d status from PENDING to PAID", event.getOrderId())
            );
        }

        logger.info("Successfully updated order {} status to PAID", event.getOrderId());
    }

    /**
     * 重试失败后的恢复处理
     * 将失败的事件记录到数据库，后续可以通过定时任务重新处理
     */
    @Recover
    @Transactional
    public void recoverFromPaymentSuccessFailure(Exception ex, PaymentSuccessEvent event) {
        logger.error("All retry attempts failed for payment success event, order: {}, error: {}", 
                event.getOrderId(), ex.getMessage(), ex);
        
        // 记录失败的事件到数据库，用于后续补偿
        recordFailedPaymentEvent(event, ex);
    }

    /**
     * 记录失败的支付事件
     */
    private void recordFailedPaymentEvent(PaymentSuccessEvent event, Exception ex) {
        try {
            // 这里可以插入到失败事件表，用于后续人工处理或定时任务重试
            // 由于当前没有失败事件表，先记录详细日志
            logger.error("CRITICAL: Failed to process payment success for order {}, payment {}, " +
                        "manual intervention may be required. Error: {}", 
                    event.getOrderId(), event.getPaymentNumber(), ex.getMessage());
            
            // TODO: 考虑发送告警通知运维团队
        } catch (Exception recordEx) {
            logger.error("Failed to record failed payment event", recordEx);
        }
    }

    /**
     * 检查并修复订单状态不一致问题
     * 定时任务可以调用此方法来修复可能存在的不一致状态
     */
    @Transactional(readOnly = true)
    public void checkAndFixOrderStatusInconsistency() {
        // 查找超过一定时间仍为PENDING状态但已有成功支付记录的订单
        Instant cutoffTime = Instant.now().minusSeconds(300); // 5分钟前
        
        // 这里需要与支付服务配合，查找已支付但订单状态未更新的情况
        logger.info("Checking for order status inconsistencies older than {}", cutoffTime);
        
        // TODO: 实现具体的不一致检查和修复逻辑
    }
}