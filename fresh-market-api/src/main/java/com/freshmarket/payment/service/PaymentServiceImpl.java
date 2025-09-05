package com.freshmarket.payment.service;

import com.freshmarket.common.exception.ResourceNotFoundException;
import com.freshmarket.order.entity.Order;
import com.freshmarket.order.repository.OrderRepository;
import com.freshmarket.payment.dto.CreatePaymentRequest;
import com.freshmarket.payment.dto.PaymentCallbackRequest;
import com.freshmarket.payment.dto.PaymentResponse;
import com.freshmarket.payment.dto.PaymentStatusResponse;
import com.freshmarket.payment.entity.Payment;
import com.freshmarket.payment.enums.PaymentGateway;
import com.freshmarket.payment.enums.PaymentStatus;
import com.freshmarket.payment.event.PaymentSuccessEvent;
import com.freshmarket.payment.event.PaymentFailedEvent;
import com.freshmarket.payment.event.PaymentCancelledEvent;
import com.freshmarket.payment.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * 支付服务实现
 * 用于开发和测试环境，模拟真实的支付处理流程
 * 
 * @author Fresh Market Team
 */
@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentServiceImpl.class);
    
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Random random = new Random();

    public PaymentServiceImpl(PaymentRepository paymentRepository, OrderRepository orderRepository, 
                              ApplicationEventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        logger.info("Creating mock payment for order: {}", request.getOrderId());
        
        // 验证订单是否存在
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + request.getOrderId()));
        
        // 检查订单是否已有成功的支付记录
        Optional<Payment> existingPayment = paymentRepository.findByOrderIdAndStatus(
                request.getOrderId(), PaymentStatus.SUCCESS);
        if (existingPayment.isPresent()) {
            throw new IllegalStateException("Order " + request.getOrderId() + " already has successful payment");
        }
        
        // 将支付方式字符串转换为枚举
        PaymentGateway gateway;
        try {
            gateway = PaymentGateway.fromCode(request.getPaymentMethod());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported payment method: " + request.getPaymentMethod());
        }
        
        // 验证支付金额是否与订单金额匹配
        if (request.getAmount().compareTo(order.getTotalAmount()) != 0) {
            throw new IllegalArgumentException("Payment amount does not match order amount");
        }
        
        // 创建支付记录
        Payment payment = new Payment(request.getOrderId(), request.getAmount(), gateway);
        payment.setStatus(PaymentStatus.PENDING);
        
        // 保存支付记录
        Payment savedPayment = paymentRepository.save(payment);
        
        logger.info("Created mock payment with number: {} for order: {}", 
                    savedPayment.getPaymentNumber(), request.getOrderId());
        
        return PaymentResponse.fromEntity(savedPayment);
    }

    @Override
    public String handlePaymentCallback(PaymentCallbackRequest callbackData) {
        logger.info("Handling payment callback for payment number: {}", callbackData.getPaymentNumber());
        
        // 查找支付记录
        Optional<Payment> paymentOpt = paymentRepository.findByPaymentNumber(callbackData.getPaymentNumber());
        if (paymentOpt.isEmpty()) {
            logger.error("Payment not found for callback: {}", callbackData.getPaymentNumber());
            return "fail";
        }
        
        Payment payment = paymentOpt.get();
        
        // 验证回调签名（模拟实现）
        if (!verifyCallbackSignature(callbackData)) {
            logger.error("Invalid callback signature for payment: {}", callbackData.getPaymentNumber());
            return "fail";
        }
        
        // 更新支付状态
        try {
            if ("success".equalsIgnoreCase(callbackData.getStatus())) {
                payment.updateStatus(PaymentStatus.SUCCESS, callbackData.getTransactionId());
                payment.setGatewayResponse(callbackData.getRawData());
                
                paymentRepository.save(payment);
                
                // 发布支付成功事件
                eventPublisher.publishEvent(new PaymentSuccessEvent(
                        this, payment.getOrderId(), payment.getPaymentNumber(), 
                        callbackData.getTransactionId()));
                
            } else {
                payment.markAsFailed(callbackData.getFailureReason());
                payment.setGatewayResponse(callbackData.getRawData());
                
                paymentRepository.save(payment);
                
                // 发布支付失败事件
                eventPublisher.publishEvent(new PaymentFailedEvent(
                        this, payment.getOrderId(), payment.getPaymentNumber(),
                        callbackData.getFailureReason()));
            }
            
            logger.info("Updated payment status to {} for payment: {}", 
                        payment.getStatus(), payment.getPaymentNumber());
            
            return "success";
        } catch (Exception e) {
            logger.error("Failed to process payment callback", e);
            return "fail";
        }
    }

    @Override
    public PaymentStatusResponse getPaymentStatusByOrderId(Long orderId) {
        logger.debug("Getting payment status for order: {}", orderId);
        
        // 查找订单的最新支付记录
        Optional<Payment> paymentOpt = paymentRepository.findLatestByOrderId(orderId);
        if (paymentOpt.isEmpty()) {
            throw new ResourceNotFoundException("No payment found for order: " + orderId);
        }
        
        Payment payment = paymentOpt.get();
        return new PaymentStatusResponse(payment.getPaymentNumber(), orderId, payment.getStatus());
    }

    @Override
    public PaymentStatusResponse getPaymentStatusByPaymentNumber(String paymentNumber) {
        logger.debug("Getting payment status for payment number: {}", paymentNumber);
        
        Payment payment = paymentRepository.findByPaymentNumber(paymentNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentNumber));
        
        PaymentStatusResponse response = new PaymentStatusResponse(
                payment.getPaymentNumber(), payment.getOrderId(), payment.getStatus());
        response.setTransactionId(payment.getTransactionId());
        response.setFailureReason(payment.getFailureReason());
        
        return response;
    }

    @Override
    public boolean cancelPayment(Long paymentId) {
        logger.info("Cancelling payment: {}", paymentId);
        
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));
        
        if (!payment.isCancellable()) {
            throw new IllegalStateException("Payment cannot be cancelled, current status: " + payment.getStatus());
        }
        
        payment.setStatus(PaymentStatus.CANCELLED);
        paymentRepository.save(payment);
        
        eventPublisher.publishEvent(new PaymentCancelledEvent(
                this, payment.getOrderId(), payment.getPaymentNumber(), "用户取消支付"));
        
        logger.info("Successfully cancelled payment: {}", paymentId);
        return true;
    }

    @Override
    public List<PaymentResponse> getPaymentHistoryByOrderId(Long orderId) {
        logger.debug("Getting payment history for order: {}", orderId);
        
        List<Payment> payments = paymentRepository.findAllByOrderIdOrderByCreatedAtDesc(orderId);
        return payments.stream()
                .map(PaymentResponse::fromEntity)
                .toList();
    }

    @Override
    public Optional<PaymentResponse> getPaymentById(Long paymentId) {
        logger.debug("Getting payment by id: {}", paymentId);
        
        return paymentRepository.findById(paymentId)
                .map(PaymentResponse::fromEntity);
    }

    @Override
    public int processExpiredPayments() {
        logger.debug("Processing expired payments");
        
        List<Payment> expiredPayments = paymentRepository.findExpiredPayments(
                PaymentStatus.PENDING, Instant.now());
        
        int processedCount = 0;
        for (Payment payment : expiredPayments) {
            try {
                payment.setStatus(PaymentStatus.CANCELLED);
                payment.setFailureReason("Payment expired");
                paymentRepository.save(payment);
                processedCount++;
            } catch (Exception e) {
                logger.error("Failed to process expired payment: {}", payment.getId(), e);
            }
        }
        
        if (processedCount > 0) {
            logger.info("Processed {} expired payments", processedCount);
        }
        
        return processedCount;
    }

    @Override
    public boolean verifyCallbackSignature(PaymentCallbackRequest callbackData) {
        // 模拟签名验证逻辑
        // 在实际实现中，这里会验证第三方支付平台的签名
        return callbackData.getSign() != null && !callbackData.getSign().trim().isEmpty();
    }

    @Override
    public PaymentResponse syncPaymentStatus(Long paymentId) {
        logger.info("Syncing payment status for payment: {}", paymentId);
        
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));
        
        // 模拟同步状态（在实际实现中会调用第三方API）
        // 这里仅作为示例，不做实际状态更新
        
        return PaymentResponse.fromEntity(payment);
    }

    /**
     * 定时任务：模拟支付成功
     * 每10秒执行一次，随机将部分PENDING状态的支付记录更新为SUCCESS
     */
    @Scheduled(fixedRate = 10000) // 10秒执行一次
    public void simulatePaymentSuccess() {
        List<Payment> pendingPayments = paymentRepository.findByStatus(
                PaymentStatus.PENDING, 
                PageRequest.of(0, 10)).getContent();
        
        if (pendingPayments.isEmpty()) {
            return;
        }
        
        // 随机选择一些待支付记录进行模拟完成
        for (Payment payment : pendingPayments) {
            // 30% 的概率模拟支付成功
            if (random.nextDouble() < 0.3) {
                try {
                    // 生成模拟的交易流水号
                    String transactionId = "MOCK_TXN_" + System.currentTimeMillis() + "_" + payment.getId();
                    
                    payment.updateStatus(PaymentStatus.SUCCESS, transactionId);
                    payment.setGatewayResponse("{\"mock_response\":\"success\",\"transaction_id\":\"" + transactionId + "\"}");
                    
                    paymentRepository.save(payment);
                    
                    // 发布支付成功事件
                    eventPublisher.publishEvent(new PaymentSuccessEvent(
                            this, payment.getOrderId(), payment.getPaymentNumber(), transactionId));
                    
                    logger.info("Simulated payment success for payment: {} with transaction: {}", 
                                payment.getPaymentNumber(), transactionId);
                    
                } catch (Exception e) {
                    logger.error("Failed to simulate payment success for payment: {}", payment.getId(), e);
                }
            }
        }
    }
}