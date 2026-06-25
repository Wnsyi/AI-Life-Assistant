package com.lifeassistant.service;

import org.springframework.stereotype.Service;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 通知推送服务 — 基于内存队列的简易推送
 *
 * 客户端通过轮询 GET /api/poll-notifications 获取待推送消息
 * 后续可升级为 WebSocket
 */
@Service
public class NotificationPushService {

    /** 推送消息队列 */
    private final Queue<PushMessage> queue = new ConcurrentLinkedQueue<>();

    /**
     * 向队列中推送一条消息
     */
    public void push(String title, String content, Long userId) {
        queue.offer(new PushMessage(title, content, userId));
    }

    /**
     * 拉取一条待推送的消息（非阻塞）
     */
    public PushMessage poll() {
        return queue.poll();
    }

    /**
     * 队列是否为空
     */
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    /**
     * 推送消息体
     */
    public static class PushMessage {
        private String title;
        private String content;
        private Long userId;
        private long timestamp;

        public PushMessage(String title, String content, Long userId) {
            this.title = title;
            this.content = content;
            this.userId = userId;
            this.timestamp = System.currentTimeMillis();
        }

        public String getTitle() { return title; }
        public String getContent() { return content; }
        public Long getUserId() { return userId; }
        public long getTimestamp() { return timestamp; }

        public String toJson() {
            return String.format(
                "{\"title\":\"%s\",\"content\":\"%s\",\"userId\":%d,\"timestamp\":%d}",
                escape(title), escape(content), userId, timestamp
            );
        }

        private String escape(String s) {
            if (s == null) return "";
            return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
        }
    }
}
