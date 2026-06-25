package com.lifeassistant.service;

import com.lifeassistant.config.JwtUtil;
import com.lifeassistant.dto.AuthResponse;
import com.lifeassistant.dto.LoginRequest;
import com.lifeassistant.dto.RegisterRequest;
import com.lifeassistant.model.User;
import com.lifeassistant.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    public AuthResponse register(RegisterRequest req) {
        if (req.getUsername() == null || req.getUsername().trim().isEmpty()) {
            return AuthResponse.fail("用户名不能为空");
        }
        if (req.getPassword() == null || req.getPassword().length() < 6) {
            return AuthResponse.fail("密码至少6位");
        }
        if (userRepository.existsByUsername(req.getUsername())) {
            return AuthResponse.fail("用户名已存在");
        }

        User user = new User();
        user.setUsername(req.getUsername().trim());
        user.setPassword(hashPassword(req.getPassword()));
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        return AuthResponse.success("注册成功", token, user.getId());
    }

    public AuthResponse login(LoginRequest req) {
        if (req.getUsername() == null || req.getPassword() == null) {
            return AuthResponse.fail("用户名和密码不能为空");
        }

        Optional<User> opt = userRepository.findByUsername(req.getUsername());
        if (opt.isEmpty()) {
            return AuthResponse.fail("用户名或密码错误");
        }

        User user = opt.get();
        if (!user.getPassword().equals(hashPassword(req.getPassword()))) {
            return AuthResponse.fail("用户名或密码错误");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        return AuthResponse.success("登录成功", token, user.getId());
    }

    public Long validateToken(String token) {
        if (token == null || token.isEmpty()) return null;
        try {
            if (jwtUtil.isTokenExpired(token)) return null;
            return jwtUtil.getUserId(token);
        } catch (Exception e) {
            return null;
        }
    }

    /** SHA-256 + 固定 salt（简单但安全） */
    private String hashPassword(String password) {
        try {
            String salted = password + "life-assistant-salt";
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(salted.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("密码加密失败", e);
        }
    }
}
