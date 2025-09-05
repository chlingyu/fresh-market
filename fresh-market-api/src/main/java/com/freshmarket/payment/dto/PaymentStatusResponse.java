package com.freshmarket.payment.dto;

import com.freshmarket.payment.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 支付状态查询响应DTO
 * 用于返回支付状态查询结果
 * 
 * @author Fresh Market Team
 */
@Schema(description = "支付状态查询响应")
public class PaymentStatusResponse {

    @Schema(description = "支付单号", example = "PAY1704067200123456")
    private String paymentNumber;

    @Schema(description = "订单ID", example = "123")
    private Long orderId;

    @Schema(description = "支付状态", example = "SUCCESS")
    private PaymentStatus status;

    @Schema(description = "支付状态描述", example = "支付成功")
    private String statusDescription;

    @Schema(description = "第三方交易流水号", example = "2024010122001234567890")
    private String transactionId;

    @Schema(description = "是否支付成功", example = "true")
    private boolean isSuccess;

    @Schema(description = "是否为最终状态", example = "true")
    private boolean isFinalState;

    @Schema(description = "支付失败原因", example = "余额不足")
    private String failureReason;

    // 默认构造函数
    public PaymentStatusResponse() {}

    // 构造函数
    public PaymentStatusResponse(String paymentNumber, Long orderId, PaymentStatus status) {
        this.paymentNumber = paymentNumber;
        this.orderId = orderId;
        this.status = status;
        this.statusDescription = status.getDescription();
        this.isSuccess = status.isSuccessful();
        this.isFinalState = status.isFinalState();
    }

    // 静态工厂方法
    public static PaymentStatusResponse success(String paymentNumber, Long orderId, String transactionId) {
        PaymentStatusResponse response = new PaymentStatusResponse(paymentNumber, orderId, PaymentStatus.SUCCESS);
        response.setTransactionId(transactionId);
        return response;
    }

    public static PaymentStatusResponse pending(String paymentNumber, Long orderId) {
        return new PaymentStatusResponse(paymentNumber, orderId, PaymentStatus.PENDING);
    }

    public static PaymentStatusResponse failed(String paymentNumber, Long orderId, String failureReason) {
        PaymentStatusResponse response = new PaymentStatusResponse(paymentNumber, orderId, PaymentStatus.FAILED);
        response.setFailureReason(failureReason);
        return response;
    }

    // Getters and Setters
    
    public String getPaymentNumber() {
        return paymentNumber;
    }

    public void setPaymentNumber(String paymentNumber) {
        this.paymentNumber = paymentNumber;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
        this.statusDescription = status.getDescription();
        this.isSuccess = status.isSuccessful();
        this.isFinalState = status.isFinalState();
    }

    public String getStatusDescription() {
        return statusDescription;
    }

    public void setStatusDescription(String statusDescription) {
        this.statusDescription = statusDescription;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public void setSuccess(boolean success) {
        isSuccess = success;
    }

    public boolean isFinalState() {
        return isFinalState;
    }

    public void setFinalState(boolean finalState) {
        isFinalState = finalState;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }
}