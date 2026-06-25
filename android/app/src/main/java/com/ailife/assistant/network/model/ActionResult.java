package com.ailife.assistant.network.model;

/**
 * Android 执行完动作后回传给后端的结果
 */
public class ActionResult {
    private String commandId;
    private boolean success;
    private String message;

    public ActionResult(String commandId, boolean success, String message) {
        this.commandId = commandId;
        this.success = success;
        this.message = message;
    }

    public String getCommandId() { return commandId; }
    public void setCommandId(String commandId) { this.commandId = commandId; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
