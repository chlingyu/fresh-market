package com.freshmarket.order.service;

import com.freshmarket.common.exception.ResourceNotFoundException;
import com.freshmarket.order.dto.CreateOrderRequest;
import com.freshmarket.order.dto.OrderItemResponse;
import com.freshmarket.order.dto.OrderResponse;
import com.freshmarket.order.dto.OrderSearchRequest;
import com.freshmarket.order.dto.OrderSummaryDto;
import com.freshmarket.order.entity.Order;
import com.freshmarket.order.mapper.OrderMapper;
import com.freshmarket.order.entity.OrderItem;
import com.freshmarket.order.enums.OrderStatus;
import com.freshmarket.order.repository.OrderRepository;
import com.freshmarket.payment.dto.CreatePaymentRequest;
import com.freshmarket.payment.dto.PaymentResponse;
import com.freshmarket.payment.enums.PaymentGateway;
import com.freshmarket.payment.event.PaymentFailedEvent;
import com.freshmarket.payment.event.PaymentSuccessEvent;
import com.freshmarket.payment.event.PaymentCancelledEvent;
import com.freshmarket.payment.service.PaymentService;
import com.freshmarket.product.entity.Product;
import com.freshmarket.inventory.service.InventoryService;
import com.freshmarket.inventory.service.InventoryService.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 订单服务
 */
