package com.lifeassistant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 动作指令 — 发给 Android 客户端执行
 *
 * 完整闭环：
 *   Server → Android: {"commandId":"cmd_xxx", "action":"set_alarm", "params":{...}}
 *   Android → Server: {"commandId":"cmd_xxx", "success":true, "message":"闹钟已设置"}
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActionCommand {
    /** 唯一指令ID（用于结果回传匹配） */
    private String commandId;
    /** 动作类型：set_alarm, send_notification, open_app 等 */
    private String action;
    /** 动作参数 */
    private Map<String, String> params;
}
