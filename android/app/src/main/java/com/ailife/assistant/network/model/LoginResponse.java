package com.ailife.assistant.network.model;

/**
 * 登录 / 注册返回的数据
 */
public class LoginResponse {
    private String token;
    private long userId;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }
}