@Service
@Transactional
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;
    private final PaymentService paymentService;
    private final OrderEventReliabilityService reliabilityService;
    private final OrderMapper orderMapper;

    public OrderService(OrderRepository orderRepository, InventoryService inventoryService, 
                       PaymentService paymentService, OrderEventReliabilityService reliabilityService,
                       OrderMapper orderMapper) {
        this.orderRepository = orderRepository;
        this.inventoryService = inventoryService;
        this.paymentService = paymentService;
        this.reliabilityService = reliabilityService;
        this.orderMapper = orderMapper;
    }

    /**
     * 创建订单
     */
    public OrderResponse createOrder(Long userId, CreateOrderRequest request) {
        logger.debug("Creating order for user: {}", userId);

        // 1. 验证和检查库存
        Map<Long, Integer> productQuantityMap = buildProductQuantityMap(request);
        InventoryCheckResult checkResult = inventoryService.checkInventory(productQuantityMap);
        
        if (!checkResult.allAvailable()) {
            String errorMsg = String.join("; ", checkResult.unavailableReasons());
            throw new IllegalArgumentException("订单创建失败: " + errorMsg);
        }

        // 2. 创建订单实体
        Order order = createOrderEntity(userId, request, checkResult.availableProducts());

        // 3. 预扣库存（原子操作）
        List<InventoryReservation> reservations = buildInventoryReservations(request, checkResult.availableProducts());
        InventoryReservationResult reservationResult = inventoryService.reserveStock(reservations);
        
        if (!reservationResult.success()) {
            String errorMsg = String.join("; ", reservationResult.failures());
            throw new IllegalStateException("库存预扣失败: " + errorMsg);
        }

        // 4. 保存订单
        Order savedOrder = orderRepository.save(order);
        logger.info("Order created successfully with ID: {}, Order Number: {}", 
                savedOrder.getId(), savedOrder.getOrderNumber());

        return orderMapper.toOrderResponse(savedOrder);
    }

    private Map<Long, Integer> buildProductQuantityMap(CreateOrderRequest request) {
        return request.getOrderItems().stream()
                .collect(Collectors.toMap(
                        item -> item.getProductId(),
                        item -> item.getQuantity(),
                        Integer::sum // 处理同一商品多次添加的情况
                ));
    }

    private Order createOrderEntity(Long userId, CreateOrderRequest request, Map<Long, Product> availableProducts) {
        Order order = new Order(userId, request.getShippingAddress(), request.getPhone());
        order.setNotes(request.getNotes());

        // 创建订单商品明细
        for (var orderItemRequest : request.getOrderItems()) {
            Product product = availableProducts.get(orderItemRequest.getProductId());
            OrderItem orderItem = new OrderItem(
                    product.getId(),
                    product.getName(),
                    product.getPrice(),
                    orderItemRequest.getQuantity()
            );
            order.addOrderItem(orderItem);
        }

        return order;
    }

    private List<InventoryReservation> buildInventoryReservations(CreateOrderRequest request, Map<Long, Product> availableProducts) {
        return request.getOrderItems().stream()
                .map(orderItemRequest -> {
                    Product product = availableProducts.get(orderItemRequest.getProductId());
                    return new InventoryReservation(
                            product.getId(),
                            product.getName(),
                            orderItemRequest.getQuantity(),
                            product.getVersion()
                    );
                })
                .toList();
    }

    /**
     * 获取订单详情
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long userId, Long orderId) {
        logger.debug("Getting order by ID: {} for user: {}", orderId, userId);

        Order order = orderRepository.findByUserIdAndId(userId, orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        return orderMapper.toOrderResponse(order);
    }

    /**
     * 根据订单编号获取订单
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrderByOrderNumber(Long userId, String orderNumber) {
        logger.debug("Getting order by number: {} for user: {}", orderNumber, userId);

        Order order = orderRepository.findByUserIdAndOrderNumber(userId, orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with number: " + orderNumber));

        return orderMapper.toOrderResponse(order);
    }

    /**
     * 搜索订单
     */
    @Transactional(readOnly = true)
    public Page<OrderResponse> searchOrders(Long userId, OrderSearchRequest searchRequest) {
        logger.debug("Searching orders for user: {} with criteria: {}", userId, searchRequest);

        Pageable pageable = createPageable(searchRequest);
        Page<Order> orders = orderRepository.findByComplexConditions(
                userId,
                searchRequest.getStatus(),
                searchRequest.getStartTime(),
                searchRequest.getEndTime(),
                searchRequest.getOrderNumberKeyword(),
                pageable
        );

        return orders.map(orderMapper::toOrderResponse);
    }

    /**
     * 搜索订单摘要（性能优化版本）
     * 用于订单列表页面，只返回必要的信息，提升查询性能
     */
    @Transactional(readOnly = true)
    public Page<OrderSummaryDto> searchOrderSummaries(Long userId, OrderSearchRequest searchRequest) {
        logger.debug("Searching order summaries for user: {} with criteria: {}", userId, searchRequest);

        Pageable pageable = createPageable(searchRequest);
        return orderRepository.findOrderSummariesByComplexConditions(
                userId,
                searchRequest.getStatus(),
                searchRequest.getStartTime(),
                searchRequest.getEndTime(),
                searchRequest.getOrderNumberKeyword(),
                pageable
        );
    }

    /**
     * 获取用户订单摘要列表（性能优化版本）
     */
    @Transactional(readOnly = true)
    public Page<OrderSummaryDto> getUserOrderSummaries(Long userId, Pageable pageable) {
        logger.debug("Getting order summaries for user: {}", userId);
        return orderRepository.findUserOrderSummaries(userId, pageable);
    }

    /**
     * 更新订单状态
     */
    public OrderResponse updateOrderStatus(Long userId, Long orderId, OrderStatus newStatus) {
        logger.debug("Updating order {} status to {} for user {}", orderId, newStatus, userId);

        Order order = orderRepository.findByUserIdAndId(userId, orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (!order.canUpdateStatus(newStatus)) {
            throw new IllegalStateException(String.format("无法将订单状态从 %s 更新为 %s", 
                    order.getStatus(), newStatus));
        }

        order.setStatus(newStatus);
        Order updatedOrder = orderRepository.save(order);

        logger.info("Order {} status updated to {}", orderId, newStatus);
        return mapEntityToResponse(updatedOrder);
    }

    /**
     * 取消订单
     */
    public void cancelOrder(Long userId, Long orderId) {
        logger.debug("Cancelling order {} for user {}", orderId, userId);

        Order order = orderRepository.findByUserIdAndId(userId, orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (!order.canUpdateStatus(OrderStatus.CANCELLED)) {
            throw new IllegalStateException("当前订单状态无法取消");
        }

        // 恢复库存 - 使用库存服务
        List<InventoryReservation> reservationsToRestore = order.getOrderItems().stream()
                .map(orderItem -> new InventoryReservation(
                        orderItem.getProductId(),
                        orderItem.getProductName(),
                        orderItem.getQuantity(),
                        null // 恢复库存时不需要版本检查
                ))
                .toList();

        inventoryService.restoreStock(reservationsToRestore);

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        logger.info("Order {} cancelled successfully", orderId);
    }

    /**
     * 发起订单支付
     * 创建支付记录并返回支付信息，订单状态通过支付事件异步更新
     */
    public PaymentResponse initiatePayment(Long userId, Long orderId, String paymentMethod) {
        logger.debug("Initiating payment for order {} by user {} with method {}", orderId, userId, paymentMethod);

        Order order = orderRepository.findByUserIdAndId(userId, orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("只有待支付订单才能进行支付");
        }

        // 创建支付请求
        CreatePaymentRequest paymentRequest = new CreatePaymentRequest(
                orderId, 
                order.getTotalAmount(), 
                paymentMethod
        );

        // 调用支付服务创建支付记录
        PaymentResponse paymentResponse = paymentService.createPayment(paymentRequest);
        
        logger.info("Payment initiated for order {} with payment number: {}", orderId, paymentResponse.getPaymentNumber());
        return paymentResponse;
    }

    /**
     * 获取用户订单统计
     */
    @Transactional(readOnly = true)
    public Map<OrderStatus, Long> getUserOrderStatistics(Long userId) {
        List<Object[]> statistics = orderRepository.countOrdersByUserIdAndStatus(userId);
        return statistics.stream()
                .collect(Collectors.toMap(
                        row -> (OrderStatus) row[0],
                        row -> (Long) row[1]
                ));
    }

    // 私有辅助方法

    private Pageable createPageable(OrderSearchRequest searchRequest) {
        Sort.Direction direction = "desc".equalsIgnoreCase(searchRequest.getSortDir()) 
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(direction, searchRequest.getSortBy());
        return PageRequest.of(searchRequest.getPage(), searchRequest.getSize(), sort);
    }

    // 支付事件监听器

    /**
     * 监听支付成功事件，更新订单状态为已支付
     * 使用可靠性服务确保最终一致性
     */
    @EventListener
    public void handlePaymentSuccess(PaymentSuccessEvent event) {
        logger.info("Delegating payment success event to reliability service for order: {}, payment: {}", 
                event.getOrderId(), event.getPaymentNumber());
        
        try {
            reliabilityService.processPaymentSuccessReliably(event);
        } catch (Exception e) {
            logger.error("Failed to delegate payment success event to reliability service for order: {}", 
                    event.getOrderId(), e);
            // 可靠性服务会处理重试和恢复，这里主要是记录委托失败的情况
        }
    }

    /**
     * 监听支付失败事件
     */
    @EventListener
    public void handlePaymentFailed(PaymentFailedEvent event) {
        logger.info("Handling payment failed event for order: {}, reason: {}", 
                event.getOrderId(), event.getFailureReason());
        
        // 支付失败时，订单保持PENDING状态，用户可以重新支付
        // 这里可以添加其他业务逻辑，如发送通知等
    }

    /**
     * 监听支付取消事件
     */
    @EventListener
    public void handlePaymentCancelled(PaymentCancelledEvent event) {
        logger.info("Handling payment cancelled event for order: {}, reason: {}", 
                event.getOrderId(), event.getCancelReason());
        
        // 支付取消时，订单保持PENDING状态
        // 这里可以添加其他业务逻辑，如发送通知等
    }
}