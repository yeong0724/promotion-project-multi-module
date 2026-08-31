package com.jinyeong.apigateway.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping("/users")   // 모든 메서드 수용
    public Mono<Map<String, Object>> userFallback() {
        return Mono.just(Map.of("status", "down"));
    }
}