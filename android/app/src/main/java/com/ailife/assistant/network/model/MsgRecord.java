package com.ailife.assistant.network.model;

/**
 * 后端返回的消息记录
 */
public class MsgRecord {
    private long id;
    private String content;
    private String role;       // "user" 或 "assistant"
    private long conversationId;
    private String createdAt;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public long getConversationId() { return conversationId; }
    public void setConversationId(long conversationId) { this.conversationId = conversationId; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
