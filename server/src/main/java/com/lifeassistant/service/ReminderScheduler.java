package com.lifeassistant.service;

import com.lifeassistant.model.Reminder;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 提醒定时调度器 — 每分钟扫描一次待提醒事件
 *
 * 发现到期提醒 → AI 生成温馨提醒语 → WebSocket 实时推送 → 标记已推送
 */
@Component
@EnableScheduling
public class ReminderScheduler {

    @Autowired
    private ReminderService reminderService;

    @Autowired
    private WebSocketNotificationService wsService;

    @Autowired
    private OpenAiChatModel chatModel;

    @Scheduled(fixedRate = 60000)
    public void scanReminders() {
        List<Reminder> dueList = reminderService.getDueReminders();

        for (Reminder reminder : dueList) {
            try {
                String prompt = String.format(
                    "用户有一个即将到来的事件：「%s」，事件日期是 %s。以贴心助手「小伴」的身份，"
                    + "生成一条温馨的提醒消息（50字以内，温暖简洁）。直接说内容。",
                    reminder.getEvent(),
                    reminder.getEventDate().toLocalDate().toString()
                );

                String pushMessage = chatModel.chat(prompt);

                // WebSocket 实时推送到所有在线的客户端
                wsService.pushToAll("📅 事件提醒", pushMessage, reminder.getUserId());

                reminderService.markPushed(reminder, pushMessage);
                System.out.println("[Scheduler] WebSocket 已推送: " + reminder.getEvent());

            } catch (Exception e) {
                System.err.println("[Scheduler] 推送失败: " + e.getMessage());
            }
        }
    }
}
