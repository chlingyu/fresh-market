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
import com.freshmarket.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PaymentServiceImpl单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("支付服务测试")
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private Order mockOrder;
    private Payment mockPayment;
    private CreatePaymentRequest validRequest;

    @BeforeEach
    void setUp() {
        // 创建模拟订单
        mockOrder = new Order();
        mockOrder.setId(1001L);
        mockOrder.setUserId(1L);
        mockOrder.setTotalAmount(new BigDecimal("99.99"));
        mockOrder.setShippingAddress("北京市朝阳区测试地址");
        mockOrder.setPhone("13800138000");

        // 创建模拟支付记录
        mockPayment = new Payment(1001L, new BigDecimal("99.99"), PaymentGateway.ALIPAY);
        mockPayment.setId(1L);
        mockPayment.setStatus(PaymentStatus.PENDING);
        mockPayment.setPaymentNumber("PAY123456789");

        // 创建有效的支付请求
        validRequest = new CreatePaymentRequest(1001L, new BigDecimal("99.99"), "alipay");
    }

    @Test
    @DisplayName("应该成功创建支付")
    void shouldCreatePaymentSuccessfully() {
        // Given
        when(orderRepository.findById(1001L)).thenReturn(Optional.of(mockOrder));
        when(paymentRepository.findByOrderIdAndStatus(1001L, PaymentStatus.SUCCESS))
                .thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment);

        // When
        PaymentResponse response = paymentService.createPayment(validRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isEqualTo(1001L);
        assertThat(response.getAmount()).isEqualTo(new BigDecimal("99.99"));
        assertThat(response.getGateway()).isEqualTo(PaymentGateway.ALIPAY);
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.PENDING);

        verify(orderRepository).findById(1001L);
        verify(paymentRepository).findByOrderIdAndStatus(1001L, PaymentStatus.SUCCESS);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    @DisplayName("订单不存在时应该抛出异常")
    void shouldThrowExceptionWhenOrderNotFound() {
        // Given
        when(orderRepository.findById(1001L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paymentService.createPayment(validRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order not found with id: 1001");

        verify(orderRepository).findById(1001L);
        verifyNoInteractions(paymentRepository);
    }

    @Test
    @DisplayName("订单已有成功支付记录时应该抛出异常")
    void shouldThrowExceptionWhenOrderAlreadyPaid() {
        // Given
        Payment existingPayment = new Payment(1001L, new BigDecimal("99.99"), PaymentGateway.ALIPAY);
        existingPayment.setStatus(PaymentStatus.SUCCESS);
        
        when(orderRepository.findById(1001L)).thenReturn(Optional.of(mockOrder));
        when(paymentRepository.findByOrderIdAndStatus(1001L, PaymentStatus.SUCCESS))
                .thenReturn(Optional.of(existingPayment));

        // When & Then
        assertThatThrownBy(() -> paymentService.createPayment(validRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Order 1001 already has successful payment");

        verify(orderRepository).findById(1001L);
        verify(paymentRepository).findByOrderIdAndStatus(1001L, PaymentStatus.SUCCESS);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("支付金额与订单金额不匹配时应该抛出异常")
    void shouldThrowExceptionWhenAmountMismatch() {
        // Given
        CreatePaymentRequest requestWithWrongAmount = new CreatePaymentRequest(
                1001L, new BigDecimal("199.99"), "alipay");
        
        when(orderRepository.findById(1001L)).thenReturn(Optional.of(mockOrder));
        when(paymentRepository.findByOrderIdAndStatus(1001L, PaymentStatus.SUCCESS))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paymentService.createPayment(requestWithWrongAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Payment amount does not match order amount");
    }

    @Test
    @DisplayName("不支持的支付方式应该抛出异常")
    void shouldThrowExceptionForUnsupportedPaymentMethod() {
        // Given
        CreatePaymentRequest requestWithInvalidMethod = new CreatePaymentRequest(
                1001L, new BigDecimal("99.99"), "invalid_method");
        
        when(orderRepository.findById(1001L)).thenReturn(Optional.of(mockOrder));
        when(paymentRepository.findByOrderIdAndStatus(1001L, PaymentStatus.SUCCESS))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paymentService.createPayment(requestWithInvalidMethod))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported payment method: invalid_method");
    }

    @Test
    @DisplayName("应该成功处理支付回调")
    void shouldHandlePaymentCallbackSuccessfully() {
        // Given
        PaymentCallbackRequest callbackRequest = new PaymentCallbackRequest();
        callbackRequest.setPaymentNumber("PAY123456789");
        callbackRequest.setStatus("success");
        callbackRequest.setTransactionId("TXN123456789");
        callbackRequest.setSign("valid_signature");
        callbackRequest.setRawData("{\"result\":\"success\"}");

        when(paymentRepository.findByPaymentNumber("PAY123456789"))
                .thenReturn(Optional.of(mockPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment);

        // When
        String result = paymentService.handlePaymentCallback(callbackRequest);

        // Then
        assertThat(result).isEqualTo("success");
        verify(paymentRepository).findByPaymentNumber("PAY123456789");
        verify(paymentRepository).save(mockPayment);
    }

    @Test
    @DisplayName("支付记录不存在时回调应该返回失败")
    void shouldReturnFailWhenPaymentNotFoundInCallback() {
        // Given
        PaymentCallbackRequest callbackRequest = new PaymentCallbackRequest();
        callbackRequest.setPaymentNumber("NONEXISTENT");
        callbackRequest.setStatus("success");
        callbackRequest.setSign("valid_signature");

        when(paymentRepository.findByPaymentNumber("NONEXISTENT"))
                .thenReturn(Optional.empty());

        // When
        String result = paymentService.handlePaymentCallback(callbackRequest);

        // Then
        assertThat(result).isEqualTo("fail");
        verify(paymentRepository).findByPaymentNumber("NONEXISTENT");
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("签名无效时回调应该返回失败")
    void shouldReturnFailWhenSignatureInvalid() {
        // Given
        PaymentCallbackRequest callbackRequest = new PaymentCallbackRequest();
        callbackRequest.setPaymentNumber("PAY123456789");
        callbackRequest.setStatus("success");
        callbackRequest.setSign(""); // 空签名，无效

        when(paymentRepository.findByPaymentNumber("PAY123456789"))
                .thenReturn(Optional.of(mockPayment));

        // When
        String result = paymentService.handlePaymentCallback(callbackRequest);

        // Then
        assertThat(result).isEqualTo("fail");
        verify(paymentRepository).findByPaymentNumber("PAY123456789");
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("应该根据订单ID查询支付状态")
    void shouldGetPaymentStatusByOrderId() {
        // Given
        when(paymentRepository.findLatestByOrderId(1001L))
                .thenReturn(Optional.of(mockPayment));

        // When
        PaymentStatusResponse response = paymentService.getPaymentStatusByOrderId(1001L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isEqualTo(1001L);
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(response.getPaymentNumber()).isEqualTo("PAY123456789");

        verify(paymentRepository).findLatestByOrderId(1001L);
    }

    @Test
    @DisplayName("订单没有支付记录时应该抛出异常")
    void shouldThrowExceptionWhenNoPaymentFoundForOrder() {
        // Given
        when(paymentRepository.findLatestByOrderId(1001L))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paymentService.getPaymentStatusByOrderId(1001L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("No payment found for order: 1001");
    }

    @Test
    @DisplayName("应该根据支付单号查询支付状态")
    void shouldGetPaymentStatusByPaymentNumber() {
        // Given
        mockPayment.setTransactionId("TXN123456789");
        mockPayment.setFailureReason("测试失败原因");
        
        when(paymentRepository.findByPaymentNumber("PAY123456789"))
                .thenReturn(Optional.of(mockPayment));

        // When
        PaymentStatusResponse response = paymentService.getPaymentStatusByPaymentNumber("PAY123456789");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getPaymentNumber()).isEqualTo("PAY123456789");
        assertThat(response.getOrderId()).isEqualTo(1001L);
        assertThat(response.getTransactionId()).isEqualTo("TXN123456789");
        assertThat(response.getFailureReason()).isEqualTo("测试失败原因");
    }

    @Test
    @DisplayName("应该成功取消支付")
    void shouldCancelPaymentSuccessfully() {
        // Given
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(mockPayment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(mockPayment);

        // When
        boolean result = paymentService.cancelPayment(1L);

        // Then
        assertThat(result).isTrue();
        verify(paymentRepository).findById(1L);
        verify(paymentRepository).save(mockPayment);
        assertThat(mockPayment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
    }

    @Test
    @DisplayName("不可取消的支付状态应该抛出异常")
    void shouldThrowExceptionWhenPaymentNotCancellable() {
        // Given
        mockPayment.setStatus(PaymentStatus.SUCCESS); // 成功状态不能取消
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(mockPayment));

        // When & Then
        assertThatThrownBy(() -> paymentService.cancelPayment(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Payment cannot be cancelled, current status: SUCCESS");
    }

    @Test
    @DisplayName("应该查询订单的支付历史")
    void shouldGetPaymentHistoryByOrderId() {
        // Given
        Payment payment1 = new Payment(1001L, new BigDecimal("99.99"), PaymentGateway.ALIPAY);
        payment1.setId(1L);
        payment1.setStatus(PaymentStatus.FAILED);
        
        Payment payment2 = new Payment(1001L, new BigDecimal("99.99"), PaymentGateway.WECHAT_PAY);
        payment2.setId(2L);
        payment2.setStatus(PaymentStatus.SUCCESS);

        when(paymentRepository.findAllByOrderIdOrderByCreatedAtDesc(1001L))
                .thenReturn(Arrays.asList(payment2, payment1)); // 按时间倒序

        // When
        List<PaymentResponse> history = paymentService.getPaymentHistoryByOrderId(1001L);

        // Then
        assertThat(history).hasSize(2);
        assertThat(history.get(0).getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(history.get(1).getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    @DisplayName("应该根据ID查询支付详情")
    void shouldGetPaymentById() {
        // Given
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(mockPayment));

        // When
        Optional<PaymentResponse> result = paymentService.getPaymentById(1L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        assertThat(result.get().getOrderId()).isEqualTo(1001L);
    }

    @Test
    @DisplayName("不存在的支付ID应该返回空")
    void shouldReturnEmptyForNonExistentPaymentId() {
        // Given
        when(paymentRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<PaymentResponse> result = paymentService.getPaymentById(999L);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("应该处理过期支付")
    void shouldProcessExpiredPayments() {
        // Given
        Payment expiredPayment1 = new Payment(1001L, new BigDecimal("99.99"), PaymentGateway.ALIPAY);
        expiredPayment1.setId(1L);
        expiredPayment1.setStatus(PaymentStatus.PENDING);
        
        Payment expiredPayment2 = new Payment(1002L, new BigDecimal("59.99"), PaymentGateway.WECHAT_PAY);
        expiredPayment2.setId(2L);
        expiredPayment2.setStatus(PaymentStatus.PENDING);

        when(paymentRepository.findExpiredPayments(eq(PaymentStatus.PENDING), any(Instant.class)))
                .thenReturn(Arrays.asList(expiredPayment1, expiredPayment2));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        int processedCount = paymentService.processExpiredPayments();

        // Then
        assertThat(processedCount).isEqualTo(2);
        assertThat(expiredPayment1.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(expiredPayment1.getFailureReason()).isEqualTo("Payment expired");
        assertThat(expiredPayment2.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(expiredPayment2.getFailureReason()).isEqualTo("Payment expired");

        verify(paymentRepository, times(2)).save(any(Payment.class));
    }

    @Test
    @DisplayName("验证回调签名应该正确工作")
    void shouldVerifyCallbackSignature() {
        // Given
        PaymentCallbackRequest validRequest = new PaymentCallbackRequest();
        validRequest.setSign("valid_signature");

        PaymentCallbackRequest invalidRequest = new PaymentCallbackRequest();
        invalidRequest.setSign(""); // 空签名

        PaymentCallbackRequest nullSignRequest = new PaymentCallbackRequest();
        nullSignRequest.setSign(null); // null签名

        // When & Then
        assertThat(paymentService.verifyCallbackSignature(validRequest)).isTrue();
        assertThat(paymentService.verifyCallbackSignature(invalidRequest)).isFalse();
        assertThat(paymentService.verifyCallbackSignature(nullSignRequest)).isFalse();
    }

    @Test
    @DisplayName("同步支付状态应该返回支付信息")
    void shouldSyncPaymentStatus() {
        // Given
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(mockPayment));

        // When
        PaymentResponse response = paymentService.syncPaymentStatus(1L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getOrderId()).isEqualTo(1001L);

        verify(paymentRepository).findById(1L);
    }

    @Test
    @DisplayName("同步不存在的支付应该抛出异常")
    void shouldThrowExceptionWhenSyncingNonExistentPayment() {
        // Given
        when(paymentRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> paymentService.syncPaymentStatus(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Payment not found: 999");
    }

    @Test
    @DisplayName("处理过期支付时遇到异常应该继续处理其他支付")
    void shouldContinueProcessingWhenExceptionOccurs() {
        // Given
        Payment expiredPayment1 = new Payment(1001L, new BigDecimal("99.99"), PaymentGateway.ALIPAY);
        expiredPayment1.setId(1L);
        expiredPayment1.setStatus(PaymentStatus.PENDING);
        
        Payment expiredPayment2 = new Payment(1002L, new BigDecimal("59.99"), PaymentGateway.WECHAT_PAY);
        expiredPayment2.setId(2L);
        expiredPayment2.setStatus(PaymentStatus.PENDING);

        when(paymentRepository.findExpiredPayments(eq(PaymentStatus.PENDING), any(Instant.class)))
                .thenReturn(Arrays.asList(expiredPayment1, expiredPayment2));
        
        // 第一个支付保存时抛出异常，第二个正常
        when(paymentRepository.save(expiredPayment1)).thenThrow(new RuntimeException("Database error"));
        when(paymentRepository.save(expiredPayment2)).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        int processedCount = paymentService.processExpiredPayments();

        // Then
        assertThat(processedCount).isEqualTo(1); // 只有一个处理成功
        assertThat(expiredPayment2.getStatus()).isEqualTo(PaymentStatus.CANCELLED);

        verify(paymentRepository, times(2)).save(any(Payment.class));
    }

    @Test
    @DisplayName("回调处理异常时应该返回fail")
    void shouldReturnFailWhenCallbackProcessingFails() {
        // Given
        PaymentCallbackRequest callbackRequest = new PaymentCallbackRequest();
        callbackRequest.setPaymentNumber("PAY123456789");
        callbackRequest.setStatus("success");
        callbackRequest.setSign("valid_signature");

        when(paymentRepository.findByPaymentNumber("PAY123456789"))
                .thenReturn(Optional.of(mockPayment));
        when(paymentRepository.save(any(Payment.class))).thenThrow(new RuntimeException("Database error"));

        // When
        String result = paymentService.handlePaymentCallback(callbackRequest);

        // Then
        assertThat(result).isEqualTo("fail");
    }
}