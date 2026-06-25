package com.lifeassistant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Android 回传的工具执行结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActionResult {
    /** 对应 ActionCommand 的 commandId */
    private String commandId;
    /** 是否执行成功 */
    private boolean success;
    /** 执行结果描述（如 "闹钟已设置" / "权限被拒绝"） */
    private String message;
}
