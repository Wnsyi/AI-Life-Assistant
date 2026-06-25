package com.lifeassistant.agent;

import com.lifeassistant.dto.ActionCommand;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 工具注册中心 — 定义 AI 可以调用的工具列表
 *
 * 工具分两类：
 * 1. 服务端工具（如 get_weather）→ 服务器真正执行，结果返回给 AI
 * 2. Android 工具（如 set_alarm）→ 生成 ActionCommand 指令，由 Android 客户端执行
 */
@Component
public class ToolRegistry {

    /** 服务端工具名称集合 — 这些工具在服务器上真正执行 */
    private final Set<String> serverSideTools = Set.of(
            "get_weather",
            "check_holiday",
            "get_quote",
            "remember_event",
            "get_current_time"
    );

    /**
     * 判断是否是服务端工具（需要在服务器执行）
     */
    public boolean isServerSideTool(String actionName) {
        return serverSideTools.contains(actionName);
    }

    /**
     * 获取所有可用工具的描述（用于拼入系统提示词）
     */
    public String getToolDescriptions() {
        return """
            你可以使用以下工具来帮助用户。当你需要调用工具时，
            请在回复的最后用如下格式输出（不要用 markdown 代码块，直接输出 JSON）：

            ---ACTIONS---
            {"actions":[{"action":"工具名","params":{"参数名":"值"}}]}
            ---END---

            可用工具列表：

            【服务端工具 — 实时查询（服务器直接执行并返回结果）】
            0. get_current_time — 获取当前的日期和时间（年/月/日/时/分/星期）
               无参数
               示例：{"actions":[{"action":"get_current_time","params":{}}]}

            1. get_weather — 查询城市天气（支持今天实时+未来3天预报）
               参数：city: 城市名, queryContext: 用户原始问题（判断查今天还是未来）
               示例（今天）：{"actions":[{"action":"get_weather","params":{"city":"北京","queryContext":"今天天气"}}]}
               示例（明天）：{"actions":[{"action":"get_weather","params":{"city":"北京","queryContext":"明天天气怎么样"}}]}

            2. check_holiday — 查询中国节假日/调休
               参数：date: 日期，格式 yyyy-MM-dd（如 "2026-06-19" 或 "06-19"）
               示例：{"actions":[{"action":"check_holiday","params":{"date":"2026-06-19"}}]}

            3. get_quote — 获取一句名言/每日一句
               无参数
               示例：{"actions":[{"action":"get_quote","params":{}}]}

            4. remember_event — 记住用户的重要事件（考试、会议、生日等）
               当用户在对话中提到具体日期的事件时，主动调用此工具帮用户记住。
               参数：
               - event: 事件描述（如 "数学考试"、"妈妈生日"）
               - date: 事件日期，格式 yyyy-MM-dd（如 "2026-06-24"），不确定的话推算一个合理日期
               - remind_before_hours: 提前多少小时提醒，默认 24（提前一天）
               示例：{"actions":[{"action":"remember_event","params":{"event":"数学考试","date":"2026-06-24","remind_before_hours":"24"}}]}

            【手机端工具 — 需要 Android 执行】
            1. set_alarm — 设置系统闹钟（会写入手机系统时钟App）
               参数：
               - time: 闹钟时间，**必须用完整格式 yyyy-MM-dd HH:mm**（如 "2026-06-18 10:00"）
                       永远不要只输出 HH:mm，必须带年月日！
               - label: 闹钟标签（如 "起床"、"开会"）
               示例：{"actions":[{"action":"set_alarm","params":{"time":"2026-06-18 10:00","label":"早上好"}}]}
               取消闹钟：{"actions":[{"action":"set_alarm","params":{"time":"cancel","label":""}}]}
               ⚠️ 重要规则：
               - 用户说"明天10点"→ 根据当前时间推算出明天日期，输出完整日期
               - 用户说"30秒后"→ 算出 当前时间+30秒 的完整日期时间
               - 用户说"5分钟后"→ 算出精确日期时间
               - 永远不要输出只有 HH:mm 的时间
               - 用户说"取消闹钟""关闹钟"时，调用 set_alarm(time="cancel")

            2. send_notification — 发送通知弹窗
               参数：
               - title: 通知标题
               - content: 通知内容
               示例：{"actions":[{"action":"send_notification","params":{"title":"提醒","content":"该学习了"}}]}

            3. open_app — 打开手机应用
               参数：
               - appName: 应用名称（如 "抖音"、"微信"、"支付宝"）
               示例：{"actions":[{"action":"open_app","params":{"appName":"抖音"}}]}

            如果用户的消息不需要调用任何工具，就不要输出 ---ACTIONS--- 块，
            像正常聊天一样回复即可。
            """;
    }

    /**
     * 从 AI 回复中解析 ActionCommand 列表
     */
    public List<ActionCommand> parseActions(String aiReply) {
        List<ActionCommand> commands = new ArrayList<>();

        int start = aiReply.indexOf("---ACTIONS---");
        int end = aiReply.indexOf("---END---");

        if (start == -1 || end == -1) {
            return commands;
        }

        // 如果 ---ACTIONS--- 块里只有 NONE（没有事件）
        String rawBlock = aiReply.substring(start + "---ACTIONS---".length(), end).trim();
        if ("NONE".equalsIgnoreCase(rawBlock)) {
            return commands;
        }

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> root = mapper.readValue(rawBlock, Map.class);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> actions = (List<Map<String, Object>>) root.get("actions");

            if (actions != null) {
                for (Map<String, Object> action : actions) {
                    String actionType = (String) action.get("action");
                    @SuppressWarnings("unchecked")
                    Map<String, String> params = (Map<String, String>) action.get("params");
                    commands.add(new ActionCommand(null, actionType, params));
                }
            }
        } catch (Exception e) {
            System.err.println("[ToolRegistry] 解析 Action 失败: " + e.getMessage());
        }

        return commands;
    }

    /**
     * 从 AI 回复中移除 ---ACTIONS--- 块，只保留纯文本回复
     */
    public String removeActionsBlock(String aiReply) {
        int start = aiReply.indexOf("---ACTIONS---");
        if (start == -1) return aiReply;
        return aiReply.substring(0, start).trim();
    }
}
