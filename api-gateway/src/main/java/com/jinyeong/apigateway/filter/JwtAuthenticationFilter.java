package com.jinyeong.apigateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.loadbalancer.reactive.ReactorLoadBalancerExchangeFilterFunction;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@Slf4j
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {
    private static final String BEARER_PREFIX = "Bearer ";

    private final WebClient webClient;

    public JwtAuthenticationFilter(ReactorLoadBalancerExchangeFilterFunction lbFunction) {
        super(Config.class);
        this.webClient = WebClient.builder()
                .filter(lbFunction)
                .baseUrl("http://user-service")
                .build();
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
            if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
                return unauthorized(exchange, "Missing or malformed Authorization header");
            }

            String token = authHeader.substring(BEARER_PREFIX.length());
            return validateToken(token)
                    .flatMap(userId -> proceedWithUserId(userId, exchange, chain))
                    .onErrorResume(e -> unauthorized(exchange, e.getMessage()));
        };
    }

    /**
     * user-service 에 토큰 검증을 위임하고 userId 를 돌려받는다.
     * 검증 실패(4xx/5xx)나 응답에 userId 가 없는 경우는 모두 에러 시그널로 변환한다.
     */
    private Mono<Long> validateToken(String token) {
        return webClient.post()
                .uri("/api/v1/users/validate-token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("token", token))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .flatMap(response -> {
                    Object id = response.get("id");
                    if (!Boolean.TRUE.equals(response.get("valid")) || id == null) {
                        return Mono.error(new IllegalStateException("Invalid token validation response: " + response));
                    }
                    return Mono.just(Long.valueOf(id.toString()));
                })
                .switchIfEmpty(Mono.error(new IllegalStateException("Empty token validation response")));
    }

    private Mono<Void> proceedWithUserId(Long userId, ServerWebExchange exchange, GatewayFilterChain chain) {
        // mutate() 는 원본을 바꾸지 않고 새 객체를 반환하므로 반드시 exchange 에 다시 넣어줘야 한다.
        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(exchange.getRequest().mutate()
                        .header("X-USER-ID", String.valueOf(userId))
                        .build())
                .build();
        return chain.filter(mutatedExchange);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String reason) {
        log.debug("Authentication failed: {}", reason);
        if (exchange.getResponse().isCommitted()) {
            return Mono.empty();
        }
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    public static class Config {
        // 필터 구성을 위한 설정 클래스
    }
}
