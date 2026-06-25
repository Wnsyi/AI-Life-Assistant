package com.ailife.assistant.action;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.ailife.assistant.MainActivity;

/**
 * 全屏闹钟 — 和系统闹钟一样的体验
 *
 * - 息屏自动亮屏唤醒
 * - 系统闹钟铃声 + 震动
 * - 滑动关闭 / 贪睡 5 分钟
 * - 60 秒自动停止（防止无限响）
 */
public class AlarmAlertActivity extends Activity {

    private static final long MAX_DURATION_MS = 60_000;  // 最多响 60 秒
    private static final long SNOOZE_MS = 5 * 60_000;     // 贪睡 5 分钟
    private static final int SNOOZE_REQUEST_CODE = 9999;

    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private String label;
    private float downY;
    private boolean dismissed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        label = getIntent().getStringExtra("label");
        if (label == null || label.isEmpty()) label = "⏰ 闹钟";

        // ===== 亮屏 + 解锁 + 保持屏幕常亮（Window flag 足以唤醒设备，不需要 WakeLock）=====
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setTurnScreenOn(true);
            setShowWhenLocked(true);
        }

        // ===== 全屏 UI =====
        setContentView(buildUI());

        // ===== 铃声 =====
        startRingtone();

        // ===== 震动 =====
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            long[] pattern = {0, 600, 400, 600, 400, 600, 1600};
            vibrator.vibrate(pattern, 0);
        }

        // ===== 60 秒自动停 =====
        handler.postDelayed(() -> {
            if (!dismissed) dismissAlarm();
        }, MAX_DURATION_MS);
    }

    // ===== UI =====

    private View buildUI() {
        android.widget.FrameLayout root = new android.widget.FrameLayout(this);
        root.setBackgroundColor(0xFF000000);

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setGravity(android.view.Gravity.CENTER);
        layout.setPadding(48, 80, 48, 80);

        // 大时间
        TextView timeView = new TextView(this);
        java.text.SimpleDateFormat tf = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
        timeView.setText(tf.format(new java.util.Date()));
        timeView.setTextSize(72);
        timeView.setTextColor(0xFFFFFFFF);
        timeView.setGravity(android.view.Gravity.CENTER);
        layout.addView(timeView);

        // 闹钟标签
        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextSize(26);
        labelView.setTextColor(0xFFB0B0B0);
        labelView.setGravity(android.view.Gravity.CENTER);
        labelView.setPadding(0, 24, 0, 80);
        layout.addView(labelView);

        // 滑动提示
        TextView hintView = new TextView(this);
        hintView.setText("↑ 向上滑动关闭  |  ↓ 向下滑动贪睡 5 分钟");
        hintView.setTextSize(16);
        hintView.setTextColor(0xFF666666);
        hintView.setGravity(android.view.Gravity.CENTER);
        hintView.setPadding(0, 0, 0, 48);
        layout.addView(hintView);

        // 按钮行
        android.widget.LinearLayout btnRow = new android.widget.LinearLayout(this);
        btnRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        btnRow.setGravity(android.view.Gravity.CENTER);

        Button snoozeBtn = new Button(this);
        snoozeBtn.setText("💤 贪睡 5 分钟");
        snoozeBtn.setTextSize(18);
        snoozeBtn.setTextColor(0xFFFFFFFF);
        snoozeBtn.setBackgroundColor(0xFF3366CC);
        snoozeBtn.setPadding(32, 20, 32, 20);
        snoozeBtn.setOnClickListener(v -> snoozeAlarm());

        Button dismissBtn = new Button(this);
        dismissBtn.setText("🔕 关闭闹钟");
        dismissBtn.setTextSize(18);
        dismissBtn.setTextColor(0xFFFFFFFF);
        dismissBtn.setBackgroundColor(0xFFCC3333);
        dismissBtn.setPadding(32, 20, 32, 20);
        dismissBtn.setOnClickListener(v -> dismissAlarm());

        btnRow.addView(snoozeBtn);
        android.widget.LinearLayout.LayoutParams spacer = new android.widget.LinearLayout.LayoutParams(48, 1);
        btnRow.addView(new View(this), spacer);
        btnRow.addView(dismissBtn);

        layout.addView(btnRow);
        root.addView(layout);

        // 滑动手势
        root.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    downY = event.getY();
                    return true;
                case MotionEvent.ACTION_UP:
                    float dy = downY - event.getY(); // 向上为负
                    if (Math.abs(dy) > 150) {
                        if (dy > 0) {
                            // 向上滑 → 关闭
                            dismissAlarm();
                        } else {
                            // 向下滑 → 贪睡
                            snoozeAlarm();
                        }
                    }
                    return true;
            }
            return true;
        });

        return root;
    }

    // ===== 铃声 =====

    private void startRingtone() {
        try {
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            }

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(this, alarmUri);
            mediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build());
            mediaPlayer.setLooping(true);

            // 渐变音量：从 30% 到 100%，10 秒内（不改系统音量，只控制 MediaPlayer）
            mediaPlayer.setVolume(0.3f, 0.3f);
            mediaPlayer.prepare();
            mediaPlayer.start();

            // 10 秒渐变到全音量
            for (int i = 1; i <= 7; i++) {
                final float vol = 0.3f + (0.7f * i / 7f);
                handler.postDelayed(() -> {
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                        mediaPlayer.setVolume(vol, vol);
                    }
                }, i * 1400L);
            }
        } catch (Exception e) {
            // 铃声失败不影响闹钟功能
        }
    }

    // ===== 操作 =====

    private void dismissAlarm() {
        dismissed = true;
        stopAll();
        finish();
    }

    private void snoozeAlarm() {
        stopAll();

        // 设置一个贪睡闹钟
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        long snoozeTime = System.currentTimeMillis() + SNOOZE_MS;

        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("label", "💤 贪睡 — " + label);
        PendingIntent operation = PendingIntent.getBroadcast(
                this, SNOOZE_REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent showIntent = new Intent(this, AlarmAlertActivity.class);
        showIntent.putExtra("label", "💤 贪睡 — " + label);
        PendingIntent showPi = PendingIntent.getActivity(
                this, SNOOZE_REQUEST_CODE, showIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        am.setExact(AlarmManager.RTC_WAKEUP, snoozeTime, operation);

        finish();
    }

    private void stopAll() {
        handler.removeCallbacksAndMessages(null);

        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception ignored) {}
            mediaPlayer = null;
        }

        if (vibrator != null) {
            vibrator.cancel();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAll();
    }

    @Override
    public void onBackPressed() {
        // 返回键 → 贪睡（不能直接关，防止误触）
        snoozeAlarm();
    }
}
