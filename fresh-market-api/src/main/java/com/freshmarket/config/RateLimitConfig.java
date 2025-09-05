package com.freshmarket.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 限流配置
 */
@Configuration
public class RateLimitConfig {

    /**
     * 购物车相关API限流配置
     */
    @Bean
    public RateLimiter cartApiRateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(100) // 每个时间窗口最多100个请求
                .limitRefreshPeriod(Duration.ofMinutes(1)) // 时间窗口1分钟
                .timeoutDuration(Duration.ofSeconds(5)) // 等待许可的超时时间
                .build();

        return RateLimiterRegistry.of(config).rateLimiter("cart-api");
    }

    /**
     * 商品搜索API限流配置
     */
    @Bean
    public RateLimiter searchApiRateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(200) // 每个时间窗口最多200个请求
                .limitRefreshPeriod(Duration.ofMinutes(1)) // 时间窗口1分钟
                .timeoutDuration(Duration.ofSeconds(3)) // 等待许可的超时时间
                .build();

        return RateLimiterRegistry.of(config).rateLimiter("search-api");
    }

    /**
     * 用户注册登录API限流配置
     */
    @Bean
    public RateLimiter authApiRateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(10) // 每个时间窗口最多10个请求（防止暴力破解）
                .limitRefreshPeriod(Duration.ofMinutes(1)) // 时间窗口1分钟
                .timeoutDuration(Duration.ofSeconds(10)) // 等待许可的超时时间
                .build();

        return RateLimiterRegistry.of(config).rateLimiter("auth-api");
    }

    /**
     * 订单创建API限流配置
     */
    @Bean
    public RateLimiter orderApiRateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(20) // 每个时间窗口最多20个请求
                .limitRefreshPeriod(Duration.ofMinutes(1)) // 时间窗口1分钟
                .timeoutDuration(Duration.ofSeconds(5)) // 等待许可的超时时间
                .build();

        return RateLimiterRegistry.of(config).rateLimiter("order-api");
    }

    /**
     * 通用API限流配置
     */
    @Bean
    public RateLimiter generalApiRateLimiter() {
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitForPeriod(500) // 每个时间窗口最多500个请求
                .limitRefreshPeriod(Duration.ofMinutes(1)) // 时间窗口1分钟
                .timeoutDuration(Duration.ofSeconds(2)) // 等待许可的超时时间
                .build();

        return RateLimiterRegistry.of(config).rateLimiter("general-api");
    }
}