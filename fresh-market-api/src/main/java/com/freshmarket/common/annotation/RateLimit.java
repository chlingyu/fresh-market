package com.freshmarket.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流注解
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    
    /**
     * 限流器名称
     */
    String value() default "general-api";
    
    /**
     * 限流失败时的回退方法名称
     */
    String fallbackMethod() default "";
}