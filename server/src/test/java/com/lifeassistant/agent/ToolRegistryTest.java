package com.lifeassistant.agent;

import com.lifeassistant.dto.ActionCommand;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * ToolRegistry 单元测试 — 测 Action 解析逻辑
 */
class ToolRegistryTest {

    private final ToolRegistry registry = new ToolRegistry();

    @Test
    void testParseSingleAction() {
        String aiReply = """
            好的，帮你定闹钟
            ---ACTIONS---
            {"actions":[{"action":"set_alarm","params":{"time":"08:00","label":"起床"}}]}
            ---END---""";

        List<ActionCommand> actions = registry.parseActions(aiReply);
        assertEquals(1, actions.size());
        assertEquals("set_alarm", actions.get(0).getAction());
        assertEquals("08:00", actions.get(0).getParams().get("time"));
    }

    @Test
    void testParseMultipleActions() {
        String aiReply = """
            ---ACTIONS---
            {"actions":[
                {"action":"open_app","params":{"appName":"抖音"}},
                {"action":"set_alarm","params":{"time":"10:00","label":"morning"}}
            ]}
            ---END---""";

        List<ActionCommand> actions = registry.parseActions(aiReply);
        assertEquals(2, actions.size());
        assertEquals("open_app", actions.get(0).getAction());
        assertEquals("set_alarm", actions.get(1).getAction());
    }

    @Test
    void testNoActions() {
        String aiReply = "你好，今天天气不错！";
        List<ActionCommand> actions = registry.parseActions(aiReply);
        assertTrue(actions.isEmpty());
    }

    @Test
    void testNoneBlock() {
        String aiReply = """
            ---ACTIONS---
            NONE
            ---END---""";
        List<ActionCommand> actions = registry.parseActions(aiReply);
        assertTrue(actions.isEmpty());
    }

    @Test
    void testRemoveActionsBlock() {
        String aiReply = "已帮你设置闹钟\n---ACTIONS---\n...\n---END---";
        String clean = registry.removeActionsBlock(aiReply);
        assertEquals("已帮你设置闹钟", clean);
    }
}
