package com.ailife.assistant.action;

import android.content.Context;

/**
 * 取消闹钟 → 直接调系统 API
 */
public class AlarmCanceller {

    private final AlarmHelper helper;

    public AlarmCanceller(Context context) {
        this.helper = new AlarmHelper(context);
    }

    public String cancelAlarm(String label) {
        int n = label != null && !label.isEmpty() ? helper.cancelByLabel(label) : helper.cancelAll();
        if (n > 0) return "✅ 已取消闹钟（" + n + " 个）";
        return "⚠️ 未找到匹配的闹钟";
    }
}
