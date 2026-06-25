package com.lifeassistant.agent;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 待执行 Action 存储 — 等待 Android 执行后回传结果
 *
 * 流程：
 * 1. Agent 生成 ActionCommand → 存入 PendingActionStore（状态：pending）
 * 2. Android 收到指令并执行
 * 3. Android 调用 /api/action-result 回传结果
 * 4. 服务器取出上下文 → 喂给 AI → AI 更新对世界的认知
 */
@Component
public class PendingActionStore {

    /** 待执行的 Action 上下文 */
    private final Map<String, PendingAction> store = new ConcurrentHashMap<>();

    /**
     * 注册一个待执行的 Action，返回唯一 commandId
     */
    public String register(PendingAction action) {
        String commandId = "cmd_" + UUID.randomUUID().toString().substring(0, 8);
        action.setCommandId(commandId);
        store.put(commandId, action);
        return commandId;
    }

    /**
     * 根据 commandId 获取并移除
     */
    public PendingAction take(String commandId) {
        return store.remove(commandId);
    }

    /**
     * 获取（不移除）
     */
    public PendingAction get(String commandId) {
        return store.get(commandId);
    }

    /**
     * 待执行的 Action 上下文
     */
    public static class PendingAction {
        private String commandId;
        private String action;
        private Map<String, String> params;
        private String conversationContext;
        private Long conversationId;

        public PendingAction() {}

        public PendingAction(String action, Map<String, String> params, String conversationContext, Long conversationId) {
            this.action = action;
            this.params = params;
            this.conversationContext = conversationContext;
            this.conversationId = conversationId;
        }

        public String getCommandId() { return commandId; }
        public void setCommandId(String id) { this.commandId = id; }
        public String getAction() { return action; }
        public void setAction(String a) { this.action = a; }
        public Map<String, String> getParams() { return params; }
        public void setParams(Map<String, String> p) { this.params = p; }
        public String getConversationContext() { return conversationContext; }
        public void setConversationContext(String c) { this.conversationContext = c; }
        public Long getConversationId() { return conversationId; }
        public void setConversationId(Long id) { this.conversationId = id; }
    }
}
