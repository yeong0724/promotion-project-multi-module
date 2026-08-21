package com.jinyeong.userservice.controller;

import com.jinyeong.userservice.dto.UserDto;
import com.jinyeong.userservice.entity.User;
import com.jinyeong.userservice.service.JwtService;
import com.jinyeong.userservice.service.UserService;
import io.jsonwebtoken.Claims;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
public class AuthController {
    private final JwtService jwtService;
    private final UserService userService;

    public AuthController(JwtService jwtService, UserService userService) {
        this.jwtService = jwtService;
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserDto.LoginRequest loginRequest) {
        User user = userService.authenticate(loginRequest.getEmail(), loginRequest.getPassword());
        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(UserDto.LoginResponse.builder()
                .token(token)
                .user(UserDto.Response.from(user))
                .build());
    }

    @PostMapping("/validate-token")
    public ResponseEntity<?> validateToken(@RequestBody UserDto.TokenRequest tokenRequest) {
        Claims claims = jwtService.validateToken(tokenRequest.getToken());
        return ResponseEntity.ok(UserDto.TokenResponse.builder()
                .id(claims.get("userId", Integer.class))
                .email(claims.getSubject())
                .valid(true)
                .role(claims.get("role", String.class))
                .build());
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<Map<String, String>> refreshToken(@RequestBody UserDto.TokenRequest tokenRequest) {
        String newToken = jwtService.refreshToken(tokenRequest.getToken());
        return ResponseEntity.ok(Collections.singletonMap("token", newToken));
    }
}
