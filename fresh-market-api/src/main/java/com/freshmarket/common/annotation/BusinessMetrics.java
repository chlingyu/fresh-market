package com.freshmarket.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 业务指标监控注解
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface BusinessMetrics {
    
    /**
     * 指标名称
     */
    String value();
    
    /**
     * 指标类型
     */
    MetricType type() default MetricType.COUNTER;
    
    /**
     * 指标描述
     */
    String description() default "";
    
    /**
     * 是否记录执行时间
     */
    boolean recordTime() default false;
    
    enum MetricType {
        COUNTER, TIMER, GAUGE
    }
}