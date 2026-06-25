package com.lifeassistant.service;

import com.lifeassistant.model.Reminder;
import com.lifeassistant.repository.ReminderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 提醒事件服务 — 存储和管理从对话中提取的用户事件
 */
@Service
public class ReminderService {

    @Autowired
    private ReminderRepository reminderRepository;

    /**
     * 从 AI 提取的信息创建提醒
     *
     * @param userId  用户ID
     * @param event   事件描述（如 "数学考试"）
     * @param dateStr 日期字符串，支持多种格式
     * @param remindBeforeHours 提前多少小时提醒
     */
    public Reminder createReminder(Long userId, String event, String dateStr, int remindBeforeHours) {
        LocalDateTime eventDate = parseEventDate(dateStr);

        Reminder reminder = new Reminder();
        reminder.setUserId(userId != null ? userId : 1L); // 默认用户1
        reminder.setEvent(event);
        reminder.setEventDate(eventDate);
        reminder.setRemindBeforeHours(remindBeforeHours);
        reminder.setRemindAt(eventDate.minusHours(remindBeforeHours));
        reminder.setStatus("pending");

        return reminderRepository.save(reminder);
    }

    /**
     * 获取所有到期待推送的提醒
     */
    public List<Reminder> getDueReminders() {
        return reminderRepository.findByStatusAndRemindAtBeforeAndPushedFalse(
                "pending", LocalDateTime.now());
    }

    /**
     * 标记提醒为已推送
     */
    public void markPushed(Reminder reminder, String pushMessage) {
        reminder.setPushed(true);
        reminder.setPushMessage(pushMessage);
        reminderRepository.save(reminder);
    }

    /**
     * 标记提醒为已完成
     */
    public void markDone(Reminder reminder) {
        reminder.setStatus("done");
        reminderRepository.save(reminder);
    }

    /**
     * 获取用户的待提醒事件列表
     */
    public List<Reminder> getUserReminders(Long userId) {
        return reminderRepository.findByUserIdAndStatusOrderByEventDateAsc(userId, "pending");
    }

    /**
     * 获取所有待提醒事件
     */
    public List<Reminder> getAllPending() {
        return reminderRepository.findByStatusOrderByEventDateAsc("pending");
    }

    /**
     * 解析多种日期格式
     * 支持: "2026-06-24", "2026-06-24 14:00", "06-24", "下周三" → 需要 AI 传具体日期
     */
    private LocalDateTime parseEventDate(String dateStr) {
        dateStr = dateStr.trim();
        try {
            // yyyy-MM-dd HH:mm
            if (dateStr.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}")) {
                return LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            }
            // yyyy-MM-dd
            if (dateStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return LocalDateTime.of(LocalDate.parse(dateStr), LocalTime.of(9, 0)); // 默认上午9点
            }
            // MM-dd（默认今年）
            if (dateStr.matches("\\d{2}-\\d{2}")) {
                LocalDate date = LocalDate.of(LocalDate.now().getYear(),
                        Integer.parseInt(dateStr.substring(0, 2)),
                        Integer.parseInt(dateStr.substring(3, 5)));
                if (date.isBefore(LocalDate.now())) {
                    date = date.plusYears(1); // 跨年
                }
                return LocalDateTime.of(date, LocalTime.of(9, 0));
            }
        } catch (Exception e) {
            // 解析失败，默认明天
        }
        return LocalDateTime.now().plusDays(1).withHour(9).withMinute(0);
    }
}
