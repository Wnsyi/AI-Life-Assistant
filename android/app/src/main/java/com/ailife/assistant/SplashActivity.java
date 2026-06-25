package com.ailife.assistant;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;

import com.ailife.assistant.util.TokenManager;

/**
 * 启动页 — 判断登录态，自动跳转
 */
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // 1.5 秒后跳转
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            TokenManager tm = new TokenManager(this);
            if (tm.isLoggedIn()) {
                // 已登录 → 直接进聊天页
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("token", tm.getToken());
                intent.putExtra("userId", tm.getUserId());
                startActivity(intent);
            } else {
                // 未登录 → 登录页
                startActivity(new Intent(this, LoginActivity.class));
            }
            finish();
        }, 1500);
    }
}
