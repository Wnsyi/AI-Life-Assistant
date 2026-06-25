package com.lifeassistant.service;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 通知推送服务 — 替换轮询方式
 *
 * 客户端连接 ws://localhost:8082/ws/notifications
 * 服务端有提醒时主动推送 JSON 消息
 */
@Service
public class WebSocketNotificationService extends TextWebSocketHandler {

    /** 所有已连接的客户端 */
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        System.out.println("[WebSocket] 新连接: " + session.getId() + " (总数: " + sessions.size() + ")");
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        System.out.println("[WebSocket] 断开: " + session.getId() + " (总数: " + sessions.size() + ")");
    }

    /**
     * 向所有连接的客户端推送消息
     *
     * @param title   通知标题
     * @param content 通知内容
     * @param userId  目标用户ID（暂无单用户推送，广播给所有人）
     */
    public void pushToAll(String title, String content, Long userId) {
        String json = String.format(
            "{\"type\":\"reminder\",\"title\":\"%s\",\"content\":\"%s\",\"userId\":%d,\"timestamp\":%d}",
            escape(title), escape(content), userId != null ? userId : 0, System.currentTimeMillis()
        );

        TextMessage message = new TextMessage(json);
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(message);
                } catch (IOException e) {
                    System.err.println("[WebSocket] 发送失败: " + e.getMessage());
                }
            }
        }
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
