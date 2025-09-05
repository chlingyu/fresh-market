package com.freshmarket.payment.repository;

import com.freshmarket.payment.entity.Payment;
import com.freshmarket.payment.enums.PaymentGateway;
import com.freshmarket.payment.enums.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PaymentRepository单元测试
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DisplayName("支付Repository测试")
class PaymentRepositoryTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Payment pendingPayment;
    private Payment successPayment;
    private Payment failedPayment;
    private Payment expiredPayment;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();

        // 创建待支付记录
        pendingPayment = new Payment(1001L, new BigDecimal("99.99"), PaymentGateway.ALIPAY);
        pendingPayment.setStatus(PaymentStatus.PENDING);
        pendingPayment.setTransactionId("TXN001");

        // 创建成功支付记录
        successPayment = new Payment(1001L, new BigDecimal("59.99"), PaymentGateway.WECHAT_PAY);
        successPayment.setStatus(PaymentStatus.SUCCESS);
        successPayment.setTransactionId("TXN002");
        successPayment.setPaidAt(Instant.now());

        // 创建失败支付记录
        failedPayment = new Payment(1002L, new BigDecimal("29.99"), PaymentGateway.UNIONPAY);
        failedPayment.setStatus(PaymentStatus.FAILED);
        failedPayment.setFailureReason("卡余额不足");

        // 创建过期支付记录
        expiredPayment = new Payment(1003L, new BigDecimal("199.99"), PaymentGateway.ALIPAY);
        expiredPayment.setStatus(PaymentStatus.PENDING);
        expiredPayment.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));

        paymentRepository.save(pendingPayment);
        paymentRepository.save(successPayment);
        paymentRepository.save(failedPayment);
        paymentRepository.save(expiredPayment);

        entityManager.flush();
    }

    @Test
    @DisplayName("应该能根据订单ID查询支付记录")
    void shouldFindByOrderId() {
        List<Payment> payments = paymentRepository.findByOrderId(1001L);
        
        assertThat(payments).hasSize(2);
        assertThat(payments).extracting(Payment::getOrderId).containsOnly(1001L);
    }

    @Test
    @DisplayName("应该能根据订单ID查询最新支付记录")
    void shouldFindLatestByOrderId() {
        Optional<Payment> latestPayment = paymentRepository.findLatestByOrderId(1001L);
        
        assertThat(latestPayment).isPresent();
        // 由于两个支付记录都属于同一订单，应该返回最近创建的一个
        assertThat(latestPayment.get().getOrderId()).isEqualTo(1001L);
    }

    @Test
    @DisplayName("应该能根据支付单号查询支付记录")
    void shouldFindByPaymentNumber() {
        String paymentNumber = pendingPayment.getPaymentNumber();
        Optional<Payment> payment = paymentRepository.findByPaymentNumber(paymentNumber);
        
        assertThat(payment).isPresent();
        assertThat(payment.get().getId()).isEqualTo(pendingPayment.getId());
    }

    @Test
    @DisplayName("应该能根据交易流水号查询支付记录")
    void shouldFindByTransactionId() {
        Optional<Payment> payment = paymentRepository.findByTransactionId("TXN001");
        
        assertThat(payment).isPresent();
        assertThat(payment.get().getId()).isEqualTo(pendingPayment.getId());
    }

    @Test
    @DisplayName("应该能根据支付状态分页查询")
    void shouldFindByStatus() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Payment> pendingPayments = paymentRepository.findByStatus(PaymentStatus.PENDING, pageable);
        
        assertThat(pendingPayments.getContent()).hasSize(2); // pendingPayment + expiredPayment
        assertThat(pendingPayments.getContent()).allMatch(p -> p.getStatus() == PaymentStatus.PENDING);
    }

    @Test
    @DisplayName("应该能根据支付网关分页查询")
    void shouldFindByGateway() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Payment> alipayPayments = paymentRepository.findByGateway(PaymentGateway.ALIPAY, pageable);
        
        assertThat(alipayPayments.getContent()).hasSize(2); // pendingPayment + expiredPayment
        assertThat(alipayPayments.getContent()).allMatch(p -> p.getGateway() == PaymentGateway.ALIPAY);
    }

    @Test
    @DisplayName("应该能根据状态和网关查询")
    void shouldFindByStatusAndGateway() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Payment> result = paymentRepository.findByStatusAndGateway(
                PaymentStatus.PENDING, PaymentGateway.ALIPAY, pageable);
        
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).allMatch(p -> 
                p.getStatus() == PaymentStatus.PENDING && p.getGateway() == PaymentGateway.ALIPAY);
    }

    @Test
    @DisplayName("应该能查询过期支付记录")
    void shouldFindExpiredPayments() {
        Instant now = Instant.now();
        List<Payment> expiredPayments = paymentRepository.findExpiredPayments(PaymentStatus.PENDING, now);
        
        assertThat(expiredPayments).hasSize(1);
        assertThat(expiredPayments.get(0).getId()).isEqualTo(expiredPayment.getId());
    }

    @Test
    @DisplayName("应该能根据时间范围查询支付记录")
    void shouldFindByDateRange() {
        Instant startTime = Instant.now().minus(2, ChronoUnit.HOURS);
        Instant endTime = Instant.now().plus(1, ChronoUnit.HOURS);
        Pageable pageable = PageRequest.of(0, 10);
        
        Page<Payment> payments = paymentRepository.findByDateRange(startTime, endTime, pageable);
        
        assertThat(payments.getContent()).hasSize(4); // 所有支付记录都在范围内
    }

    @Test
    @DisplayName("应该能根据订单ID和状态查询支付记录")
    void shouldFindByOrderIdAndStatus() {
        Optional<Payment> payment = paymentRepository.findByOrderIdAndStatus(1001L, PaymentStatus.SUCCESS);
        
        assertThat(payment).isPresent();
        assertThat(payment.get().getId()).isEqualTo(successPayment.getId());
    }

    @Test
    @DisplayName("应该能统计指定状态的支付记录数量")
    void shouldCountByStatus() {
        long pendingCount = paymentRepository.countByStatus(PaymentStatus.PENDING);
        long successCount = paymentRepository.countByStatus(PaymentStatus.SUCCESS);
        long failedCount = paymentRepository.countByStatus(PaymentStatus.FAILED);
        
        assertThat(pendingCount).isEqualTo(2);
        assertThat(successCount).isEqualTo(1);
        assertThat(failedCount).isEqualTo(1);
    }

    @Test
    @DisplayName("应该能统计指定网关的支付记录数量")
    void shouldCountByGateway() {
        long alipayCount = paymentRepository.countByGateway(PaymentGateway.ALIPAY);
        long wechatCount = paymentRepository.countByGateway(PaymentGateway.WECHAT_PAY);
        long unionpayCount = paymentRepository.countByGateway(PaymentGateway.UNIONPAY);
        
        assertThat(alipayCount).isEqualTo(2);
        assertThat(wechatCount).isEqualTo(1);
        assertThat(unionpayCount).isEqualTo(1);
    }

    @Test
    @DisplayName("应该能统计时间范围内成功支付总金额")
    void shouldSumSuccessfulPaymentAmount() {
        Instant startTime = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant endTime = Instant.now().plus(1, ChronoUnit.HOURS);
        
        Double totalAmount = paymentRepository.sumSuccessfulPaymentAmount(startTime, endTime);
        
        assertThat(totalAmount).isEqualTo(59.99); // 只有successPayment是SUCCESS状态
    }

    @Test
    @DisplayName("应该能查询订单所有支付记录并按创建时间倒序")
    void shouldFindAllByOrderIdOrderByCreatedAtDesc() {
        List<Payment> payments = paymentRepository.findAllByOrderIdOrderByCreatedAtDesc(1001L);
        
        assertThat(payments).hasSize(2);
        assertThat(payments).extracting(Payment::getOrderId).containsOnly(1001L);
        
        // 验证按创建时间倒序排列
        if (payments.size() > 1) {
            for (int i = 0; i < payments.size() - 1; i++) {
                assertThat(payments.get(i).getCreatedAt()).isAfterOrEqualTo(payments.get(i + 1).getCreatedAt());
            }
        }
    }

    @Test
    @DisplayName("不存在的记录查询应该返回空结果")
    void shouldReturnEmptyForNonExistentRecords() {
        // 测试不存在的订单ID
        List<Payment> payments = paymentRepository.findByOrderId(9999L);
        assertThat(payments).isEmpty();
        
        // 测试不存在的支付单号
        Optional<Payment> payment = paymentRepository.findByPaymentNumber("NON_EXISTENT");
        assertThat(payment).isEmpty();
        
        // 测试不存在的交易流水号
        Optional<Payment> paymentByTxn = paymentRepository.findByTransactionId("NON_EXISTENT_TXN");
        assertThat(paymentByTxn).isEmpty();
    }

    @Test
    @DisplayName("应该正确处理null值查询")
    void shouldHandleNullValues() {
        // 创建一个没有交易流水号的支付记录
        Payment paymentWithoutTxnId = new Payment(2001L, new BigDecimal("15.00"), PaymentGateway.MOCK);
        paymentWithoutTxnId.setStatus(PaymentStatus.PENDING);
        paymentRepository.save(paymentWithoutTxnId);
        
        // 查询应该正常工作
        List<Payment> payments = paymentRepository.findByOrderId(2001L);
        assertThat(payments).hasSize(1);
        assertThat(payments.get(0).getTransactionId()).isNull();
    }

    @Test
    @DisplayName("分页查询应该正确工作")
    void shouldWorkWithPagination() {
        // 添加更多数据以测试分页
        for (int i = 0; i < 15; i++) {
            Payment payment = new Payment(3000L + i, new BigDecimal("10.00"), PaymentGateway.MOCK);
            paymentRepository.save(payment);
        }
        
        Pageable firstPage = PageRequest.of(0, 5);
        Page<Payment> page1 = paymentRepository.findByGateway(PaymentGateway.MOCK, firstPage);
        
        assertThat(page1.getContent()).hasSize(5);
        assertThat(page1.getTotalElements()).isGreaterThanOrEqualTo(15);
        assertThat(page1.hasNext()).isTrue();
        
        Pageable secondPage = PageRequest.of(1, 5);
        Page<Payment> page2 = paymentRepository.findByGateway(PaymentGateway.MOCK, secondPage);
        
        assertThat(page2.getContent()).hasSize(5);
        assertThat(page2.getNumber()).isEqualTo(1);
    }
}