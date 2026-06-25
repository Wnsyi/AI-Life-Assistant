package com.ailife.assistant.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Token 和用户信息本地存储（SharedPreferences）
 */
public class TokenManager {
    private static final String PREF_NAME = "ai_life_prefs";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_USER_ID = "userId";

    private final SharedPreferences prefs;

    public TokenManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveLogin(String token, long userId) {
        prefs.edit()
                .putString(KEY_TOKEN, token)
                .putLong(KEY_USER_ID, userId)
                .apply();
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public long getUserId() {
        return prefs.getLong(KEY_USER_ID, 0);
    }

    public boolean isLoggedIn() {
        return getToken() != null;
    }

    public void logout() {
        prefs.edit().clear().apply();
    }
}
