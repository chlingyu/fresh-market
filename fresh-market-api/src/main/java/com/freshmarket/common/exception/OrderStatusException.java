package com.freshmarket.common.exception;

/**
 * 订单状态异常
 */
public class OrderStatusException extends BusinessException {
    
    private final Long orderId;
    private final String currentStatus;
    private final String requiredStatus;
    private final String operation;

    public OrderStatusException(Long orderId, String currentStatus, 
                              String requiredStatus, String operation) {
        super("ORDER_STATUS_INVALID", 
              String.format("订单 %d 当前状态为 %s，无法执行 %s 操作，需要状态为 %s", 
                          orderId, currentStatus, operation, requiredStatus));
        this.orderId = orderId;
        this.currentStatus = currentStatus;
        this.requiredStatus = requiredStatus;
        this.operation = operation;
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public String getRequiredStatus() {
        return requiredStatus;
    }

    public String getOperation() {
        return operation;
    }
}