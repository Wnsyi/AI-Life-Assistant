package com.lifeassistant.repository;

import com.lifeassistant.model.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 提醒事件数据访问层
 */
@Repository
public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    /** 查找待提醒且提醒时间已到的事件 */
    List<Reminder> findByStatusAndRemindAtBeforeAndPushedFalse(
            String status, LocalDateTime remindAt);

    /** 查找某个用户的所有待提醒事件 */
    List<Reminder> findByUserIdAndStatusOrderByEventDateAsc(Long userId, String status);

    /** 查找已过期的待提醒事件（到达事件日期但未标记完成） */
    List<Reminder> findByStatusAndEventDateBefore(String status, LocalDateTime date);

    /** 按状态查找所有提醒（不限用户） */
    List<Reminder> findByStatusOrderByEventDateAsc(String status);
}
