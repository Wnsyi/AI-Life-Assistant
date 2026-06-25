package com.ailife.assistant.network.model;

import java.util.List;

/**
 * 后端的聊天响应
 */
public class ChatResponse {
    private String reply;
    private List<ActionCommand> actionCommands;
    private String conversationId;
    private String timestamp;

    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }
    public List<ActionCommand> getActionCommands() { return actionCommands; }
    public void setActionCommands(List<ActionCommand> actionCommands) { this.actionCommands = actionCommands; }
    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
