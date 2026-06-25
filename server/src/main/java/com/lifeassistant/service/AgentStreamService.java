package com.lifeassistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifeassistant.agent.ToolRegistry;
import com.lifeassistant.config.AiConfig;
import com.lifeassistant.dto.ActionCommand;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class AgentStreamService {

    private static final Logger log = LoggerFactory.getLogger(AgentStreamService.class);

    @Value("${deepseek.api-key}")
    private String apiKey;

    @Value("${deepseek.base-url}")
    private String baseUrl;

    @Value("${deepseek.model}")
    private String model;

    @Autowired
    private ToolRegistry toolRegistry;

    @Autowired
    private Map<Long, ChatMemory> conversationMemories;

    @Autowired
    private ConversationService conversationService;

    private final ObjectMapper mapper = new ObjectMapper();

    public SseEmitter streamChat(Long conversationId, String userMessage) {
        SseEmitter emitter = new SseEmitter(0L);
        ChatMemory memory = AiConfig.getOrCreateMemory(conversationMemories, conversationId);

        // 构建消息列表
        List<Map<String, String>> messages = buildMessages(conversationId, userMessage);

        // 异步流式调用
        new Thread(() -> {
            StringBuilder fullReply = new StringBuilder();
            try {
                HttpURLConnection conn = openStreamConnection(messages);
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6);
                        if ("[DONE]".equals(data)) break;

                        JsonNode node = mapper.readTree(data);
                        JsonNode choices = node.path("choices");
                        if (choices.size() > 0) {
                            JsonNode delta = choices.get(0).path("delta");
                            String content = delta.path("content").asText("");
                            if (!content.isEmpty()) {
                                fullReply.append(content);
                                emitter.send(SseEmitter.event().data(content));
                            }
                        }
                    }
                }
                reader.close();

                String aiText = fullReply.toString();
                String cleanReply = toolRegistry.removeActionsBlock(aiText);
                List<ActionCommand> actions = toolRegistry.parseActions(aiText);

                // 更新记忆
                memory.add(UserMessage.from(userMessage));
                memory.add(AiMessage.from(cleanReply));
                conversationService.saveMessage(conversationId, "user", userMessage);
                conversationService.saveMessage(conversationId, "assistant", cleanReply);

                // 发送 actionCommands
                emitter.send(SseEmitter.event().name("actions").data(actionsToJson(actions)));
                emitter.complete();

            } catch (Exception e) {
                log.error("Stream error", e);
                try { emitter.send(SseEmitter.event().name("error")
                        .data("{\"error\":\"" + e.getMessage() + "\"}")); } catch (IOException ignored) {}
                emitter.completeWithError(e);
            }
        }).start();

        return emitter;
    }

    private List<Map<String, String>> buildMessages(Long conversationId, String userMessage) {
        ChatMemory memory = AiConfig.getOrCreateMemory(conversationMemories, conversationId);
        List<Map<String, String>> msgs = new ArrayList<>();

        // 系统提示
        msgs.add(Map.of("role", "system", "content", buildSystemPrompt()));

        // 时间注入
        var now = java.time.LocalDateTime.now();
        String timeInfo = String.format("现在时间是 %s %s，你现在有实时时钟。用户问时间、设置闹钟时据此推算日期。",
                now.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                now.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.CHINESE));
        msgs.add(Map.of("role", "system", "content", timeInfo));

        // 对话历史
        for (ChatMessage msg : memory.messages()) {
            if (msg instanceof UserMessage) {
                msgs.add(Map.of("role", "user", "content", ((UserMessage) msg).singleText()));
            } else if (msg instanceof AiMessage) {
                msgs.add(Map.of("role", "assistant", "content", ((AiMessage) msg).text()));
            }
        }

        msgs.add(Map.of("role", "user", "content", userMessage));
        return msgs;
    }

    private HttpURLConnection openStreamConnection(List<Map<String, String>> messages) throws Exception {
        String body = mapper.writeValueAsString(Map.of(
                "model", model,
                "messages", messages,
                "stream", true,
                "temperature", 0.7,
                "max_tokens", 2000));

        HttpURLConnection conn = (HttpURLConnection) URI.create(baseUrl + "/chat/completions").toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "text/event-stream");
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(60000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }
        return conn;
    }

    private String buildSystemPrompt() {
        return """
            你是贴心的 AI 生活助手「小伴」。
            ⚠️ 调用 set_alarm 时，time 参数必须是 "yyyy-MM-dd HH:mm" 格式，禁止只输出 HH:mm！
            回复风格：温暖、简洁、有用。不要输出 ---ACTIONS--- 块，总是直接回复。
            """ + toolRegistry.getToolDescriptions();
    }

    private String actionsToJson(List<ActionCommand> actions) {
        if (actions.isEmpty()) return "[]";
        try {
            return mapper.writeValueAsString(actions);
        } catch (Exception e) {
            return "[]";
        }
    }
}
