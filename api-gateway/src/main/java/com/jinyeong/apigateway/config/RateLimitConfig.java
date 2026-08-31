package com.jinyeong.apigateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Configuration
public class RateLimitConfig {

    @Bean
    public RedisRateLimiter redisRateLimiter() {
        /**
         * replenishRate: 초당 허용되는 요청 수
         * burstCapacity: 최대 누적 가능한 요청 수
         */
        return new RedisRateLimiter(10, 20);
    }

    @Bean
    public KeyResolver userKeyResolver() {
        // 무엇을 키값으로 잡고 요청을 카운트하는지에 대한 설정
        return exchange -> Mono.just(
                Optional.ofNullable(exchange.getRequest().getHeaders().getFirst("X-User-ID"))
                        .orElseGet(() -> Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                                .map(addr -> addr.getAddress().getHostAddress())
                                .orElse("unknown"))
        );
    }
}
