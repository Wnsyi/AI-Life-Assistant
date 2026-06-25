package com.ailife.assistant.action;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import com.ailife.assistant.network.ApiClient;
import com.ailife.assistant.network.model.ActionCommand;
import com.ailife.assistant.network.model.ActionResult;

import java.util.List;

/**
 * 动作分发器 — 收到后端 actionCommands 后，分发到对应 Helper 执行，然后回传结果
 */
public class ActionExecutor {

    private final AlarmHelper alarmHelper;
    private final AlarmCanceller alarmCanceller;
    private final NotificationHelper notificationHelper;
    private final AppLauncherHelper appLauncherHelper;
    private final ApiClient apiClient;
    private final Activity activity;

    public ActionExecutor(Activity activity, ApiClient apiClient) {
        this.activity = activity;
        this.alarmHelper = new AlarmHelper(activity);
        this.alarmCanceller = new AlarmCanceller(activity);
        this.notificationHelper = new NotificationHelper(activity);
        this.appLauncherHelper = new AppLauncherHelper(activity);
        this.apiClient = apiClient;
    }

    /**
     * 执行一组动作指令（在后台线程调用）
     * @param commands 动作列表
     * @param aiReply  AI 的回复文本（用于判断"明天"/"后天"等时间上下文）
     */
    public void execute(List<ActionCommand> commands, String aiReply) {
        if (commands == null || commands.isEmpty()) return;

        for (ActionCommand cmd : commands) {
            String resultMsg;
            try {
                resultMsg = executeOne(cmd, aiReply);
            } catch (Exception e) {
                resultMsg = "❌ 执行异常: " + e.getClass().getSimpleName();
            }

            final String finalResult = resultMsg;
            try {
                if (activity != null && !activity.isFinishing()) {
                    activity.runOnUiThread(() ->
                            Toast.makeText(activity, finalResult, Toast.LENGTH_LONG).show());
                }
            } catch (Exception ignored) {}

            if (cmd.getCommandId() != null && !cmd.getCommandId().isEmpty()) {
                try {
                    apiClient.reportActionResult(
                            new ActionResult(cmd.getCommandId(),
                                    !resultMsg.startsWith("❌"), resultMsg));
                } catch (Exception ignored) {}
            }
        }
    }

    private String executeOne(ActionCommand cmd, String aiReply) {
        switch (cmd.getAction()) {
            case "set_alarm":
                if ("cancel".equals(cmd.getParams() != null ? cmd.getParams().get("time") : null)) {
                    return alarmCanceller.cancelAlarm(
                            cmd.getParams() != null ? cmd.getParams().get("label") : null);
                }
                return alarmHelper.setAlarm(cmd.getParams(), aiReply);
            case "send_notification":
                return notificationHelper.sendNotification(cmd.getParams());
            case "open_app":
                return appLauncherHelper.openApp(cmd.getParams());
            default:
                return "❌ 未知动作: " + cmd.getAction();
        }
    }
}
