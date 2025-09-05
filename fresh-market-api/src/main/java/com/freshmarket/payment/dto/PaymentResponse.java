package com.freshmarket.payment.dto;

import com.freshmarket.payment.entity.Payment;
import com.freshmarket.payment.enums.PaymentGateway;
import com.freshmarket.payment.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 支付记录响应DTO
 * 
 * @author Fresh Market Team
 */
@Schema(description = "支付记录响应")
public class PaymentResponse {

    @Schema(description = "支付ID", example = "1")
    private Long id;

    @Schema(description = "订单ID", example = "123")
    private Long orderId;

    @Schema(description = "支付金额", example = "99.99")
    private BigDecimal amount;

    @Schema(description = "支付状态", example = "SUCCESS")
    private PaymentStatus status;

    @Schema(description = "支付网关", example = "ALIPAY")
    private PaymentGateway gateway;

    @Schema(description = "支付单号", example = "PAY1704067200123456")
    private String paymentNumber;

    @Schema(description = "第三方交易流水号", example = "2024010122001234567890")
    private String transactionId;

    @Schema(description = "支付失败原因", example = "余额不足")
    private String failureReason;

    @Schema(description = "支付完成时间", example = "2024-01-01T10:30:00Z")
    private Instant paidAt;

    @Schema(description = "支付过期时间", example = "2024-01-01T11:00:00Z")
    private Instant expiresAt;

    @Schema(description = "创建时间", example = "2024-01-01T10:00:00Z")
    private Instant createdAt;

    @Schema(description = "更新时间", example = "2024-01-01T10:30:00Z")
    private Instant updatedAt;

    // 默认构造函数
    public PaymentResponse() {}

    // 从实体转换的构造函数
    public PaymentResponse(Payment payment) {
        this.id = payment.getId();
        this.orderId = payment.getOrderId();
        this.amount = payment.getAmount();
        this.status = payment.getStatus();
        this.gateway = payment.getGateway();
        this.paymentNumber = payment.getPaymentNumber();
        this.transactionId = payment.getTransactionId();
        this.failureReason = payment.getFailureReason();
        this.paidAt = payment.getPaidAt();
        this.expiresAt = payment.getExpiresAt();
        this.createdAt = payment.getCreatedAt();
        this.updatedAt = payment.getUpdatedAt();
    }

    // 静态工厂方法
    public static PaymentResponse fromEntity(Payment payment) {
        return new PaymentResponse(payment);
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

    public String getPaymentNumber() {
        return paymentNumber;
    }

    public void setPaymentNumber(String paymentNumber) {
        this.paymentNumber = paymentNumber;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
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