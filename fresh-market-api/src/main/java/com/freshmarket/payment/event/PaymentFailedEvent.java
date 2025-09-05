package com.freshmarket.payment.event;

import com.freshmarket.payment.enums.PaymentStatus;

/**
 * 支付失败事件
 * 当支付失败时发布此事件，用于通知其他模块进行相应处理
 * 
 * @author Fresh Market Team
 */
public class PaymentFailedEvent extends PaymentEvent {
    
    private final String failureReason;
    
    public PaymentFailedEvent(Object source, Long orderId, String paymentNumber, String failureReason) {
        super(source, orderId, paymentNumber, PaymentStatus.FAILED);
        this.failureReason = failureReason;
    }
    
    public String getFailureReason() {
        return failureReason;
    }
    
    @Override
    public String toString() {
        return String.format("PaymentFailedEvent{orderId=%d, paymentNumber='%s', reason='%s'}", 
                getOrderId(), getPaymentNumber(), failureReason);
    }
}