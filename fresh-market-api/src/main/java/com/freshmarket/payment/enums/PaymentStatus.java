package com.freshmarket.payment.enums;

/**
 * 支付状态枚举
 * 用于表示支付记录的各种状态
 * 
 * @author Fresh Market Team
 */
public enum PaymentStatus {
    
    /**
     * 待支付 - 支付记录已创建，等待用户支付
     */
    PENDING("待支付"),
    
    /**
     * 处理中 - 支付请求已提交给支付网关，正在处理
     */
    PROCESSING("处理中"),
    
    /**
     * 支付成功 - 支付已完成且成功
     */
    SUCCESS("支付成功"),
    
    /**
     * 支付失败 - 支付失败或被拒绝
     */
    FAILED("支付失败"),
    
    /**
     * 已取消 - 支付被用户或系统取消
     */
    CANCELLED("已取消"),
    
    /**
     * 已退款 - 支付成功后进行了退款
     */
    REFUNDED("已退款");
    
    private final String description;
    
    PaymentStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 判断是否为最终状态（不可再变更）
     * @return true if final state
     */
    public boolean isFinalState() {
        return this == SUCCESS || this == FAILED || this == CANCELLED || this == REFUNDED;
    }
    
    /**
     * 判断是否为成功状态
     * @return true if successful payment
     */
    public boolean isSuccessful() {
        return this == SUCCESS;
    }
}