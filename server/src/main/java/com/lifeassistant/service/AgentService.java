package com.lifeassistant.service;

import com.lifeassistant.agent.PendingActionStore;
import com.lifeassistant.agent.ToolRegistry;
import com.lifeassistant.config.AiConfig;
import com.lifeassistant.dto.ActionCommand;
import com.lifeassistant.dto.ActionResult;
import com.lifeassistant.dto.ChatResponse;
import com.lifeassistant.model.Reminder;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);

    @Autowired
    private OpenAiChatModel chatModel;

    @Autowired
    private Map<Long, ChatMemory> conversationMemories;

    @Autowired
    private ToolRegistry toolRegistry;

    @Autowired
    private WeatherService weatherService;

    @Autowired
    private HolidayService holidayService;

    @Autowired
    private QuoteService quoteService;

    @Autowired
    private ReminderService reminderService;

    @Autowired
    private MemorySearchService memorySearch;

    @Autowired
    private ConversationService conversationService;

    @Autowired
    private PendingActionStore pendingActions;

    /**
     * 处理用户消息（对话级记忆）
     */
    public ChatResponse processMessage(Long conversationId, String userMessage) {
        ChatMemory memory = AiConfig.getOrCreateMemory(conversationMemories, conversationId);

        // 1. AI 分析意图
        String prompt1 = buildFirstRoundPrompt(conversationId, userMessage);
        String aiReply1 = chatModel.chat(prompt1);

        // 2. 解析工具调用
        List<ActionCommand> allActions = toolRegistry.parseActions(aiReply1);

        // 3. 事件提取
        List<ActionCommand> eventActions = extractEvents(userMessage);
        allActions.addAll(eventActions);

        // 4. 分离
        List<ActionCommand> serverActions = new ArrayList<>();
        List<ActionCommand> androidActions = new ArrayList<>();
        for (ActionCommand cmd : allActions) {
            if (toolRegistry.isServerSideTool(cmd.getAction())) {
                serverActions.add(cmd);
            } else {
                androidActions.add(cmd);
            }
        }

        // 5. 执行服务端工具
        StringBuilder toolResults = new StringBuilder();
        for (ActionCommand cmd : serverActions) {
            toolResults.append("工具 ").append(cmd.getAction())
                    .append(" 执行结果：").append(executeServerTool(cmd)).append("\n");
        }

        // 6. 结果喂回 AI
        String finalReply;
        if (toolResults.length() > 0) {
            finalReply = chatModel.chat(buildObservationPrompt(aiReply1, toolResults.toString()));
        } else {
            finalReply = toolRegistry.removeActionsBlock(aiReply1);
        }

        // 7. Android 工具分配 commandId + 自动补全日期
        for (ActionCommand cmd : androidActions) {
            // 修正闹钟时间格式：如果 AI 只给了 HH:mm，自动补为 yyyy-MM-dd HH:mm
            if ("set_alarm".equals(cmd.getAction()) && cmd.getParams() != null) {
                String time = cmd.getParams().get("time");
                if (time != null && !"cancel".equals(time) && time.matches("\\d{1,2}:\\d{2}")) {
                    String full = autoCompleteDateTime(time);
                    cmd.setParams(new java.util.HashMap<>(cmd.getParams())); // 确保可变
                    cmd.getParams().put("time", full);
                }
            }
            String cmdId = pendingActions.register(new PendingActionStore.PendingAction(
                    cmd.getAction(), cmd.getParams(),
                    buildConversationSummary(memory), conversationId));
            cmd.setCommandId(cmdId);
        }

        // 8. 更新记忆
        memory.add(UserMessage.from(userMessage));
        memory.add(AiMessage.from(finalReply));

        // 9. 持久化消息
        conversationService.saveMessage(conversationId, "user", userMessage);
        conversationService.saveMessage(conversationId, "assistant", finalReply);

        // 10. RAG
        memorySearch.indexMemory(userMessage, finalReply);

        return new ChatResponse(finalReply, androidActions,
                String.valueOf(conversationId), "");
    }

    /**
     * Android 结果回调 — 使用对话级记忆
     */
    public String handleActionResult(ActionResult result) {
        PendingActionStore.PendingAction pending = pendingActions.take(result.getCommandId());
        if (pending == null) {
            return "{\"status\":\"error\",\"message\":\"未找到对应的 action\"}";
        }

        Long convId = pending.getConversationId() != null ? pending.getConversationId() : 1L;
        ChatMemory memory = AiConfig.getOrCreateMemory(conversationMemories, convId);

        String statusEmoji = result.isSuccess() ? "✅" : "❌";
        String observation = String.format(
                "工具执行完毕：\n  工具名: %s\n  参数: %s\n  执行结果: %s %s\n",
                pending.getAction(), pending.getParams(),
                statusEmoji, result.getMessage() != null ? result.getMessage() : "");

        String aiPrompt = """
            以下是你之前帮用户调用的工具，现在执行结果出来了：
            %s
            对话上下文：
            %s
            请根据工具执行结果更新认知，回复要简洁自然。
            """.formatted(observation, pending.getConversationContext());

        String aiReply = chatModel.chat(aiPrompt);
        memory.add(AiMessage.from("（系统通知）工具 " + pending.getAction() + " 执行"
                + (result.isSuccess() ? "成功" : "失败") + ": " + result.getMessage()));
        memory.add(AiMessage.from(aiReply));

        return "{\"status\":\"ok\",\"ai_acknowledgment\":\"" + escapeJson(aiReply) + "\"}";
    }

    // ==================== Prompt 构建 ====================

    private String buildFirstRoundPrompt(Long conversationId, String userMessage) {
        ChatMemory memory = AiConfig.getOrCreateMemory(conversationMemories, conversationId);
        StringBuilder sb = new StringBuilder();
        sb.append(buildSystemPrompt()).append("\n\n");

        // ChatMemory 中的消息（对话级隔离）
        for (ChatMessage msg : memory.messages()) {
            if (msg instanceof UserMessage) {
                sb.append("用户: ").append(((UserMessage) msg).singleText()).append("\n");
            } else if (msg instanceof AiMessage) {
                sb.append("助手: ").append(((AiMessage) msg).text()).append("\n");
            }
        }

        sb.append("用户: ").append(userMessage).append("\n助手: ");
        return sb.toString();
    }

    private String buildObservationPrompt(String firstReply, String toolResults) {
        return """
            你之前回复了：%s
            工具执行结果：%s
            请根据结果生成自然回复，不要输出 ---ACTIONS--- 块。
            """.formatted(firstReply, toolResults);
    }

    private String buildConversationSummary(ChatMemory memory) {
        StringBuilder sb = new StringBuilder();
        for (ChatMessage msg : memory.messages()) {
            if (msg instanceof UserMessage) sb.append("用户: ").append(((UserMessage) msg).singleText()).append("\n");
            else if (msg instanceof AiMessage) sb.append("助手: ").append(((AiMessage) msg).text()).append("\n");
        }
        return sb.toString();
    }

    // ==================== 工具执行 ====================

    private List<ActionCommand> extractEvents(String userMessage) {
        String prompt = """
            从以下用户消息中提取未来的事件（考试、生日、面试等）。
            如果有，按格式输出；如果没有，只输出 NONE。
            格式：
            ---ACTIONS---
            {"actions":[{"action":"remember_event","params":{"event":"描述","date":"yyyy-MM-dd","remind_before_hours":"24"}}]}
            ---END---
            用户消息：「%s」
            """.formatted(userMessage);
        String reply = chatModel.chat(prompt);
        return toolRegistry.parseActions(reply);
    }

    private String executeServerTool(ActionCommand cmd) {
        Map<String, String> p = cmd.getParams();
        return switch (cmd.getAction()) {
            case "get_weather" -> {
                String city = p.getOrDefault("city", "Beijing");
                String ctx = p.getOrDefault("queryContext", "");
                if (ctx.contains("明天") || ctx.contains("后天") || ctx.contains("未来")
                        || ctx.contains("预报") || ctx.contains("tomorrow")) {
                    yield weatherService.getForecast(city);
                } else {
                    yield weatherService.getWeather(city);
                }
            }
            case "check_holiday" -> holidayService.checkHoliday(p.getOrDefault("date", ""));
            case "get_quote" -> quoteService.getDailyQuote();
            case "get_current_time" -> getCurrentTime();
            case "remember_event" -> rememberEvent(p);
            default -> "未知工具: " + cmd.getAction();
        };
    }

    private String getCurrentTime() {
        var now = java.time.LocalDateTime.now();
        return String.format("现在是 %s %s %s",
                now.toLocalDate(),
                now.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.CHINESE),
                now.toLocalTime().withSecond(0).withNano(0));
    }

    private String rememberEvent(Map<String, String> params) {
        try {
            String event = params.getOrDefault("event", "未知事件");
            String date = params.getOrDefault("date", "");
            String hoursStr = params.getOrDefault("remind_before_hours", "24");
            int hours = (int) Double.parseDouble(hoursStr); // 容错浮点数
            Reminder saved = reminderService.createReminder(1L, event, date, hours);
            return "✅ 已记住！事件「" + event + "」日期 " + date + "，ID=" + saved.getId();
        } catch (Exception e) {
            return "❌ 存储失败: " + e.getMessage();
        }
    }

    private String buildSystemPrompt() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        String timeStr = now.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                + "，" + now.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.CHINESE);

        return String.format("""
            你是贴心的 AI 生活助手「小伴」。
            现在时间是 %s。

            ⚠️ 调用 set_alarm 时，time 参数必须是 "yyyy-MM-dd HH:mm" 格式（如 "2026-06-18 10:00"），
            禁止只输出 HH:mm！根据当前时间推算出完整日期。

            能力：日常聊天、查天气、设闹钟、发通知、打开App、记住重要事件。
            回复风格：温暖、简洁、有用。
            %s
            """, timeStr, toolRegistry.getToolDescriptions());
    }

    /** AI 只输出 HH:mm 时，自动推断完整日期 */
    private String autoCompleteDateTime(String hhmm) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        String[] parts = hhmm.split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;

        java.time.LocalDateTime target = now.withHour(hour).withMinute(minute).withSecond(0);
        // 如果时间已过，默认推到明天
        if (!target.isAfter(now)) {
            target = target.plusDays(1);
        }
        return target.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
