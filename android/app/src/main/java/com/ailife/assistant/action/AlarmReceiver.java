package com.ailife.assistant.action;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * 闹钟广播接收器 — 闹钟到时间后启动全屏弹窗 Activity
 */
public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String label = intent.getStringExtra("label");
        if (label == null) label = "⏰ 闹钟时间到！";

        // 启动全屏弹窗 Activity
        Intent alertIntent = new Intent(context, AlarmAlertActivity.class);
        alertIntent.putExtra("label", label);
        alertIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(alertIntent);
    }
}
