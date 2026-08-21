package com.jinyeong.userservice.controller;

import com.jinyeong.userservice.dto.UserDto;
import com.jinyeong.userservice.entity.User;
import com.jinyeong.userservice.entity.UserLoginHistory;
import com.jinyeong.userservice.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> createUser(@RequestBody UserDto.SignupRequest request) {
        User user = userService.createUser(request.getEmail(), request.getPassword(), request.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserDto.Response.from(user));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getProfile(@RequestHeader("X-USER-ID") Integer userId) {
        User user = userService.getUserById(userId);
        return ResponseEntity.ok(UserDto.Response.from(user));
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateProfile(
            @RequestHeader("X-USER-ID") Integer userId,
            @RequestBody UserDto.UpdateRequest request
    ) {
        User user = userService.updateUser(userId, request.getName());
        return ResponseEntity.ok(UserDto.Response.from(user));
    }

    @PostMapping("/me/password")
    public ResponseEntity<?> changePassword(
            @RequestHeader("X-USER-ID") Integer userId,
            @RequestBody UserDto.PasswordChangeRequest request
    ) {
        userService.changePassword(userId, request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me/login-history")
    public ResponseEntity<List<UserLoginHistory>> getLoginHistory(
            @RequestHeader("X-USER-ID") Integer userId) {
        List<UserLoginHistory> history = userService.getUserLoginHistory(userId);
        return ResponseEntity.ok(history);
    }
}
