package com.freshmarket.payment.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshmarket.config.TestSecurityConfig;
import com.freshmarket.order.entity.Order;
import com.freshmarket.order.entity.OrderItem;
import com.freshmarket.order.enums.OrderStatus;
import com.freshmarket.order.repository.OrderRepository;
import com.freshmarket.payment.dto.CreatePaymentRequest;
import com.freshmarket.payment.dto.PaymentCallbackRequest;
import com.freshmarket.payment.entity.Payment;
import com.freshmarket.payment.enums.PaymentGateway;
import com.freshmarket.payment.enums.PaymentStatus;
import com.freshmarket.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 支付模块集成测试
 * 测试完整的支付流程，包括数据库交互
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
@Transactional
@DisplayName("支付模块集成测试")
class PaymentIntegrationIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    private Order testOrder;
    private CreatePaymentRequest createPaymentRequest;

    private RequestPostProcessor mockAuthentication() {
        return request -> {
            Authentication auth = new UsernamePasswordAuthenticationToken("1", "password", List.of());
            request.setUserPrincipal(auth);
            return request;
        };
    }

    @BeforeEach
    void setUp() {
        // 清理数据
        paymentRepository.deleteAll();
        orderRepository.deleteAll();

        // 创建测试订单
        testOrder = new Order(1L, "北京市朝阳区集成测试地址", "13800138000");
        testOrder.setStatus(OrderStatus.PENDING);
        testOrder.setNotes("集成测试订单");
        
        OrderItem orderItem = new OrderItem(1L, "集成测试商品", new BigDecimal("99.99"), 1);
        testOrder.addOrderItem(orderItem);
        
        testOrder = orderRepository.save(testOrder);

        // 创建支付请求
        createPaymentRequest = new CreatePaymentRequest(
                testOrder.getId(), 
                testOrder.getTotalAmount(), 
                "alipay"
        );
    }

    @Test
    @DisplayName("完整支付流程测试：创建->回调->查询")
    void shouldCompleteFullPaymentFlow() throws Exception {
        // Step 1: 创建支付
        String createResponse = mockMvc.perform(post("/api/v1/payments/orders/{orderId}/pay", testOrder.getId())
                        .with(mockAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createPaymentRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderId").value(testOrder.getId().intValue()))
                .andExpect(jsonPath("$.data.amount").value(99.99))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.gateway").value("ALIPAY"))
                .andReturn().getResponse().getContentAsString();

        // 从响应中提取支付单号
        String paymentNumber = objectMapper.readTree(createResponse)
                .get("data").get("paymentNumber").asText();

        // 验证数据库中的支付记录
        Payment savedPayment = paymentRepository.findByPaymentNumber(paymentNumber).orElse(null);
        assertThat(savedPayment).isNotNull();
        assertThat(savedPayment.getOrderId()).isEqualTo(testOrder.getId());
        assertThat(savedPayment.getAmount()).isEqualByComparingTo(new BigDecimal("99.99"));
        assertThat(savedPayment.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(savedPayment.getGateway()).isEqualTo(PaymentGateway.ALIPAY);

        // Step 2: 查询支付状态（应该是PENDING）
        mockMvc.perform(get("/api/v1/payments/{paymentNumber}/status", paymentNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paymentNumber").value(paymentNumber))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.isSuccess").value(false))
                .andExpect(jsonPath("$.data.isFinalState").value(false));

        // Step 3: 模拟支付成功回调
        PaymentCallbackRequest callbackRequest = new PaymentCallbackRequest();
        callbackRequest.setPaymentNumber(paymentNumber);
        callbackRequest.setStatus("success");
        callbackRequest.setTransactionId("TXN_INTEGRATION_TEST_123");
        callbackRequest.setSign("integration_test_signature");
        callbackRequest.setRawData("{\"integration_test\":\"success\"}");

        mockMvc.perform(post("/api/v1/payments/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(callbackRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string("success"));

        // Step 4: 验证支付状态已更新为成功
        Payment updatedPayment = paymentRepository.findByPaymentNumber(paymentNumber).orElse(null);
        assertThat(updatedPayment).isNotNull();
        assertThat(updatedPayment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(updatedPayment.getTransactionId()).isEqualTo("TXN_INTEGRATION_TEST_123");
        assertThat(updatedPayment.getPaidAt()).isNotNull();
        assertThat(updatedPayment.getGatewayResponse()).contains("integration_test");

        // Step 5: 再次查询支付状态（应该是SUCCESS）
        mockMvc.perform(get("/api/v1/payments/{paymentNumber}/status", paymentNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.isSuccess").value(true))
                .andExpect(jsonPath("$.data.isFinalState").value(true))
                .andExpect(jsonPath("$.data.transactionId").value("TXN_INTEGRATION_TEST_123"));

        // Step 6: 查询订单支付状态
        mockMvc.perform(get("/api/v1/payments/orders/{orderId}/status", testOrder.getId())
                        .with(mockAuthentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderId").value(testOrder.getId().intValue()))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));

        // Step 7: 查询支付历史
        mockMvc.perform(get("/api/v1/payments/orders/{orderId}/history", testOrder.getId())
                        .with(mockAuthentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].status").value("SUCCESS"));
    }

    @Test
    @DisplayName("支付失败流程测试")
    void shouldHandlePaymentFailureFlow() throws Exception {
        // Step 1: 创建支付
        String createResponse = mockMvc.perform(post("/api/v1/payments/orders/{orderId}/pay", testOrder.getId())
                        .with(mockAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createPaymentRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String paymentNumber = objectMapper.readTree(createResponse)
                .get("data").get("paymentNumber").asText();

        // Step 2: 模拟支付失败回调
        PaymentCallbackRequest failureCallback = new PaymentCallbackRequest();
        failureCallback.setPaymentNumber(paymentNumber);
        failureCallback.setStatus("failed");
        failureCallback.setFailureReason("余额不足");
        failureCallback.setSign("failure_signature");
        failureCallback.setRawData("{\"error\":\"insufficient_funds\"}");

        mockMvc.perform(post("/api/v1/payments/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(failureCallback)))
                .andExpect(status().isOk())
                .andExpect(content().string("success"));

        // Step 3: 验证支付状态已更新为失败
        Payment failedPayment = paymentRepository.findByPaymentNumber(paymentNumber).orElse(null);
        assertThat(failedPayment).isNotNull();
        assertThat(failedPayment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(failedPayment.getFailureReason()).isEqualTo("余额不足");

        // Step 4: 查询支付状态确认失败
        mockMvc.perform(get("/api/v1/payments/{paymentNumber}/status", paymentNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FAILED"))
                .andExpect(jsonPath("$.data.isSuccess").value(false))
                .andExpect(jsonPath("$.data.isFinalState").value(true))
                .andExpect(jsonPath("$.data.failureReason").value("余额不足"));
    }

    @Test
    @DisplayName("支付取消流程测试")
    void shouldHandlePaymentCancellationFlow() throws Exception {
        // Step 1: 创建支付
        String createResponse = mockMvc.perform(post("/api/v1/payments/orders/{orderId}/pay", testOrder.getId())
                        .with(mockAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createPaymentRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long paymentId = objectMapper.readTree(createResponse)
                .get("data").get("id").asLong();

        // Step 2: 取消支付
        mockMvc.perform(put("/api/v1/payments/{paymentId}/cancel", paymentId)
                        .with(mockAuthentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("Payment cancelled successfully"));

        // Step 3: 验证支付状态已更新为取消
        Payment cancelledPayment = paymentRepository.findById(paymentId).orElse(null);
        assertThat(cancelledPayment).isNotNull();
        assertThat(cancelledPayment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
    }

    @Test
    @DisplayName("重复支付检测测试")
    void shouldPreventDuplicatePayments() throws Exception {
        // Step 1: 创建第一个支付
        String response1 = mockMvc.perform(post("/api/v1/payments/orders/{orderId}/pay", testOrder.getId())
                        .with(mockAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createPaymentRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String paymentNumber = objectMapper.readTree(response1)
                .get("data").get("paymentNumber").asText();

        // Step 2: 模拟第一个支付成功
        PaymentCallbackRequest callbackRequest = new PaymentCallbackRequest();
        callbackRequest.setPaymentNumber(paymentNumber);
        callbackRequest.setStatus("success");
        callbackRequest.setTransactionId("TXN_SUCCESS_123");
        callbackRequest.setSign("valid_signature");

        mockMvc.perform(post("/api/v1/payments/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(callbackRequest)))
                .andExpect(status().isOk());

        // Step 3: 尝试创建第二个支付（应该失败）
        mockMvc.perform(post("/api/v1/payments/orders/{orderId}/pay", testOrder.getId())
                        .with(mockAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createPaymentRequest)))
                .andExpect(status().isConflict());

        // 验证只有一个支付记录
        List<Payment> payments = paymentRepository.findByOrderId(testOrder.getId());
        assertThat(payments).hasSize(1);
        assertThat(payments.get(0).getStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    @DisplayName("多种支付网关测试")
    void shouldSupportMultiplePaymentGateways() throws Exception {
        // 测试支付宝
        CreatePaymentRequest alipayRequest = new CreatePaymentRequest(
                testOrder.getId(), testOrder.getTotalAmount(), "alipay");
        
        mockMvc.perform(post("/api/v1/payments/orders/{orderId}/pay", testOrder.getId())
                        .with(mockAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(alipayRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.gateway").value("ALIPAY"));

        // 创建新订单测试微信支付
        Order wechatOrder = new Order(1L, "微信支付测试地址", "13800138001");
        wechatOrder.setStatus(OrderStatus.PENDING);
        OrderItem wechatItem = new OrderItem(2L, "微信支付商品", new BigDecimal("59.99"), 1);
        wechatOrder.addOrderItem(wechatItem);
        wechatOrder = orderRepository.save(wechatOrder);

        CreatePaymentRequest wechatRequest = new CreatePaymentRequest(
                wechatOrder.getId(), wechatOrder.getTotalAmount(), "wechat");

        mockMvc.perform(post("/api/v1/payments/orders/{orderId}/pay", wechatOrder.getId())
                        .with(mockAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wechatRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.gateway").value("WECHAT_PAY"));

        // 验证数据库中有两个不同网关的支付记录
        List<Payment> alipayPayments = paymentRepository.findByGateway(
                PaymentGateway.ALIPAY, org.springframework.data.domain.PageRequest.of(0, 10)).getContent();
        List<Payment> wechatPayments = paymentRepository.findByGateway(
                PaymentGateway.WECHAT_PAY, org.springframework.data.domain.PageRequest.of(0, 10)).getContent();
        
        assertThat(alipayPayments).hasSize(1);
        assertThat(wechatPayments).hasSize(1);
    }

    @Test
    @DisplayName("支付金额不匹配检测测试")
    void shouldDetectAmountMismatch() throws Exception {
        // 创建金额不匹配的支付请求
        CreatePaymentRequest mismatchRequest = new CreatePaymentRequest(
                testOrder.getId(), 
                new BigDecimal("199.99"), // 订单实际金额是99.99
                "alipay"
        );

        mockMvc.perform(post("/api/v1/payments/orders/{orderId}/pay", testOrder.getId())
                        .with(mockAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(mismatchRequest)))
                .andExpect(status().isBadRequest());

        // 验证没有创建支付记录
        List<Payment> payments = paymentRepository.findByOrderId(testOrder.getId());
        assertThat(payments).isEmpty();
    }

    @Test
    @DisplayName("支付同步状态测试")
    void shouldSyncPaymentStatus() throws Exception {
        // Step 1: 创建支付
        String createResponse = mockMvc.perform(post("/api/v1/payments/orders/{orderId}/pay", testOrder.getId())
                        .with(mockAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createPaymentRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long paymentId = objectMapper.readTree(createResponse)
                .get("data").get("id").asLong();

        // Step 2: 同步支付状态
        mockMvc.perform(post("/api/v1/payments/{paymentId}/sync", paymentId)
                        .with(mockAuthentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(paymentId.intValue()));
    }

    @Test
    @DisplayName("无效签名回调测试")
    void shouldRejectInvalidSignatureCallbacks() throws Exception {
        // Step 1: 创建支付
        String createResponse = mockMvc.perform(post("/api/v1/payments/orders/{orderId}/pay", testOrder.getId())
                        .with(mockAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createPaymentRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String paymentNumber = objectMapper.readTree(createResponse)
                .get("data").get("paymentNumber").asText();

        // Step 2: 发送无效签名的回调
        PaymentCallbackRequest invalidCallback = new PaymentCallbackRequest();
        invalidCallback.setPaymentNumber(paymentNumber);
        invalidCallback.setStatus("success");
        invalidCallback.setSign(""); // 无效签名

        mockMvc.perform(post("/api/v1/payments/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidCallback)))
                .andExpect(status().isOk())
                .andExpect(content().string("fail"));

        // Step 3: 验证支付状态未发生变化
        Payment unchangedPayment = paymentRepository.findByPaymentNumber(paymentNumber).orElse(null);
        assertThat(unchangedPayment).isNotNull();
        assertThat(unchangedPayment.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }
}