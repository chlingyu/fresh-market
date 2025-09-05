package com.freshmarket.common.exception;

/**
 * 支付处理异常
 */
public class PaymentProcessingException extends BusinessException {
    
    private final String paymentGateway;
    private final String gatewayErrorCode;
    private final String gatewayErrorMessage;

    public PaymentProcessingException(String paymentGateway, String message) {
        super("PAYMENT_PROCESSING_ERROR", message);
        this.paymentGateway = paymentGateway;
        this.gatewayErrorCode = null;
        this.gatewayErrorMessage = null;
    }

    public PaymentProcessingException(String paymentGateway, String gatewayErrorCode, 
                                    String gatewayErrorMessage, String message) {
        super("PAYMENT_PROCESSING_ERROR", message);
        this.paymentGateway = paymentGateway;
        this.gatewayErrorCode = gatewayErrorCode;
        this.gatewayErrorMessage = gatewayErrorMessage;
    }

    public String getPaymentGateway() {
        return paymentGateway;
    }

    public String getGatewayErrorCode() {
        return gatewayErrorCode;
    }

    public String getGatewayErrorMessage() {
        return gatewayErrorMessage;
    }
}