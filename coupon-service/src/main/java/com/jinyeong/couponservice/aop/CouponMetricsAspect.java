package com.jinyeong.couponservice.aop;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class CouponMetricsAspect {
    private final MeterRegistry registry;

    @Around("@annotation(CouponMetered)")
    public Object measureCouponOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        Timer.Sample sample = Timer.start(registry);
        String version = extractVersion(joinPoint);
        String operation = extractOperation(joinPoint);
        String result = "success";
        String error = "none";

        try {
            return joinPoint.proceed();
        } catch (Exception e) {
            result = "failure";
            error = e.getClass().getSimpleName();
            throw e;
        } finally {
            registry.counter("coupon.operation.total",
                    "version", version,
                    "operation", operation,
                    "result", result,
                    "error", error
            ).increment();

            sample.stop(Timer.builder("coupon.operation.duration")
                    .tag("version", version)
                    .tag("operation", operation)
                    .tag("result", result)
                    .register(registry));
        }
    }

    private String extractVersion(ProceedingJoinPoint joinPoint) {
        CouponMetered annotation = ((MethodSignature) joinPoint.getSignature())
                .getMethod()
                .getAnnotation(CouponMetered.class);
        return annotation.version();
    }

    private String extractOperation(ProceedingJoinPoint joinPoint) {
        return joinPoint.getSignature().getName();
    }
}