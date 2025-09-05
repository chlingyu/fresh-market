package com.freshmarket.order.integration;

import com.freshmarket.order.dto.CreateOrderRequest;
import com.freshmarket.order.dto.OrderItemRequest;
import com.freshmarket.order.dto.OrderResponse;
import com.freshmarket.order.entity.Order;
import com.freshmarket.order.enums.OrderStatus;
import com.freshmarket.order.repository.OrderRepository;
import com.freshmarket.order.service.OrderService;
import com.freshmarket.payment.dto.PaymentResponse;
import com.freshmarket.payment.entity.Payment;
import com.freshmarket.payment.enums.PaymentStatus;
import com.freshmarket.payment.event.PaymentSuccessEvent;
import com.freshmarket.payment.event.PaymentFailedEvent;
import com.freshmarket.payment.repository.PaymentRepository;
import com.freshmarket.payment.service.PaymentService;
import com.freshmarket.product.entity.Product;
import com.freshmarket.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 订单-支付模块集成测试
 * 测试订单创建、支付发起、支付事件处理的完整业务流程
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.test.database.replace=none",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
public class OrderPaymentIntegrationIT {

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    private Long testUserId;

    @BeforeEach
    void setUp() {
        testUserId = 1L;
    }
    
    private Product createTestProduct(String name) {
        Product product = new Product(name, "集成测试用商品", BigDecimal.valueOf(99.99), "测试分类");
        product.setStock(100);
        return productRepository.save(product);
    }

