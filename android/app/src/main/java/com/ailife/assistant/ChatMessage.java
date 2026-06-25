package com.ailife.assistant;

import com.ailife.assistant.network.model.ActionCommand;
import java.util.List;

/**
 * 聊天消息数据模型
 */
public class ChatMessage {
    public static final int TYPE_USER = 0;
    public static final int TYPE_AI = 1;

    private int type;
    private String content;
    private List<ActionCommand> actions;

    public ChatMessage(int type, String content) {
        this.type = type;
        this.content = content;
    }

    public ChatMessage(int type, String content, List<ActionCommand> actions) {
        this.type = type;
        this.content = content;
        this.actions = actions;
    }

    public int getType() { return type; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public List<ActionCommand> getActions() { return actions; }
    public void setActions(List<ActionCommand> actions) { this.actions = actions; }
    public boolean hasActions() { return actions != null && !actions.isEmpty(); }
}
