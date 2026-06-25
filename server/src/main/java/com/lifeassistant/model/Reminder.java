package com.lifeassistant.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 提醒事件表 — 存储从对话中提取的用户事件
 *
 * 比如用户说"下周三考试" → AI 提取后存入此表
 * 定时任务扫描此表，到期时主动推送提醒
 */
@Entity
@Table(name = "reminders")
@Data
@NoArgsConstructor
public class Reminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 用户ID（关联 users 表） */
    @Column(name = "user_id")
    private Long userId;

    /** 事件描述（如 "数学考试"、"张三生日"） */
    @Column(nullable = false, length = 255)
    private String event;

    /** 事件日期（哪天发生） */
    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    /** 提前多久提醒（小时），默认 24（提前一天） */
    @Column(name = "remind_before_hours")
    private Integer remindBeforeHours = 24;

    /** 提醒时间（定时任务计算得出） */
    @Column(name = "remind_at")
    private LocalDateTime remindAt;

    /** 状态：pending(等待提醒) / reminded(已提醒) / done(已完成) / cancelled(已取消) */
    @Column(nullable = false, length = 20)
    private String status = "pending";

    /** 创建时间 */
    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    /** 是否已推送过 */
    @Column(name = "pushed")
    private Boolean pushed = false;

    /** 推送消息内容 */
    @Column(name = "push_message", columnDefinition = "TEXT")
    private String pushMessage;
}
