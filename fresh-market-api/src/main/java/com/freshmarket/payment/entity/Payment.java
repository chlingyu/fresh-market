package com.freshmarket.payment.entity;

import com.freshmarket.payment.enums.PaymentGateway;
import com.freshmarket.payment.enums.PaymentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 支付记录实体
 * 用于记录订单的支付信息和状态
 * 
 * @author Fresh Market Team
 */
@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_order_id", columnList = "order_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_gateway", columnList = "gateway"),
    @Index(name = "idx_transaction_id", columnList = "transaction_id"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联的订单ID
     */
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    /**
     * 支付金额
     */
    @NotNull(message = "支付金额不能为空")
    @DecimalMin(value = "0.01", message = "支付金额必须大于0")
    @Digits(integer = 10, fraction = 2, message = "支付金额格式不正确")
    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    /**
     * 支付状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.PENDING;

    /**
     * 支付网关
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "gateway", nullable = false, length = 20)
    private PaymentGateway gateway;

    /**
     * 第三方支付平台的交易流水号
     */
    @Size(max = 64, message = "交易流水号长度不能超过64个字符")
    @Column(name = "transaction_id", length = 64)
    private String transactionId;

    /**
     * 内部支付单号（唯一标识）
     */
    @Column(name = "payment_number", unique = true, nullable = false, length = 32)
    private String paymentNumber;

    /**
     * 支付网关返回的原始响应数据
     */
    @Column(name = "gateway_response", columnDefinition = "TEXT")
    private String gatewayResponse;

    /**
     * 支付失败原因
     */
    @Size(max = 500, message = "支付失败原因长度不能超过500个字符")
    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    /**
     * 支付完成时间
     */
    @Column(name = "paid_at")
    private Instant paidAt;

    /**
     * 支付过期时间
     */
    @Column(name = "expires_at")
    private Instant expiresAt;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * 在持久化之前自动生成支付单号
     */
    @PrePersist
    protected void onCreate() {
        if (paymentNumber == null) {
            paymentNumber = generatePaymentNumber();
        }
        if (expiresAt == null) {
            // 默认30分钟过期
            expiresAt = Instant.now().plusSeconds(30 * 60);
        }
    }

    /**
     * 生成唯一的支付单号
     */
    private String generatePaymentNumber() {
        return "PAY" + System.currentTimeMillis() + String.format("%04d", (int) (Math.random() * 10000));
    }

    // 默认构造函数
    public Payment() {}

    // 构造函数
    public Payment(Long orderId, BigDecimal amount, PaymentGateway gateway) {
        this.orderId = orderId;
        this.amount = amount;
        this.gateway = gateway;
    }

    // 业务方法

    /**
     * 更新支付状态
     * @param newStatus 新的支付状态
     * @param transactionId 交易流水号
     */
    public void updateStatus(PaymentStatus newStatus, String transactionId) {
        if (this.status.isFinalState() && newStatus != this.status) {
            throw new IllegalStateException("Cannot change status from final state " + this.status + " to " + newStatus);
        }
        
        this.status = newStatus;
        if (transactionId != null) {
            this.transactionId = transactionId;
        }
        
        if (newStatus == PaymentStatus.SUCCESS) {
            this.paidAt = Instant.now();
        }
    }

    /**
     * 标记支付失败
     * @param reason 失败原因
     */
    public void markAsFailed(String reason) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
    }

    /**
     * 判断支付是否已过期
     * @return true if expired
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    /**
     * 判断支付是否可以被取消
     * @return true if cancellable
     */
    public boolean isCancellable() {
        return status == PaymentStatus.PENDING || status == PaymentStatus.PROCESSING;
    }

    // Getters and Setters
    
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public PaymentGateway getGateway() {
        return gateway;
    }

    public void setGateway(PaymentGateway gateway) {
        this.gateway = gateway;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getPaymentNumber() {
        return paymentNumber;
    }

    public void setPaymentNumber(String paymentNumber) {
        this.paymentNumber = paymentNumber;
    }

    public String getGatewayResponse() {
        return gatewayResponse;
    }

    public void setGatewayResponse(String gatewayResponse) {
        this.gatewayResponse = gatewayResponse;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(Instant paidAt) {
        this.paidAt = paidAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}