    @Test
    @DisplayName("测试完整的订单-支付业务流程")
    void testCompleteOrderPaymentFlow() {
        // 0. 创建测试商品
        Product testProduct = createTestProduct("测试商品-支付流程");
        
        // 1. 创建订单
        CreateOrderRequest orderRequest = createOrderRequest(testProduct.getId());
        OrderResponse orderResponse = orderService.createOrder(testUserId, orderRequest);

        assertThat(orderResponse).isNotNull();
        assertThat(orderResponse.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(orderResponse.getTotalAmount()).isEqualTo(BigDecimal.valueOf(199.98));

        // 验证订单已保存
        Optional<Order> savedOrder = orderRepository.findById(orderResponse.getId());
        assertThat(savedOrder).isPresent();
        assertThat(savedOrder.get().getStatus()).isEqualTo(OrderStatus.PENDING);

        // 验证库存已扣减
        Product updatedProduct = productRepository.findById(testProduct.getId()).get();
        assertThat(updatedProduct.getStock()).isEqualTo(98); // 100 - 2

        // 2. 发起支付
        PaymentResponse paymentResponse = orderService.initiatePayment(
                testUserId, orderResponse.getId(), "alipay");

        assertThat(paymentResponse).isNotNull();
        assertThat(paymentResponse.getOrderId()).isEqualTo(orderResponse.getId());
        assertThat(paymentResponse.getAmount()).isEqualTo(BigDecimal.valueOf(199.98));
        assertThat(paymentResponse.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertNotNull(paymentResponse.getPaymentNumber());

        // 验证支付记录已保存
        Optional<Payment> savedPayment = paymentRepository.findById(paymentResponse.getId());
        assertThat(savedPayment).isPresent();
        assertThat(savedPayment.get().getStatus()).isEqualTo(PaymentStatus.PENDING);

        // 3. 模拟支付回调 - 支付成功
        simulatePaymentCallback(paymentResponse.getPaymentNumber(), "success", "TXN_12345");

        // 等待事件处理
        waitForEventProcessing();

        // 4. 验证支付状态更新
        Payment updatedPayment = paymentRepository.findById(paymentResponse.getId()).get();
        assertThat(updatedPayment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(updatedPayment.getTransactionId()).isEqualTo("TXN_12345");

        // 5. 验证订单状态通过事件更新为已支付
        Order finalOrder = orderRepository.findById(orderResponse.getId()).get();
        assertThat(finalOrder.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    @DisplayName("测试支付失败时的业务流程")
    void testPaymentFailureFlow() {
        // 0. 创建测试商品
        Product testProduct = createTestProduct("测试商品-支付失败");
        
        // 1. 创建订单并发起支付
        CreateOrderRequest orderRequest = createOrderRequest(testProduct.getId());
        OrderResponse orderResponse = orderService.createOrder(testUserId, orderRequest);
        PaymentResponse paymentResponse = orderService.initiatePayment(
                testUserId, orderResponse.getId(), "wechat");

        // 2. 模拟支付回调 - 支付失败
        simulatePaymentCallback(paymentResponse.getPaymentNumber(), "failed", "余额不足");

        waitForEventProcessing();

        // 3. 验证支付状态为失败
        Payment updatedPayment = paymentRepository.findById(paymentResponse.getId()).get();
        assertThat(updatedPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(updatedPayment.getFailureReason()).isEqualTo("余额不足");

        // 4. 验证订单状态保持为待支付，允许重新支付
        Order finalOrder = orderRepository.findById(orderResponse.getId()).get();
        assertThat(finalOrder.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    @DisplayName("测试订单取消时的库存恢复")
    void testOrderCancellationFlow() {
        // 0. 创建测试商品
        Product testProduct = createTestProduct("测试商品-订单取消");
        
        // 1. 创建订单
        CreateOrderRequest orderRequest = createOrderRequest(testProduct.getId());
        OrderResponse orderResponse = orderService.createOrder(testUserId, orderRequest);

        // 验证库存已扣减
        Product afterOrderProduct = productRepository.findById(testProduct.getId()).get();
        assertThat(afterOrderProduct.getStock()).isEqualTo(98);

        // 2. 取消订单
        orderService.cancelOrder(testUserId, orderResponse.getId());

        // 3. 验证订单状态为已取消
        Order cancelledOrder = orderRepository.findById(orderResponse.getId()).get();
        assertThat(cancelledOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        // 4. 验证库存已恢复
        Product restoredProduct = productRepository.findById(testProduct.getId()).get();
        assertThat(restoredProduct.getStock()).isEqualTo(100);
    }

    @Test
    @DisplayName("测试支付取消事件处理")
    void testPaymentCancellationFlow() {
        // 0. 创建测试商品
        Product testProduct = createTestProduct("测试商品-支付取消");
        
        // 1. 创建订单并发起支付
        CreateOrderRequest orderRequest = createOrderRequest(testProduct.getId());
        OrderResponse orderResponse = orderService.createOrder(testUserId, orderRequest);
        PaymentResponse paymentResponse = orderService.initiatePayment(
                testUserId, orderResponse.getId(), "unionpay");

        // 2. 取消支付
        boolean cancelled = paymentService.cancelPayment(paymentResponse.getId());
        assertThat(cancelled).isTrue();

        waitForEventProcessing();

        // 3. 验证支付状态为已取消
        Payment cancelledPayment = paymentRepository.findById(paymentResponse.getId()).get();
        assertThat(cancelledPayment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);

        // 4. 验证订单状态保持为待支付
        Order order = orderRepository.findById(orderResponse.getId()).get();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    // 辅助方法

    private CreateOrderRequest createOrderRequest(Long productId) {
        OrderItemRequest orderItem = new OrderItemRequest();
        orderItem.setProductId(productId);
        orderItem.setQuantity(2);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setOrderItems(List.of(orderItem));
        request.setShippingAddress("测试地址：北京市朝阳区");
        request.setPhone("13800138000");
        request.setNotes("集成测试订单");

        return request;
    }

    private void simulatePaymentCallback(String paymentNumber, String status, String extraData) {
        Payment payment = paymentRepository.findByPaymentNumber(paymentNumber).get();
        
        if ("success".equals(status)) {
            // 更新支付状态
            payment.updateStatus(PaymentStatus.SUCCESS, extraData);
            paymentRepository.save(payment);
            
            // 发布支付成功事件，让OrderService的事件监听器处理订单状态更新
            eventPublisher.publishEvent(new PaymentSuccessEvent(
                    this, payment.getOrderId(), payment.getPaymentNumber(), extraData));
        } else {
            // 更新支付状态
            payment.markAsFailed(extraData);
            paymentRepository.save(payment);
            
            // 发布支付失败事件
            eventPublisher.publishEvent(new PaymentFailedEvent(
                    this, payment.getOrderId(), payment.getPaymentNumber(), extraData));
        }
    }

    private void waitForEventProcessing() {
        try {
            // 等待异步事件处理完成
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}