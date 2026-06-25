package com.lifeassistant.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 聊天请求体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    /** 用户ID（暂时可以不传） */
    private String userId;
    /** 会话ID（暂时可以不传） */
    private String conversationId;
    /** 用户发送的消息 */
    private String message;
    /** 设备类型：web / android */
    private String deviceType;
}
