package com.ailife.assistant.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

/**
 * 权限检查 — 引导用户开启必要权限
 */
public class PermissionHelper {

    /**
     * 检查并引导开启所有必要权限
     */
    public static void checkAll(Activity activity) {
        StringBuilder missing = new StringBuilder();

        // Android 13+ 通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (activity.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                missing.append("● 通知权限（未开启）\n");
            }
        }

        // Android 12+ 精确闹钟
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            android.app.AlarmManager am = (android.app.AlarmManager)
                    activity.getSystemService(Activity.ALARM_SERVICE);
            if (am != null && !am.canScheduleExactAlarms()) {
                missing.append("● 闹钟和提醒权限（未开启）\n");
            }
        }

        // Android 10+ 悬浮窗（闹钟弹窗用）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (!Settings.canDrawOverlays(activity)) {
                missing.append("● 悬浮窗权限（未开启）\n");
            }
        }

        if (missing.length() > 0) {
            new AlertDialog.Builder(activity)
                    .setTitle("需要开启以下权限")
                    .setMessage("为保证通知、闹钟、弹窗等功能正常使用：\n\n"
                            + missing.toString()
                            + "\n点击确定进入设置页面，逐个开启即可。")
                    .setPositiveButton("去设置", (d, w) -> {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(Uri.parse("package:" + activity.getPackageName()));
                        activity.startActivity(intent);
                    })
                    .setNegativeButton("稍后", null)
                    .show();
        }
    }
}
