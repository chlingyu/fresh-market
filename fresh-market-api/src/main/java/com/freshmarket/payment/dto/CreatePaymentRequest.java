package com.freshmarket.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * 创建支付请求DTO
 * 
 * @author Fresh Market Team
 */
@Schema(description = "创建支付请求")
public class CreatePaymentRequest {

    @NotNull(message = "订单ID不能为空")
    @Positive(message = "订单ID必须为正数")
    @Schema(description = "订单ID", example = "1", required = true)
    private Long orderId;

    @NotNull(message = "支付金额不能为空")
    @DecimalMin(value = "0.01", message = "支付金额必须大于0")
    @Digits(integer = 10, fraction = 2, message = "支付金额格式不正确")
    @Schema(description = "支付金额", example = "99.99", required = true)
    private BigDecimal amount;

    @NotBlank(message = "支付方式不能为空")
    @Pattern(regexp = "^(mock|alipay|wechat|unionpay)$", 
             message = "支付方式只能是: mock, alipay, wechat, unionpay")
    @Schema(description = "支付方式", 
            example = "alipay", 
            allowableValues = {"mock", "alipay", "wechat", "unionpay"}, 
            required = true)
    private String paymentMethod;

    @Schema(description = "支付回调地址", example = "https://example.com/payment/callback")
    private String callbackUrl;

    @Schema(description = "支付完成后跳转地址", example = "https://example.com/payment/success")
    private String returnUrl;

    // 默认构造函数
    public CreatePaymentRequest() {}

    // 构造函数
    public CreatePaymentRequest(Long orderId, BigDecimal amount, String paymentMethod) {
        this.orderId = orderId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
    }

    // Getters and Setters
    
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

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }
}