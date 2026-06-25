package com.ailife.assistant.network.model;

import java.util.Map;

/**
 * 后端返回的动作指令（闹钟、通知、打开App）
 */
public class ActionCommand {
    private String commandId;
    private String action;
    private Map<String, String> params;

    public String getCommandId() { return commandId; }
    public void setCommandId(String commandId) { this.commandId = commandId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public Map<String, String> getParams() { return params; }
    public void setParams(Map<String, String> params) { this.params = params; }
}
