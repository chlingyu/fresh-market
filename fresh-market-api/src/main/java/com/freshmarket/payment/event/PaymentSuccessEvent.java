package com.freshmarket.payment.event;

import com.freshmarket.payment.enums.PaymentStatus;

/**
 * 支付成功事件
 * 当支付成功时发布此事件，订单模块监听后更新订单状态
 * 
 * @author Fresh Market Team
 */
public class PaymentSuccessEvent extends PaymentEvent {
    
    private final String transactionId;
    
    public PaymentSuccessEvent(Object source, Long orderId, String paymentNumber, String transactionId) {
        super(source, orderId, paymentNumber, PaymentStatus.SUCCESS);
        this.transactionId = transactionId;
    }
    
    public String getTransactionId() {
        return transactionId;
    }
    
    @Override
    public String toString() {
        return String.format("PaymentSuccessEvent{orderId=%d, paymentNumber='%s', transactionId='%s'}", 
                getOrderId(), getPaymentNumber(), transactionId);
    }
}