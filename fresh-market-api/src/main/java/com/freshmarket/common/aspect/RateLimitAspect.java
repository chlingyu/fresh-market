package com.freshmarket.common.aspect;

import com.freshmarket.common.annotation.RateLimit;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.Callable;

/**
 * 限流切面
 */
@Aspect
@Component
public class RateLimitAspect {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitAspect.class);

    private final ApplicationContext applicationContext;

    public RateLimitAspect(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String rateLimiterName = rateLimit.value();
        
        // 获取对应的限流器
        RateLimiter rateLimiter;
        try {
            rateLimiter = applicationContext.getBean(rateLimiterName + "RateLimiter", RateLimiter.class);
        } catch (Exception e) {
            logger.warn("RateLimiter {} not found, using default", rateLimiterName);
            rateLimiter = applicationContext.getBean("generalApiRateLimiter", RateLimiter.class);
        }

        // 执行限流逻辑
        Callable<Object> restrictedCall = RateLimiter.decorateCallable(rateLimiter, () -> {
            try {
                return joinPoint.proceed();
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        });

        try {
            return restrictedCall.call();
        } catch (RequestNotPermitted requestNotPermitted) {
            logger.warn("Rate limit exceeded for API: {}", joinPoint.getSignature().getName());
            
            // 如果配置了回退方法，尝试执行
            if (!rateLimit.fallbackMethod().isEmpty()) {
                return handleFallback(joinPoint, rateLimit.fallbackMethod());
            }
            
            throw new ResponseStatusException(
                HttpStatus.TOO_MANY_REQUESTS, 
                "API调用频率过高，请稍后重试"
            );
        } catch (Exception e) {
            if (e.getCause() instanceof RuntimeException && e.getCause().getCause() instanceof Throwable) {
                throw (Throwable) e.getCause().getCause();
            }
            throw e;
        }
    }

    /**
     * 处理回退方法
     */
    private Object handleFallback(ProceedingJoinPoint joinPoint, String fallbackMethodName) {
        try {
            // 获取目标对象和方法参数
            Object target = joinPoint.getTarget();
            Object[] args = joinPoint.getArgs();
            
            // 通过反射调用回退方法
            return target.getClass()
                    .getMethod(fallbackMethodName, getParameterTypes(args))
                    .invoke(target, args);
        } catch (Exception e) {
            logger.error("Failed to execute fallback method: {}", fallbackMethodName, e);
            throw new ResponseStatusException(
                HttpStatus.TOO_MANY_REQUESTS,
                "API调用频率过高，请稍后重试"
            );
        }
    }

    private Class<?>[] getParameterTypes(Object[] args) {
        Class<?>[] types = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            types[i] = args[i] != null ? args[i].getClass() : Object.class;
        }
        return types;
    }
}