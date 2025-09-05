package com.freshmarket.payment.enums;

/**
 * 支付网关枚举
 * 用于标识不同的支付服务提供商
 * 
 * @author Fresh Market Team
 */
public enum PaymentGateway {
    
    /**
     * 模拟支付 - 用于开发和测试环境
     */
    MOCK("模拟支付", "mock"),
    
    /**
     * 支付宝 - 蚂蚁集团支付服务
     */
    ALIPAY("支付宝", "alipay"),
    
    /**
     * 微信支付 - 腾讯微信支付服务
     */
    WECHAT_PAY("微信支付", "wechat"),
    
    /**
     * 银联支付 - 中国银联支付服务
     */
    UNIONPAY("银联支付", "unionpay");
    
    private final String displayName;
    private final String code;
    
    PaymentGateway(String displayName, String code) {
        this.displayName = displayName;
        this.code = code;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getCode() {
        return code;
    }
    
    /**
     * 根据代码获取支付网关枚举
     * @param code 支付网关代码
     * @return PaymentGateway 枚举值
     * @throws IllegalArgumentException 如果代码不存在
     */
    public static PaymentGateway fromCode(String code) {
        if (code == null) {
            throw new IllegalArgumentException("Payment gateway code cannot be null");
        }
        
        for (PaymentGateway gateway : values()) {
            if (gateway.code.equals(code.toLowerCase())) {
                return gateway;
            }
        }
        
        throw new IllegalArgumentException("Unknown payment gateway code: " + code);
    }
    
    /**
     * 判断是否为模拟支付网关
     * @return true if mock gateway
     */
    public boolean isMock() {
        return this == MOCK;
    }
}