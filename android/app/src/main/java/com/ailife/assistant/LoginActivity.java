package com.ailife.assistant;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.ailife.assistant.network.ApiClient;
import com.ailife.assistant.network.model.LoginResponse;
import com.ailife.assistant.util.TokenManager;

public class LoginActivity extends AppCompatActivity {

    private EditText inputUsername;
    private EditText inputPassword;
    private Button btnLogin;
    private Button btnRegister;

    private static final String BASE_URL = "http://39.105.51.168:8082";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        TokenManager tokenManager = new TokenManager(this);

        // 如果已经登录过，直接跳到主页
        if (tokenManager.isLoggedIn()) {
            goToMain(tokenManager.getToken(), tokenManager.getUserId());
            return;
        }

        inputUsername = findViewById(R.id.input_username);
        inputPassword = findViewById(R.id.input_password);
        btnLogin = findViewById(R.id.btn_login);
        btnRegister = findViewById(R.id.btn_register);

        // 登录
        btnLogin.setOnClickListener(v -> {
            String username = inputUsername.getText().toString().trim();
            String password = inputPassword.getText().toString().trim();
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "请填写用户名和密码", Toast.LENGTH_SHORT).show();
                return;
            }
            doLogin(username, password, tokenManager);
        });

        // 注册
        btnRegister.setOnClickListener(v -> {
            String username = inputUsername.getText().toString().trim();
            String password = inputPassword.getText().toString().trim();
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "请填写用户名和密码", Toast.LENGTH_SHORT).show();
                return;
            }
            doRegister(username, password, tokenManager);
        });
    }

    private void doLogin(String username, String password, TokenManager tokenManager) {
        setLoading(true);
        new Thread(() -> {
            try {
                ApiClient api = new ApiClient(BASE_URL, null);
                LoginResponse resp = api.login(username, password);
                tokenManager.saveLogin(resp.getToken(), resp.getUserId());
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(this, "登录成功", Toast.LENGTH_SHORT).show();
                    goToMain(resp.getToken(), resp.getUserId());
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(this, "登录失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void doRegister(String username, String password, TokenManager tokenManager) {
        setLoading(true);
        new Thread(() -> {
            try {
                ApiClient api = new ApiClient(BASE_URL, null);
                LoginResponse resp = api.register(username, password);
                tokenManager.saveLogin(resp.getToken(), resp.getUserId());
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(this, "注册成功，已自动登录", Toast.LENGTH_SHORT).show();
                    goToMain(resp.getToken(), resp.getUserId());
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(this, "注册失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void setLoading(boolean loading) {
        runOnUiThread(() -> {
            btnLogin.setEnabled(!loading);
            btnRegister.setEnabled(!loading);
            btnLogin.setText(loading ? "请稍候..." : "登 录");
        });
    }

    private void goToMain(String token, long userId) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("token", token);
        intent.putExtra("userId", userId);
        startActivity(intent);
        finish();
    }
}
