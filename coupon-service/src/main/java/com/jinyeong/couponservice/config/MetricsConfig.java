package com.jinyeong.couponservice.config;

import com.jinyeong.couponservice.aop.CouponMetricsAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAspectJAutoProxy
public class MetricsConfig {

    // @Bean
    // public MetricsAspect metricsAspect(MeterRegistry registry) {
    //     return new MetricsAspect(registry);
    // }

    @Bean
    public CouponMetricsAspect couponMetricsAspect(MeterRegistry registry) {
        return new CouponMetricsAspect(registry);
    }
}
