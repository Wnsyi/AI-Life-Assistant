package com.ailife.assistant.network;

import com.ailife.assistant.network.model.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import okhttp3.*;

/**
 * 后端 API 客户端 — 封装所有 HTTP 请求
 *
 * 使用方式：
 *   ApiClient api = new ApiClient("http://39.105.51.168:8082", token);
 *   ChatResponse resp = api.sendMessage("你好", 1);
 */
public class ApiClient {
    private final String baseUrl;
    private final OkHttpClient httpClient;
    private final Gson gson;

    public ApiClient(String baseUrl, String token) {
        this.baseUrl = baseUrl;
        this.gson = new Gson();

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        // 如果有 token，自动带在请求头
        if (token != null && !token.isEmpty()) {
            builder.addInterceptor(chain -> {
                Request req = chain.request().newBuilder()
                        .header("Authorization", "Bearer " + token)
                        .build();
                return chain.proceed(req);
            });
        }
        this.httpClient = builder.build();
    }

    // ==================== 认证 ====================

    /** 注册 */
    public LoginResponse register(String username, String password) throws IOException {
        String json = post("/api/register", gson.toJson(new LoginRequest(username, password)));
        checkAuthError(json);
        return gson.fromJson(json, LoginResponse.class);
    }

    /** 登录 */
    public LoginResponse login(String username, String password) throws IOException {
        String json = post("/api/login", gson.toJson(new LoginRequest(username, password)));
        checkAuthError(json);
        return gson.fromJson(json, LoginResponse.class);
    }

    /** 检查后端返回的业务错误码 */
    private void checkAuthError(String json) throws IOException {
        try {
            Map<String, Object> map = gson.fromJson(json, Map.class);
            Object code = map.get("code");
            if (code != null && ((Number) code).intValue() != 200) {
                String msg = map.getOrDefault("message", "未知错误").toString();
                throw new IOException(msg);
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception ignored) {}
    }

    // ==================== 对话 ====================

    /** 发送消息给 AI Agent */
    public ChatResponse sendMessage(String message, long conversationId) throws IOException {
        String body = gson.toJson(new ChatRequest(message, conversationId));
        String json = post("/api/agent-chat", body);
        return gson.fromJson(json, ChatResponse.class);
    }

    /** 获取对话列表 */
    public List<Conversation> getConversations(long userId) throws IOException {
        String json = get("/api/conversations?userId=" + userId);
        Type listType = new TypeToken<List<Conversation>>() {}.getType();
        return gson.fromJson(json, listType);
    }

    /** 获取对话历史消息 */
    public List<MsgRecord> getMessages(long conversationId) throws IOException {
        String json = get("/api/conversations/" + conversationId + "/messages");
        Type listType = new TypeToken<List<MsgRecord>>() {}.getType();
        return gson.fromJson(json, listType);
    }

    /** 创建新对话 */
    public Conversation createConversation(long userId, String title) throws IOException {
        String body = "{\"userId\":" + userId + ",\"title\":\"" + title + "\"}";
        String json = post("/api/conversations", body);
        return gson.fromJson(json, Conversation.class);
    }

    /** 删除对话 */
    public void deleteConversation(long conversationId, long userId) throws IOException {
        delete("/api/conversations/" + conversationId + "?userId=" + userId);
    }

    /** 重命名对话 */
    public void renameConversation(long conversationId, String title) throws IOException {
        String json = "{\"title\":\"" + title + "\"}";
        put("/api/conversations/" + conversationId, json);
    }

    /** 回传动作执行结果 */
    public String reportActionResult(ActionResult result) throws IOException {
        return post("/api/action-result", gson.toJson(result));
    }

    // ==================== 流式聊天 ====================

    public interface StreamCallback {
        void onToken(String token);
        void onComplete(List<ActionCommand> actions);
        void onError(String error);
    }

    public void streamChat(String message, long conversationId, StreamCallback callback) {
        try {
            String encoded = URLEncoder.encode(message, "UTF-8");
            String url = baseUrl + "/api/agent-chat-stream?message=" + encoded
                    + "&conversationId=" + conversationId;

            Request req = new Request.Builder()
                    .url(url)
                    .get()
                    .header("Accept", "text/event-stream")
                    .build();

            httpClient.newCall(req).enqueue(new okhttp3.Callback() {
                @Override
                public void onResponse(okhttp3.Call call, Response response) {
                    try (Response resp = response) {
                        if (!resp.isSuccessful()) {
                            callback.onError("流式请求失败 " + resp.code());
                            return;
                        }
                        BufferedReader reader = new BufferedReader(
                                new InputStreamReader(resp.body().byteStream(), "UTF-8"));
                        StringBuilder textBuffer = new StringBuilder();
                        List<ActionCommand> actions = new ArrayList<>();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.startsWith("data:")) {
                                String token = line.substring(5).trim();
                                if (token.isEmpty()) continue;
                                // 尝试解析 JSON（actionCommands）
                                if (token.startsWith("{")) {
                                    try {
                                        ChatResponse cr = gson.fromJson(token, ChatResponse.class);
                                        if (cr.getActionCommands() != null) {
                                            actions.addAll(cr.getActionCommands());
                                        }
                                    } catch (Exception ignored) {}
                                } else {
                                    textBuffer.append(token);
                                    callback.onToken(token);
                                }
                            }
                        }
                        callback.onComplete(actions);
                    } catch (Exception e) {
                        callback.onError("流读取异常: " + e.getMessage());
                    }
                }

                @Override
                public void onFailure(okhttp3.Call call, IOException e) {
                    callback.onError("连接失败: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            callback.onError("请求构建失败: " + e.getMessage());
        }
    }

    // ==================== 底层 HTTP ====================

    private String get(String path) throws IOException {
        Request req = new Request.Builder()
                .url(baseUrl + path)
                .get()
                .build();
        try (Response resp = httpClient.newCall(req).execute()) {
            return handleResponse(resp);
        }
    }

    private String post(String path, String jsonBody) throws IOException {
        RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));
        Request req = new Request.Builder()
                .url(baseUrl + path)
                .post(body)
                .build();
        try (Response resp = httpClient.newCall(req).execute()) {
            return handleResponse(resp);
        }
    }

    private String put(String path, String jsonBody) throws IOException {
        RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));
        Request req = new Request.Builder()
                .url(baseUrl + path)
                .put(body)
                .build();
        try (Response resp = httpClient.newCall(req).execute()) {
            return handleResponse(resp);
        }
    }

    private String delete(String path) throws IOException {
        Request req = new Request.Builder()
                .url(baseUrl + path)
                .delete()
                .build();
        try (Response resp = httpClient.newCall(req).execute()) {
            return handleResponse(resp);
        }
    }

    private String handleResponse(Response resp) throws IOException {
        if (!resp.isSuccessful()) {
            String errBody = resp.body() != null ? resp.body().string() : "";
            throw new IOException("请求失败 " + resp.code() + ": " + errBody);
        }
        return resp.body() != null ? resp.body().string() : "{}";
    }

}
