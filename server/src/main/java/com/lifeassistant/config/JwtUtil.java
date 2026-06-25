package com.lifeassistant.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private final String secret;

    // JWT 密钥由环境变量 JWT_SECRET 注入，默认值仅开发使用
    public JwtUtil(@Value("${jwt.secret}") String secret) {
        if (secret == null || secret.isBlank() || secret.length() < 32) {
            throw new IllegalStateException(
                "JWT_SECRET 未设置或长度不足（需≥32字符）。请在环境变量或 .env 中设置。");
        }
        this.secret = secret;
    }

    private static final long EXPIRATION = 7 * 24 * 60 * 60 * 1000L; // 7天

    private SecretKey getKey() {
        // HMAC-SHA256 需要至少 256 bit = 32 bytes
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Long userId, String username) {
        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(getKey())
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenExpired(String token) {
        try {
            return parseToken(token).getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    public Long getUserId(String token) {
        return parseToken(token).get("userId", Long.class);
    }
}
