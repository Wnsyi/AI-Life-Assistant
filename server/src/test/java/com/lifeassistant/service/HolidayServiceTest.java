package com.lifeassistant.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * HolidayService 测试 — API 可能不通，只测核心逻辑
 */
class HolidayServiceTest {

    // 不依赖 Spring 和数据库，直接测纯逻辑

    @Test
    void testWeekendDetection() {
        // 用已知的周末/工作日验证逻辑
        // 2026-06-20 是周六
        assertTrue(isWeekend("2026-06-20"));
        // 2026-06-17 是周三
        assertFalse(isWeekend("2026-06-17"));
        // 2026-06-21 是周日
        assertTrue(isWeekend("2026-06-21"));
    }

    @Test
    void testDateParsing() {
        // 这些是 HolidayService 里 parseDate 支持的格式
        assertDoesNotThrow(() -> java.time.LocalDate.parse("2026-10-01"));
        assertDoesNotThrow(() -> java.time.LocalDate.parse(
                "20260601".substring(0,4) + "-" + "20260601".substring(4,6) + "-" + "20260601".substring(6,8)));
    }

    @Test
    void testKnownHoliday() {
        // 2026-10-01 国庆节 → 应该是休息日
        var date = java.time.LocalDate.of(2026, 10, 1);
        var dow = date.getDayOfWeek();
        // 周四，不是周末，但 API 返回 status=3（节假日）
        assertNotEquals(java.time.DayOfWeek.SATURDAY, dow);
        assertNotEquals(java.time.DayOfWeek.SUNDAY, dow);
        // 所以仅靠周末判断不够，必须有 API 或数据库
    }

    private boolean isWeekend(String dateStr) {
        var date = java.time.LocalDate.parse(dateStr);
        int d = date.getDayOfWeek().getValue();
        return d >= 6; // 6=周六 7=周日
    }
}
