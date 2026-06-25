package com.ailife.assistant.action;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.*;

public class AlarmHelper {

    private final Context context;
    private static final String PREFS_KEY = "alarm_store";

    public AlarmHelper(Context context) {
        this.context = context.getApplicationContext();
    }

    public String setAlarm(Map<String, String> params, String aiReply) {
        try {
            String timeStr = params.get("time");
            String label = params.getOrDefault("label", "AI 提醒");
            if (timeStr == null || timeStr.isEmpty()) return "❌ 时间不能为空";

            Calendar target = parseTime(timeStr);
            if (target == null) return "❌ 无法解析时间: " + timeStr;

            long now = System.currentTimeMillis();
            long targetMs = target.getTimeInMillis();

            if (!timeStr.contains("-")) {
                int extraDays = 0;
                if (aiReply != null) {
                    if (aiReply.contains("大后天")) extraDays = 3;
                    else if (aiReply.contains("后天")) extraDays = 2;
                    else if (aiReply.contains("明天") || aiReply.contains("次日")) extraDays = 1;
                }
                if (extraDays == 0 && targetMs <= now) extraDays = 1;
                if (extraDays > 0) { target.add(Calendar.DAY_OF_MONTH, extraDays); targetMs = target.getTimeInMillis(); }
            }

            long delaySec = (targetMs - now) / 1000;
            if (delaySec < 3) {
                return "❌ 闹钟时间太近（" + delaySec + " 秒后），请至少设置 3 秒后的闹钟";
            }

            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            String timeDisplay = sdf.format(target.getTime());

            // ===== setAlarmClock — 系统级闹钟 =====
            // 闹钟时间会显示在状态栏、锁屏、系统时钟 App 的"下一个闹钟"
            int requestCode = Math.abs((label + timeStr).hashCode());
            Intent intent = new Intent(context, AlarmReceiver.class);
            intent.putExtra("label", label);
            PendingIntent operation = PendingIntent.getBroadcast(
                    context, requestCode, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent showIntent = new Intent(context, AlarmAlertActivity.class);
            showIntent.putExtra("label", label);
            PendingIntent showPi = PendingIntent.getActivity(context, requestCode, showIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            am.setAlarmClock(new AlarmManager.AlarmClockInfo(targetMs, showPi), operation);

            // 存记录
            saveAlarmRecord(label, timeDisplay, targetMs, requestCode);

            if (delaySec < 120) return "✅ 闹钟已设置，将在 " + delaySec + " 秒后响铃（息屏也会响）";
            long min = delaySec / 60;
            return "✅ 闹钟已设置，" + (min < 60 ? min + "分钟后" : (min/60) + "时" + (min%60) + "分后") + "响铃（息屏也会响）";

        } catch (Exception e) {
            return "❌ 设置闹钟异常: " + e.getMessage();
        }
    }

    public int cancelAll() { return cancelByLabel(null); }

    public int cancelByLabel(String label) {
        try {
            List<AlarmRecord> records = loadAlarmRecords();
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            int cancelled = 0;
            for (AlarmRecord r : records) {
                if (label == null || label.equals(r.label)) {
                    Intent intent = new Intent(context, AlarmReceiver.class);
                    PendingIntent pi = PendingIntent.getBroadcast(
                            context, r.requestCode, intent,
                            PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
                    if (pi != null) { am.cancel(pi); pi.cancel(); }
                    cancelled++;
                }
            }
            if (cancelled > 0) {
                if (label != null) {
                    List<AlarmRecord> remaining = new ArrayList<>();
                    for (AlarmRecord r : records) if (!label.equals(r.label)) remaining.add(r);
                    saveAllRecords(remaining);
                } else {
                    saveAllRecords(Collections.emptyList());
                }
            }
            return cancelled;
        } catch (Exception e) {
            return 0;
        }
    }

    public List<String> listAlarms() {
        List<String> r = new ArrayList<>();
        for (AlarmRecord a : loadAlarmRecords()) r.add(a.label + "（" + a.time + "）");
        return r;
    }

    // ===== 持久化 =====

    private static class AlarmRecord {
        String label, time; long triggerMs; int requestCode;
        AlarmRecord(String l, String t, long ms, int rc) { label=l; time=t; triggerMs=ms; requestCode=rc; }
    }

    private void saveAlarmRecord(String label, String time, long triggerMs, int requestCode) {
        List<AlarmRecord> records = loadAlarmRecords();
        records.add(new AlarmRecord(label, time, triggerMs, requestCode));
        records.removeIf(r -> r.triggerMs < System.currentTimeMillis());
        saveAllRecords(records);
    }

    private void saveAllRecords(List<AlarmRecord> records) {
        JSONArray arr = new JSONArray();
        for (AlarmRecord r : records) {
            try {
                JSONObject o = new JSONObject();
                o.put("label", r.label); o.put("time", r.time);
                o.put("triggerMs", r.triggerMs); o.put("requestCode", r.requestCode);
                arr.put(o);
            } catch (Exception ignored) {}
        }
        SharedPreferences p = context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE);
        p.edit().putString("alarms", arr.toString()).apply();
    }

    private List<AlarmRecord> loadAlarmRecords() {
        List<AlarmRecord> records = new ArrayList<>();
        SharedPreferences p = context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE);
        try {
            JSONArray arr = new JSONArray(p.getString("alarms", "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                records.add(new AlarmRecord(o.getString("label"), o.getString("time"),
                        o.getLong("triggerMs"), o.getInt("requestCode")));
            }
        } catch (Exception ignored) {}
        records.removeIf(r -> r.triggerMs < System.currentTimeMillis());
        return records;
    }

    // ===== 时间解析 =====

    private Calendar parseTime(String s) {
        s = s.trim();
        Calendar r = tryParse(s, "yyyy-MM-dd HH:mm:ss"); if (r != null) return r;
        r = tryParse(s, "yyyy-MM-dd HH:mm"); if (r != null) return r;
        r = tryParse(s, "yyyy/MM/dd HH:mm"); if (r != null) return r;
        r = tryParse(s, "HH:mm:ss"); if (r != null) return applyToday(r);
        r = tryParse(s, "HH:mm"); if (r != null) return applyToday(r);
        return null;
    }

    private Calendar applyToday(Calendar parsed) {
        Calendar now = Calendar.getInstance();
        now.set(Calendar.HOUR_OF_DAY, parsed.get(Calendar.HOUR_OF_DAY));
        now.set(Calendar.MINUTE, parsed.get(Calendar.MINUTE));
        now.set(Calendar.SECOND, parsed.get(Calendar.SECOND));   // 保留秒数，修复"X秒后"的 bug
        now.set(Calendar.MILLISECOND, 0);
        return now;
    }

    private Calendar tryParse(String str, String pattern) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.getDefault());
            sdf.setLenient(false);
            Calendar cal = Calendar.getInstance();
            cal.setTime(sdf.parse(str));
            return cal;
        } catch (Exception ignored) { return null; }
    }
}
