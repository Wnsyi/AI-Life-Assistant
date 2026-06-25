package com.lifeassistant.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifeassistant.config.AiConfig;
import com.lifeassistant.dto.ActionResult;
import com.lifeassistant.dto.ChatResponse;
import com.lifeassistant.exception.BusinessException;
import com.lifeassistant.model.Conversation;
import com.lifeassistant.model.Message;
import com.lifeassistant.service.AgentService;
import com.lifeassistant.service.AgentStreamService;
import com.lifeassistant.service.ConversationService;
import com.lifeassistant.service.ReminderService;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ChatController {

    @Autowired
    private OpenAiChatModel chatModel;

    @Autowired
    private Map<Long, ChatMemory> conversationMemories;

    @Autowired
    private AgentService agentService;

    @Autowired
    private AgentStreamService agentStreamService;

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private ReminderService reminderService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== 工具接口 ====================

    @GetMapping("/ping")
    public String ping() { return "pong!"; }

    // ==================== 对话管理 ====================

    /** 获取用户对话列表 */
    @GetMapping("/conversations")
    public List<Conversation> getConversations(@RequestParam Long userId) {
        return conversationService.getUserConversations(userId);
    }

    /** 创建新对话 */
    @PostMapping("/conversations")
    public Conversation createConversation(@RequestBody String body,
                                           HttpServletRequest request) {
        try {
            Map<String, String> m = objectMapper.readValue(body, new TypeReference<>() {});
            Long userId = getUserId(request);  // 用认证用户的ID
            String title = m.getOrDefault("title", "新对话");
            return conversationService.createConversation(userId, title);
        } catch (Exception e) {
            throw new BusinessException(400, "请求格式错误");
        }
    }

    /** 删除对话 */
    @DeleteMapping("/conversations/{id}")
    public String deleteConversation(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getUserId(request);
        conversationService.deleteConversation(userId, id);
        conversationMemories.remove(id);
        return "{\"status\":\"ok\"}";
    }

    /** 修改对话标题 */
    @PutMapping("/conversations/{id}")
    public Conversation updateTitle(@PathVariable Long id,
            @RequestBody String body, HttpServletRequest request) {
        Map<String, String> m = parseBody(body);
        Long userId = getUserId(request);
        String title = m.getOrDefault("title", "");
        if (title.isBlank()) throw new BusinessException(400, "标题不能为空");
        return conversationService.updateTitle(userId, id, title);
    }

    /** 获取对话历史消息 */
    @GetMapping("/conversations/{id}/messages")
    public List<Message> getMessages(@PathVariable Long id) {
        return conversationService.getMessages(id);
    }

    // ==================== 聊天接口 ====================

    /** 简单对话 */
    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody String body, HttpServletRequest request) {
        Map<String, String> m = parseBody(body);
        String message = m.getOrDefault("message", "");
        Long userId = getUserId(request);
        Long convId = getOrCreateConversation(m, userId);

        // 自动命名
        conversationService.autoTitleIfNew(convId, message);

        ChatMemory memory = AiConfig.getOrCreateMemory(conversationMemories, convId);
        memory.add(dev.langchain4j.data.message.UserMessage.from(message));

        String prompt = buildContextFromMemory(memory) + "\n用户: " + message + "\n助手: ";
        String aiText = chatModel.chat(prompt);

        memory.add(dev.langchain4j.data.message.AiMessage.from(aiText));
        conversationService.saveMessage(convId, "user", message);
        conversationService.saveMessage(convId, "assistant", aiText);

        return new ChatResponse(aiText, List.of(), String.valueOf(convId), "");
    }

    /** Agent 对话 */
    @PostMapping("/agent-chat")
    public ChatResponse agentChat(@RequestBody String body, HttpServletRequest request) {
        Map<String, String> m = parseBody(body);
        String message = m.getOrDefault("message", "");
        Long userId = getUserId(request);
        Long convId = getOrCreateConversation(m, userId);

        // 自动命名
        conversationService.autoTitleIfNew(convId, message);

        return agentService.processMessage(convId, message);
    }

    /** 流式 Agent 对话（SSE — Server-Sent Events） */
    @GetMapping(value = "/agent-chat-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter agentChatStream(@RequestParam String message,
            @RequestParam(defaultValue = "0") Long conversationId,
            HttpServletRequest request) {
        try {
            Long userId = getUserId(request);
            if (conversationId == 0) {
                var c = conversationService.createConversation(userId, "新对话");
                conversationId = c.getId();
            }
            conversationService.autoTitleIfNew(conversationId, message);
            return agentStreamService.streamChat(conversationId, message);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    /** 清除对话记忆 */
    @PostMapping("/forget")
    public String forget(@RequestBody String body, HttpServletRequest request) {
        Map<String, String> m = parseBody(body);
        Long userId = getUserId(request);
        Long convId = getOrCreateConversation(m, userId);

        conversationMemories.remove(convId);
        conversationService.deleteConversation(userId, convId);
        return "{\"status\":\"ok\",\"message\":\"对话已清除\"}";
    }

    // ==================== 其他 ====================

    @PostMapping("/action-result")
    public String actionResult(@RequestBody String body) {
        try {
            ActionResult result = objectMapper.readValue(body, ActionResult.class);
            return agentService.handleActionResult(result);
        } catch (Exception e) {
            throw new BusinessException(400, "请求格式错误");
        }
    }

    @GetMapping("/pending-reminders")
    public String pendingReminders() {
        return "{\"reminders\": " + reminderService.getAllPending().toString() + "}";
    }

    @PostMapping("/test-trigger")
    public String testTrigger() {
        reminderService.createReminder(1L, "【测试】即时提醒",
                java.time.LocalDateTime.now().plusMinutes(1)
                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), 0);
        return "{\"status\":\"ok\"}";
    }

    @GetMapping("/memory-size")
    public String memorySize(@RequestParam(defaultValue = "1") Long convId) {
        ChatMemory m = conversationMemories.get(convId);
        int count = m != null ? m.messages().size() : 0;
        return "{\"messageCount\": " + count + "}";
    }

    // ==================== 辅助方法 ====================

    private Map<String, String> parseBody(String body) {
        try {
            return objectMapper.readValue(body, new TypeReference<>() {});
        } catch (Exception e) {
            throw new BusinessException(400, "JSON 格式错误");
        }
    }

    /** 从拦截器注入的 userId 中获取 */
    private Long getUserId(HttpServletRequest request) {
        Object uid = request.getAttribute("userId");
        return uid != null ? (Long) uid : 1L;
    }

    /** 获取或创建 conversationId */
    private Long getOrCreateConversation(Map<String, String> m, Long userId) {
        String convIdStr = m.get("conversationId");
        if (convIdStr != null && !convIdStr.isEmpty() && !"0".equals(convIdStr)) {
            return Long.valueOf(convIdStr);
        }
        // 自动创建新对话
        Conversation c = conversationService.createConversation(userId, "新对话");
        return c.getId();
    }

    private String buildContextFromMemory(ChatMemory memory) {
        StringBuilder sb = new StringBuilder("以下是与用户的对话历史：\n");
        for (dev.langchain4j.data.message.ChatMessage msg : memory.messages()) {
            if (msg instanceof dev.langchain4j.data.message.UserMessage um) {
                sb.append("用户: ").append(um.singleText()).append("\n");
            } else if (msg instanceof dev.langchain4j.data.message.AiMessage am) {
                sb.append("助手: ").append(am.text()).append("\n");
            }
        }
        return sb.toString();
    }
}
