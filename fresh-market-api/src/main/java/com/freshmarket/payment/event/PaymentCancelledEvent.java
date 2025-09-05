package com.freshmarket.payment.event;

import com.freshmarket.payment.enums.PaymentStatus;

/**
 * 支付取消事件
 * 当支付被取消时发布此事件，用于通知其他模块进行相应处理
 * 
 * @author Fresh Market Team
 */
public class PaymentCancelledEvent extends PaymentEvent {
    
    private final String cancelReason;
    
    public PaymentCancelledEvent(Object source, Long orderId, String paymentNumber, String cancelReason) {
        super(source, orderId, paymentNumber, PaymentStatus.CANCELLED);
        this.cancelReason = cancelReason;
    }
    
    public String getCancelReason() {
        return cancelReason;
    }
    
    @Override
    public String toString() {
        return String.format("PaymentCancelledEvent{orderId=%d, paymentNumber='%s', reason='%s'}", 
                getOrderId(), getPaymentNumber(), cancelReason);
    }
}