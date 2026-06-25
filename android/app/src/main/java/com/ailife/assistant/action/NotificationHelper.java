package com.ailife.assistant.action;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import androidx.core.app.NotificationCompat;

import com.ailife.assistant.MainActivity;

import java.util.Map;

/**
 * 系统通知 — 悬浮弹窗模式（heads-up notification）
 */
public class NotificationHelper {

    private static final String CHANNEL_ID = "ai_life_notify";
    private static boolean channelCreated = false;
    private final Context context;

    public NotificationHelper(Context context) {
        this.context = context.getApplicationContext();
    }

    public String sendNotification(Map<String, String> params) {
        String title = params.getOrDefault("title", "AI 提醒");
        String content = params.getOrDefault("content", "");

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // 创建通知渠道（只做一次，高优先级 = 悬浮弹窗）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !channelCreated) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "AI 消息",
                    NotificationManager.IMPORTANCE_HIGH);  // HIGH = heads-up 悬浮
            channel.setDescription("AI 生活助手消息推送");
            channel.enableVibration(true);
            channel.setBypassDnd(true);  // 绕过免打扰
            channel.setShowBadge(true);
            manager.createNotificationChannel(channel);
            channelCreated = true;
        }

        // 声音
        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // 点击通知 → 回聊天页
        Intent clickIntent = new Intent(context, MainActivity.class);
        clickIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
                .setSound(sound)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setFullScreenIntent(pendingIntent, false);  // 高优弹窗

        int notifyId = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
        manager.notify(notifyId, builder.build());

        return "✅ 通知已发送: " + title;
    }
}
