package com.lifeassistant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private int code;
    private String message;
    private String token;
    private Long userId;

    public static AuthResponse success(String message, String token, Long userId) {
        return new AuthResponse(200, message, token, userId);
    }

    public static AuthResponse fail(String message) {
        return new AuthResponse(400, message, null, null);
    }
}
