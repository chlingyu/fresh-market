package com.freshmarket.order.service;

import com.freshmarket.common.exception.ResourceNotFoundException;
import com.freshmarket.order.dto.CreateOrderRequest;
import com.freshmarket.order.dto.OrderItemRequest;
import com.freshmarket.order.dto.OrderResponse;
import com.freshmarket.order.dto.OrderSearchRequest;
import com.freshmarket.order.entity.Order;
import com.freshmarket.order.entity.OrderItem;
import com.freshmarket.order.enums.OrderStatus;
import com.freshmarket.order.repository.OrderRepository;
import com.freshmarket.payment.dto.PaymentResponse;
import com.freshmarket.payment.service.PaymentService;
import com.freshmarket.product.entity.Product;
import com.freshmarket.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 订单Service测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("订单Service测试")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;
    
    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private OrderService orderService;

    private Product testProduct1;
    private Product testProduct2;
    private Order testOrder;
    private CreateOrderRequest createOrderRequest;

    @BeforeEach
    void setUp() {
        testProduct1 = new Product();
        testProduct1.setId(1L);
        testProduct1.setName("测试商品1");
        testProduct1.setPrice(new BigDecimal("10.00"));
        testProduct1.setStock(100);
        testProduct1.setActive(true);

        testProduct2 = new Product();
        testProduct2.setId(2L);
        testProduct2.setName("测试商品2");
        testProduct2.setPrice(new BigDecimal("15.00"));
        testProduct2.setStock(50);
        testProduct2.setActive(true);

        testOrder = new Order(1L, "北京市朝阳区测试地址", "13800138000");
        testOrder.setId(1L);
        testOrder.setOrderNumber("ORD123456789");
        testOrder.setStatus(OrderStatus.PENDING);
        
        OrderItem item1 = new OrderItem(1L, "测试商品1", new BigDecimal("10.00"), 2);
        OrderItem item2 = new OrderItem(2L, "测试商品2", new BigDecimal("15.00"), 1);
        testOrder.addOrderItem(item1);
        testOrder.addOrderItem(item2);

        createOrderRequest = new CreateOrderRequest();
        createOrderRequest.setShippingAddress("北京市朝阳区测试地址");
        createOrderRequest.setPhone("13800138000");
        createOrderRequest.setNotes("测试订单备注");
        createOrderRequest.setOrderItems(List.of(
                new OrderItemRequest(1L, 2),
                new OrderItemRequest(2L, 1)
        ));
    }

    @Test
    @DisplayName("应该能成功创建订单")
    void shouldCreateOrder() {
        when(productRepository.findAllById(anyList())).thenReturn(List.of(testProduct1, testProduct2));
        when(productRepository.decreaseStock(1L, 2)).thenReturn(1);
        when(productRepository.decreaseStock(2L, 1)).thenReturn(1);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        OrderResponse result = orderService.createOrder(1L, createOrderRequest);

        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getShippingAddress()).isEqualTo("北京市朝阳区测试地址");
        assertThat(result.getPhone()).isEqualTo("13800138000");
        assertThat(result.getOrderItems()).hasSize(2);
        assertThat(result.getTotalAmount()).isEqualTo(new BigDecimal("35.00"));

        verify(productRepository).findAllById(List.of(1L, 2L));
        verify(productRepository).decreaseStock(1L, 2);
        verify(productRepository).decreaseStock(2L, 1);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("创建订单时商品不存在应该抛出异常")
    void shouldThrowExceptionWhenProductNotFound() {
        when(productRepository.findAllById(anyList())).thenReturn(List.of(testProduct1)); // 只返回一个商品

        assertThatThrownBy(() -> orderService.createOrder(1L, createOrderRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("部分商品不存在或已下架");
    }

    @Test
    @DisplayName("创建订单时商品已下架应该抛出异常")
    void shouldThrowExceptionWhenProductInactive() {
        testProduct2.setActive(false);
        when(productRepository.findAllById(anyList())).thenReturn(List.of(testProduct1, testProduct2));

        assertThatThrownBy(() -> orderService.createOrder(1L, createOrderRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("部分商品已下架，无法创建订单");
    }

    @Test
    @DisplayName("创建订单时库存不足应该抛出异常")
    void shouldThrowExceptionWhenInsufficientStock() {
        testProduct1.setStock(1); // 库存不足
        when(productRepository.findAllById(anyList())).thenReturn(List.of(testProduct1, testProduct2));

        assertThatThrownBy(() -> orderService.createOrder(1L, createOrderRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("库存不足");
    }

    @Test
    @DisplayName("应该能根据订单ID获取订单详情")
    void shouldGetOrderById() {
        when(orderRepository.findByUserIdAndId(1L, 1L)).thenReturn(Optional.of(testOrder));

        OrderResponse result = orderService.getOrderById(1L, 1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getOrderNumber()).isEqualTo("ORD123456789");
        verify(orderRepository).findByUserIdAndId(1L, 1L);
    }

    @Test
    @DisplayName("获取不存在的订单应该抛出异常")
    void shouldThrowExceptionWhenOrderNotFound() {
        when(orderRepository.findByUserIdAndId(1L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(1L, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Order not found with id: 1");
    }

    @Test
    @DisplayName("应该能根据订单编号获取订单")
    void shouldGetOrderByOrderNumber() {
        when(orderRepository.findByUserIdAndOrderNumber(1L, "ORD123456789")).thenReturn(Optional.of(testOrder));

        OrderResponse result = orderService.getOrderByOrderNumber(1L, "ORD123456789");

        assertThat(result).isNotNull();
        assertThat(result.getOrderNumber()).isEqualTo("ORD123456789");
        verify(orderRepository).findByUserIdAndOrderNumber(1L, "ORD123456789");
    }

    @Test
    @DisplayName("应该能搜索订单")
    void shouldSearchOrders() {
        OrderSearchRequest searchRequest = new OrderSearchRequest();
        searchRequest.setStatus(OrderStatus.PENDING);
        searchRequest.setPage(0);
        searchRequest.setSize(20);
        searchRequest.setSortBy("createdAt");
        searchRequest.setSortDir("desc");

        Pageable pageable = PageRequest.of(0, 20);
        Page<Order> orderPage = new PageImpl<>(List.of(testOrder), pageable, 1);

        when(orderRepository.findByComplexConditions(eq(1L), eq(OrderStatus.PENDING), 
                eq(null), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(orderPage);

        Page<OrderResponse> result = orderService.searchOrders(1L, searchRequest);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(OrderStatus.PENDING);
        verify(orderRepository).findByComplexConditions(eq(1L), eq(OrderStatus.PENDING), 
                eq(null), eq(null), eq(null), any(Pageable.class));
    }

    @Test
    @DisplayName("应该能更新订单状态")
    void shouldUpdateOrderStatus() {
        when(orderRepository.findByUserIdAndId(1L, 1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(testOrder)).thenReturn(testOrder);

        OrderResponse result = orderService.updateOrderStatus(1L, 1L, OrderStatus.PAID);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PAID);
        verify(orderRepository).findByUserIdAndId(1L, 1L);
        verify(orderRepository).save(testOrder);
    }

    @Test
    @DisplayName("更新订单状态为不合法状态应该抛出异常")
    void shouldThrowExceptionWhenInvalidStatusTransition() {
        testOrder.setStatus(OrderStatus.DELIVERED); // 已完成订单
        when(orderRepository.findByUserIdAndId(1L, 1L)).thenReturn(Optional.of(testOrder));

        assertThatThrownBy(() -> orderService.updateOrderStatus(1L, 1L, OrderStatus.PENDING))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("无法将订单状态从");
    }

    @Test
    @DisplayName("应该能取消订单")
    void shouldCancelOrder() {
        when(orderRepository.findByUserIdAndId(1L, 1L)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(testOrder)).thenReturn(testOrder);
        when(productRepository.increaseStock(1L, 2)).thenReturn(1);
        when(productRepository.increaseStock(2L, 1)).thenReturn(1);

        orderService.cancelOrder(1L, 1L);

        assertThat(testOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderRepository).findByUserIdAndId(1L, 1L);
        verify(productRepository).increaseStock(1L, 2);
        verify(productRepository).increaseStock(2L, 1);
        verify(orderRepository).save(testOrder);
    }

    @Test
    @DisplayName("取消不能取消的订单应该抛出异常")
    void shouldThrowExceptionWhenCancelInvalidOrder() {
        testOrder.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findByUserIdAndId(1L, 1L)).thenReturn(Optional.of(testOrder));

        assertThatThrownBy(() -> orderService.cancelOrder(1L, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("当前订单状态无法取消");
    }

    @Test
    @DisplayName("应该能发起订单支付")
    void shouldInitiatePayment() {
        PaymentResponse paymentResponse = new PaymentResponse();
        paymentResponse.setOrderId(1L);
        paymentResponse.setPaymentNumber("PAY123456");
        
        when(orderRepository.findByUserIdAndId(1L, 1L)).thenReturn(Optional.of(testOrder));
        when(paymentService.createPayment(any())).thenReturn(paymentResponse);

        PaymentResponse result = orderService.initiatePayment(1L, 1L, "ALIPAY");

        assertThat(result).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(1L);
        assertThat(result.getPaymentNumber()).isEqualTo("PAY123456");
        verify(orderRepository).findByUserIdAndId(1L, 1L);
        verify(paymentService).createPayment(any());
    }

    @Test
    @DisplayName("对非待支付订单发起支付应该抛出异常")
    void shouldThrowExceptionWhenInitiatePaymentForNonPendingOrder() {
        testOrder.setStatus(OrderStatus.PAID);
        when(orderRepository.findByUserIdAndId(1L, 1L)).thenReturn(Optional.of(testOrder));

        assertThatThrownBy(() -> orderService.initiatePayment(1L, 1L, "ALIPAY"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("只有待支付订单才能进行支付");
    }

    @Test
    @DisplayName("应该能获取用户订单统计")
    void shouldGetUserOrderStatistics() {
        List<Object[]> mockStatistics = List.of(
                new Object[]{OrderStatus.PENDING, 2L},
                new Object[]{OrderStatus.PAID, 3L},
                new Object[]{OrderStatus.CANCELLED, 1L}
        );
        when(orderRepository.countOrdersByUserIdAndStatus(1L)).thenReturn(mockStatistics);

        Map<OrderStatus, Long> result = orderService.getUserOrderStatistics(1L);

        assertThat(result).hasSize(3);
        assertThat(result.get(OrderStatus.PENDING)).isEqualTo(2L);
        assertThat(result.get(OrderStatus.PAID)).isEqualTo(3L);
        assertThat(result.get(OrderStatus.CANCELLED)).isEqualTo(1L);
        verify(orderRepository).countOrdersByUserIdAndStatus(1L);
    }
}