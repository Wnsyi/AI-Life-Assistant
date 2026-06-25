package com.ailife.assistant.network.model;

/**
 * 发送给后端的聊天请求
 */
public class ChatRequest {
    private String message;
    private long conversationId;  // 0 表示让后端自动创建新对话
    private String deviceType = "android";

    public ChatRequest(String message, long conversationId) {
        this.message = message;
        this.conversationId = conversationId;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public long getConversationId() { return conversationId; }
    public void setConversationId(long conversationId) { this.conversationId = conversationId; }
    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }
}
