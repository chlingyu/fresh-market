package com.freshmarket.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 业务指标监控配置
 */
@Configuration
public class MetricsConfig {

    private final MeterRegistry meterRegistry;

    public MetricsConfig(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * 订单创建成功计数器
     */
    @Bean
    public Counter orderCreatedCounter() {
        return Counter.builder("business.order.created")
                .description("订单创建成功数量")
                .tag("type", "success")
                .register(meterRegistry);
    }

    /**
     * 订单创建失败计数器
     */
    @Bean
    public Counter orderFailedCounter() {
        return Counter.builder("business.order.failed")
                .description("订单创建失败数量")
                .tag("type", "failed")
                .register(meterRegistry);
    }

    /**
     * 支付成功计数器
     */
    @Bean
    public Counter paymentSuccessCounter() {
        return Counter.builder("business.payment.success")
                .description("支付成功数量")
                .tag("type", "success")
                .register(meterRegistry);
    }

    /**
     * 支付失败计数器
     */
    @Bean
    public Counter paymentFailedCounter() {
        return Counter.builder("business.payment.failed")
                .description("支付失败数量")
                .tag("type", "failed")
                .register(meterRegistry);
    }

    /**
     * 库存减少操作计时器
     */
    @Bean
    public Timer inventoryDecreaseTimer() {
        return Timer.builder("business.inventory.decrease.time")
                .description("库存减少操作耗时")
                .register(meterRegistry);
    }

    /**
     * 购物车添加商品计数器
     */
    @Bean
    public Counter cartAddItemCounter() {
        return Counter.builder("business.cart.add_item")
                .description("购物车添加商品数量")
                .register(meterRegistry);
    }

    /**
     * 用户注册计数器
     */
    @Bean
    public Counter userRegisterCounter() {
        return Counter.builder("business.user.register")
                .description("用户注册数量")
                .register(meterRegistry);
    }

    /**
     * 用户登录成功计数器
     */
    @Bean
    public Counter userLoginSuccessCounter() {
        return Counter.builder("business.user.login.success")
                .description("用户登录成功数量")
                .register(meterRegistry);
    }

    /**
     * 用户登录失败计数器
     */
    @Bean
    public Counter userLoginFailedCounter() {
        return Counter.builder("business.user.login.failed")
                .description("用户登录失败数量")
                .register(meterRegistry);
    }

    /**
     * 商品搜索计数器
     */
    @Bean
    public Counter productSearchCounter() {
        return Counter.builder("business.product.search")
                .description("商品搜索次数")
                .register(meterRegistry);
    }

    /**
     * 分类访问计数器
     */
    @Bean
    public Counter categoryAccessCounter() {
        return Counter.builder("business.category.access")
                .description("分类访问次数")
                .register(meterRegistry);
    }
}