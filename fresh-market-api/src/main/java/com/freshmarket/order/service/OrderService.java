package com.freshmarket.order.service;

import com.freshmarket.common.exception.ResourceNotFoundException;
import com.freshmarket.order.dto.CreateOrderRequest;
import com.freshmarket.order.dto.OrderItemResponse;
import com.freshmarket.order.dto.OrderResponse;
import com.freshmarket.order.dto.OrderSearchRequest;
import com.freshmarket.order.entity.Order;
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
import com.freshmarket.product.repository.ProductRepository;
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
    private final ProductRepository productRepository;
    private final PaymentService paymentService;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository, PaymentService paymentService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.paymentService = paymentService;
    }

    /**
     * 创建订单
     */
    public OrderResponse createOrder(Long userId, CreateOrderRequest request) {
        logger.debug("Creating order for user: {}", userId);

        // 验证商品并获取商品信息
        List<Long> productIds = request.getOrderItems().stream()
                .map(item -> item.getProductId())
                .toList();
        
        List<Product> products = productRepository.findAllById(productIds);
        if (products.size() != productIds.size()) {
            throw new IllegalArgumentException("部分商品不存在或已下架");
        }

        Map<Long, Product> productMap = products.stream()
                .filter(Product::getActive)
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        if (productMap.size() != productIds.size()) {
            throw new IllegalArgumentException("部分商品已下架，无法创建订单");
        }

        // 检查库存
        for (var orderItemRequest : request.getOrderItems()) {
            Product product = productMap.get(orderItemRequest.getProductId());
            if (product.getStock() < orderItemRequest.getQuantity()) {
                throw new IllegalStateException(String.format("商品 %s 库存不足，可用库存：%d，需要：%d", 
                        product.getName(), product.getStock(), orderItemRequest.getQuantity()));
            }
        }

        // 创建订单
        Order order = new Order(userId, request.getShippingAddress(), request.getPhone());
        order.setNotes(request.getNotes());

        // 创建订单商品明细
        for (var orderItemRequest : request.getOrderItems()) {
            Product product = productMap.get(orderItemRequest.getProductId());
            OrderItem orderItem = new OrderItem(
                    product.getId(),
                    product.getName(),
                    product.getPrice(),
                    orderItemRequest.getQuantity()
            );
            order.addOrderItem(orderItem);
        }

        // 预扣库存
        for (var orderItemRequest : request.getOrderItems()) {
            int updatedCount = productRepository.decreaseStock(
                    orderItemRequest.getProductId(), 
                    orderItemRequest.getQuantity()
            );
            if (updatedCount == 0) {
                throw new IllegalStateException("库存扣减失败，可能存在并发问题");
            }
        }

        Order savedOrder = orderRepository.save(order);
        logger.info("Order created successfully with ID: {}, Order Number: {}", 
                savedOrder.getId(), savedOrder.getOrderNumber());

        return mapEntityToResponse(savedOrder);
    }

    /**
     * 获取订单详情
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long userId, Long orderId) {
        logger.debug("Getting order by ID: {} for user: {}", orderId, userId);

        Order order = orderRepository.findByUserIdAndId(userId, orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        return mapEntityToResponse(order);
    }

    /**
     * 根据订单编号获取订单
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrderByOrderNumber(Long userId, String orderNumber) {
        logger.debug("Getting order by number: {} for user: {}", orderNumber, userId);

        Order order = orderRepository.findByUserIdAndOrderNumber(userId, orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with number: " + orderNumber));

        return mapEntityToResponse(order);
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

        return orders.map(this::mapEntityToResponse);
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

        // 恢复库存
        for (OrderItem orderItem : order.getOrderItems()) {
            productRepository.increaseStock(orderItem.getProductId(), orderItem.getQuantity());
        }

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

    private OrderResponse mapEntityToResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setOrderNumber(order.getOrderNumber());
        response.setUserId(order.getUserId());
        response.setStatus(order.getStatus());
        response.setTotalAmount(order.getTotalAmount());
        response.setShippingAddress(order.getShippingAddress());
        response.setPhone(order.getPhone());
        response.setNotes(order.getNotes());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());

        // 映射订单商品列表
        List<OrderItemResponse> orderItemResponses = order.getOrderItems().stream()
                .map(this::mapOrderItemToResponse)
                .collect(Collectors.toList());
        response.setOrderItems(orderItemResponses);

        return response;
    }

    private OrderItemResponse mapOrderItemToResponse(OrderItem orderItem) {
        OrderItemResponse response = new OrderItemResponse();
        response.setId(orderItem.getId());
        response.setProductId(orderItem.getProductId());
        response.setProductName(orderItem.getProductName());
        response.setProductPrice(orderItem.getProductPrice());
        response.setQuantity(orderItem.getQuantity());
        response.setSubtotal(orderItem.getSubtotal());
        response.setCreatedAt(orderItem.getCreatedAt());
        return response;
    }

    private Pageable createPageable(OrderSearchRequest searchRequest) {
        Sort.Direction direction = "desc".equalsIgnoreCase(searchRequest.getSortDir()) 
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(direction, searchRequest.getSortBy());
        return PageRequest.of(searchRequest.getPage(), searchRequest.getSize(), sort);
    }

    // 支付事件监听器

    /**
     * 监听支付成功事件，更新订单状态为已支付
     */
    @EventListener
    @Transactional
    public void handlePaymentSuccess(PaymentSuccessEvent event) {
        logger.info("Handling payment success event for order: {}, payment: {}", 
                event.getOrderId(), event.getPaymentNumber());
        
        try {
            Order order = orderRepository.findById(event.getOrderId())
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + event.getOrderId()));
            
            if (order.getStatus() == OrderStatus.PENDING) {
                order.setStatus(OrderStatus.PAID);
                orderRepository.save(order);
                logger.info("Order {} status updated to PAID due to payment success", event.getOrderId());
            }
        } catch (Exception e) {
            logger.error("Failed to handle payment success event for order: {}", event.getOrderId(), e);
            // 这里可以考虑发布补偿事件或者加入重试队列
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