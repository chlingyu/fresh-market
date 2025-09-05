package com.freshmarket.payment.event;

import com.freshmarket.payment.enums.PaymentStatus;
import org.springframework.context.ApplicationEvent;

/**
 * 支付事件基类
 * 用于支付模块与其他模块间的解耦通信
 * 
 * @author Fresh Market Team
 */
public abstract class PaymentEvent extends ApplicationEvent {
    
    private final Long orderId;
    private final String paymentNumber;
    private final PaymentStatus paymentStatus;
    
    public PaymentEvent(Object source, Long orderId, String paymentNumber, PaymentStatus paymentStatus) {
        super(source);
        this.orderId = orderId;
        this.paymentNumber = paymentNumber;
        this.paymentStatus = paymentStatus;
    }
    
    public Long getOrderId() {
        return orderId;
    }
    
    public String getPaymentNumber() {
        return paymentNumber;
    }
    
    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }
}