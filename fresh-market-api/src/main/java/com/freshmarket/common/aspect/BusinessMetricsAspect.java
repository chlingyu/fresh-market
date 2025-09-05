package com.freshmarket.common.aspect;

import com.freshmarket.common.annotation.BusinessMetrics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 业务指标监控切面
 */
@Aspect
@Component
public class BusinessMetricsAspect {

    private static final Logger logger = LoggerFactory.getLogger(BusinessMetricsAspect.class);

    private final MeterRegistry meterRegistry;
    private final ConcurrentMap<String, Counter> counters = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Timer> timers = new ConcurrentHashMap<>();

    public BusinessMetricsAspect(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Around("@annotation(businessMetrics)")
    public Object around(ProceedingJoinPoint joinPoint, BusinessMetrics businessMetrics) throws Throwable {
        String metricName = businessMetrics.value();
        boolean recordTime = businessMetrics.recordTime();

        Timer.Sample sample = null;
        if (recordTime || businessMetrics.type() == BusinessMetrics.MetricType.TIMER) {
            sample = Timer.start(meterRegistry);
        }

        try {
            Object result = joinPoint.proceed();
            
            // 记录成功指标
            recordSuccessMetric(metricName, businessMetrics);
            
            return result;
        } catch (Exception e) {
            // 记录失败指标
            recordFailureMetric(metricName, businessMetrics);
            throw e;
        } finally {
            // 记录执行时间
            if (sample != null) {
                Timer timer = getOrCreateTimer(metricName + ".time", businessMetrics.description() + " 执行时间");
                sample.stop(timer);
            }
        }
    }

    private void recordSuccessMetric(String metricName, BusinessMetrics businessMetrics) {
        switch (businessMetrics.type()) {
            case COUNTER:
                Counter successCounter = getOrCreateCounter(metricName + ".success", 
                    businessMetrics.description() + " 成功次数");
                successCounter.increment();
                break;
            case TIMER:
                // Timer will be handled in finally block
                break;
            default:
                logger.debug("Unsupported metric type: {}", businessMetrics.type());
        }
    }

    private void recordFailureMetric(String metricName, BusinessMetrics businessMetrics) {
        Counter failureCounter = getOrCreateCounter(metricName + ".failed", 
            businessMetrics.description() + " 失败次数");
        failureCounter.increment();
    }

    private Counter getOrCreateCounter(String name, String description) {
        return counters.computeIfAbsent(name, key -> 
            Counter.builder(key)
                .description(description)
                .register(meterRegistry)
        );
    }

    private Timer getOrCreateTimer(String name, String description) {
        return timers.computeIfAbsent(name, key -> 
            Timer.builder(key)
                .description(description)
                .register(meterRegistry)
        );
    }
}