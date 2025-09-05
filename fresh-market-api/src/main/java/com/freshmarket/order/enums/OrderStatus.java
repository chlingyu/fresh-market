package com.freshmarket.order.enums;

/**
 * 订单状态枚举
 */
public enum OrderStatus {
    PENDING("待支付"),
    PAID("已支付"),
    SHIPPING("配送中"),
    DELIVERED("已完成"),
    CANCELLED("已取消");

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 检查状态转换是否有效
     */
    public boolean canTransitionTo(OrderStatus newStatus) {
        return switch (this) {
            case PENDING -> newStatus == PAID || newStatus == CANCELLED;
            case PAID -> newStatus == SHIPPING || newStatus == CANCELLED;
            case SHIPPING -> newStatus == DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };
    }
}