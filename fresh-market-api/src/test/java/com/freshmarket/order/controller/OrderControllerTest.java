package com.freshmarket.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshmarket.common.exception.ResourceNotFoundException;
import com.freshmarket.config.TestSecurityConfig;
import com.freshmarket.order.dto.CreateOrderRequest;
import com.freshmarket.order.dto.OrderItemRequest;
import com.freshmarket.order.dto.OrderResponse;
import com.freshmarket.order.enums.OrderStatus;
import com.freshmarket.order.service.OrderService;
import com.freshmarket.payment.dto.PaymentResponse;
import com.freshmarket.payment.enums.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 订单Controller测试
 */
@WebMvcTest(OrderController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@DisplayName("订单Controller测试")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;
    
    @MockBean
    private com.freshmarket.security.JwtTokenProvider jwtTokenProvider;

    private OrderResponse testOrderResponse;
    private CreateOrderRequest createOrderRequest;
    
    private RequestPostProcessor mockAuthentication() {
        return request -> {
            Authentication auth = new UsernamePasswordAuthenticationToken("1", "password", List.of());
            request.setUserPrincipal(auth);
            return request;
        };
    }

    @BeforeEach
    void setUp() {
        testOrderResponse = new OrderResponse();
        testOrderResponse.setId(1L);
        testOrderResponse.setOrderNumber("ORD123456789");
        testOrderResponse.setUserId(1L);
        testOrderResponse.setStatus(OrderStatus.PENDING);
        testOrderResponse.setTotalAmount(new BigDecimal("35.00"));
        testOrderResponse.setShippingAddress("北京市朝阳区测试地址");
        testOrderResponse.setPhone("13800138000");
        testOrderResponse.setNotes("测试订单");
        testOrderResponse.setCreatedAt(Instant.now());
        testOrderResponse.setUpdatedAt(Instant.now());
        testOrderResponse.setOrderItems(List.of());

        createOrderRequest = new CreateOrderRequest();
        createOrderRequest.setShippingAddress("北京市朝阳区测试地址");
        createOrderRequest.setPhone("13800138000");
        createOrderRequest.setNotes("测试订单");
        createOrderRequest.setOrderItems(List.of(
                new OrderItemRequest(1L, 2),
                new OrderItemRequest(2L, 1)
        ));
    }

    @Test
    @DisplayName("应该能创建订单")
    void shouldCreateOrder() throws Exception {
        when(orderService.createOrder(eq(1L), any(CreateOrderRequest.class))).thenReturn(testOrderResponse);

        mockMvc.perform(post("/api/v1/orders")
                        .with(mockAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createOrderRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(1)))
                .andExpect(jsonPath("$.data.orderNumber", is("ORD123456789")))
                .andExpect(jsonPath("$.data.totalAmount", is(35.00)))
                .andExpect(jsonPath("$.data.status", is("PENDING")));

        verify(orderService).createOrder(eq(1L), any(CreateOrderRequest.class));
    }

    @Test
    @DisplayName("创建订单时验证失败应该返回400")
    void shouldReturn400WhenCreateOrderWithInvalidData() throws Exception {
        CreateOrderRequest invalidRequest = new CreateOrderRequest();
        // 缺少必填字段

        mockMvc.perform(post("/api/v1/orders")
                        .with(mockAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("应该能获取订单详情")
    void shouldGetOrder() throws Exception {
        when(orderService.getOrderById(1L, 1L)).thenReturn(testOrderResponse);

        mockMvc.perform(get("/api/v1/orders/1")
                        .with(mockAuthentication()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(1)))
                .andExpect(jsonPath("$.data.orderNumber", is("ORD123456789")));

        verify(orderService).getOrderById(1L, 1L);
    }

    @Test
    @DisplayName("获取不存在的订单应该返回404")
    void shouldReturn404WhenGetNonExistentOrder() throws Exception {
        when(orderService.getOrderById(1L, 1L)).thenThrow(new ResourceNotFoundException("Order not found with id: 1"));

        mockMvc.perform(get("/api/v1/orders/1")
                        .with(mockAuthentication()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("应该能根据订单编号获取订单")
    void shouldGetOrderByNumber() throws Exception {
        when(orderService.getOrderByOrderNumber(1L, "ORD123456789")).thenReturn(testOrderResponse);

        mockMvc.perform(get("/api/v1/orders/number/ORD123456789")
                        .with(mockAuthentication()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.orderNumber", is("ORD123456789")));

        verify(orderService).getOrderByOrderNumber(1L, "ORD123456789");
    }

    @Test
    @DisplayName("应该能搜索订单")
    void shouldSearchOrders() throws Exception {
        Page<OrderResponse> orderPage = new PageImpl<>(
                List.of(testOrderResponse), 
                PageRequest.of(0, 20), 
                1
        );
        when(orderService.searchOrders(eq(1L), any())).thenReturn(orderPage);

        mockMvc.perform(get("/api/v1/orders")
                        .with(mockAuthentication())
                        .param("status", "PENDING")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].status", is("PENDING")));

        verify(orderService).searchOrders(eq(1L), any());
    }

    @Test
    @DisplayName("应该能更新订单状态")
    void shouldUpdateOrderStatus() throws Exception {
        testOrderResponse.setStatus(OrderStatus.PAID);
        when(orderService.updateOrderStatus(1L, 1L, OrderStatus.PAID)).thenReturn(testOrderResponse);

        mockMvc.perform(put("/api/v1/orders/1/status")
                        .with(mockAuthentication())
                        .param("status", "PAID"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("PAID")));

        verify(orderService).updateOrderStatus(1L, 1L, OrderStatus.PAID);
    }

    @Test
    @DisplayName("更新订单状态为不合法状态应该返回400")
    void shouldReturn400WhenUpdateOrderStatusWithInvalidTransition() throws Exception {
        doThrow(new IllegalStateException("Invalid status transition")).when(orderService)
                .updateOrderStatus(1L, 1L, OrderStatus.DELIVERED);

        mockMvc.perform(put("/api/v1/orders/1/status")
                        .with(mockAuthentication())
                        .param("status", "DELIVERED"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("应该能发起订单支付")
    void shouldInitiatePayment() throws Exception {
        PaymentResponse paymentResponse = new PaymentResponse();
        paymentResponse.setId(1L);
        paymentResponse.setOrderId(1L);
        paymentResponse.setPaymentNumber("PAY123456789");
        paymentResponse.setStatus(PaymentStatus.PENDING);
        
        when(orderService.initiatePayment(1L, 1L, "ALIPAY")).thenReturn(paymentResponse);

        mockMvc.perform(post("/api/v1/orders/1/pay")
                        .param("paymentMethod", "ALIPAY")
                        .with(mockAuthentication()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.paymentNumber", is("PAY123456789")))
                .andExpect(jsonPath("$.data.status", is("PENDING")));

        verify(orderService).initiatePayment(1L, 1L, "ALIPAY");
    }

    @Test
    @DisplayName("应该能取消订单")
    void shouldCancelOrder() throws Exception {
        doNothing().when(orderService).cancelOrder(1L, 1L);

        mockMvc.perform(delete("/api/v1/orders/1")
                        .with(mockAuthentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        verify(orderService).cancelOrder(1L, 1L);
    }

    @Test
    @DisplayName("取消不能取消的订单应该返回400")
    void shouldReturn400WhenCancelInvalidOrder() throws Exception {
        doThrow(new IllegalStateException("Cannot cancel order")).when(orderService).cancelOrder(1L, 1L);

        mockMvc.perform(delete("/api/v1/orders/1")
                        .with(mockAuthentication()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("应该能获取订单统计信息")
    void shouldGetOrderStatistics() throws Exception {
        Map<OrderStatus, Long> statistics = Map.of(
                OrderStatus.PENDING, 2L,
                OrderStatus.PAID, 3L,
                OrderStatus.CANCELLED, 1L
        );
        when(orderService.getUserOrderStatistics(1L)).thenReturn(statistics);

        mockMvc.perform(get("/api/v1/orders/statistics")
                        .with(mockAuthentication()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.PENDING", is(2)))
                .andExpect(jsonPath("$.data.PAID", is(3)))
                .andExpect(jsonPath("$.data.CANCELLED", is(1)));

        verify(orderService).getUserOrderStatistics(1L);
    }

    @Test
    @DisplayName("未授权访问应该返回401")
    void shouldReturn401WhenUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createOrderRequest)))
                .andExpect(status().isUnauthorized());
    }
}