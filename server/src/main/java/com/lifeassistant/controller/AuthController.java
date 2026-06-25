package com.lifeassistant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifeassistant.dto.AuthResponse;
import com.lifeassistant.dto.LoginRequest;
import com.lifeassistant.dto.RegisterRequest;
import com.lifeassistant.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuthController {

    @Autowired
    private AuthService authService;

    private final ObjectMapper mapper = new ObjectMapper();

    @GetMapping("/auth-test")
    public String authTest() {
        return "AuthController is working!";
    }

    @PostMapping("/register")
    public AuthResponse register(@RequestBody String body) {
        try {
            RegisterRequest req = mapper.readValue(body, RegisterRequest.class);
            return authService.register(req);
        } catch (Exception e) {
            e.printStackTrace();
            return AuthResponse.fail("解析失败: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody String body) {
        try {
            LoginRequest req = mapper.readValue(body, LoginRequest.class);
            return authService.login(req);
        } catch (Exception e) {
            e.printStackTrace();
            return AuthResponse.fail("解析失败: " + e.getMessage());
        }
    }
}
