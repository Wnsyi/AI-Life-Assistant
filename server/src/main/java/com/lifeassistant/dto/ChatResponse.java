package com.lifeassistant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 聊天响应体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    /** AI 的文字回复 */
    private String reply;
    /** 需要手机执行的动作指令（闹钟、通知等），暂时为空 */
    private List<ActionCommand> actionCommands;
    /** 会话ID */
    private String conversationId;
    /** 时间戳 */
    private String timestamp;

    /**
     * 快速构造一个纯文本回复（没有动作指令）
     */
    public static ChatResponse ofText(String reply) {
        return new ChatResponse(reply, List.of(), "", "");
    }
}
