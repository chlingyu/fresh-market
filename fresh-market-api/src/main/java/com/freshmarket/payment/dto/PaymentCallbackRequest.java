package com.freshmarket.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 支付回调请求DTO
 * 用于接收第三方支付平台的回调通知
 * 
 * @author Fresh Market Team
 */
@Schema(description = "支付回调请求")
public class PaymentCallbackRequest {

    @NotBlank(message = "支付单号不能为空")
    @Schema(description = "支付单号", example = "PAY1704067200123456", required = true)
    private String paymentNumber;

    @Schema(description = "第三方交易流水号", example = "2024010122001234567890")
    private String transactionId;

    @NotBlank(message = "支付状态不能为空")
    @Schema(description = "支付状态", example = "success", required = true)
    private String status;

    @Schema(description = "支付金额", example = "99.99")
    private String amount;

    @Schema(description = "签名", example = "abc123def456")
    private String sign;

    @Schema(description = "原始回调数据", example = "{\"trade_no\":\"xxx\",\"out_trade_no\":\"yyy\"}")
    private String rawData;

    @Schema(description = "失败原因", example = "余额不足")
    private String failureReason;

    @Schema(description = "支付时间", example = "2024-01-01 10:30:00")
    private String paidTime;

    // 默认构造函数
    public PaymentCallbackRequest() {}

    // Getters and Setters
    
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getSign() {
        return sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }

    public String getRawData() {
        return rawData;
    }

    public void setRawData(String rawData) {
        this.rawData = rawData;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getPaidTime() {
        return paidTime;
    }

    public void setPaidTime(String paidTime) {
        this.paidTime = paidTime;
    }
}