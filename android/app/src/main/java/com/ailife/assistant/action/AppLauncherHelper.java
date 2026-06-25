package com.ailife.assistant.action;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;

import java.util.HashMap;
import java.util.Map;

/**
 * 打开第三方 App — 根据中文名称找包名，用 Intent 启动
 */
public class AppLauncherHelper {

    private final Activity activity;

    // 常见 App 中文名 → 包名
    private static final Map<String, String> APP_MAP = new HashMap<>();
    static {
        // 中文 + 英文别名
        APP_MAP.put("抖音", "com.ss.android.ugc.aweme");
        APP_MAP.put("douyin", "com.ss.android.ugc.aweme");
        APP_MAP.put("微信", "com.tencent.mm");
        APP_MAP.put("wechat", "com.tencent.mm");
        APP_MAP.put("WeChat", "com.tencent.mm");
        APP_MAP.put("支付宝", "com.eg.android.AlipayGphone");
        APP_MAP.put("alipay", "com.eg.android.AlipayGphone");
        APP_MAP.put("微博", "com.sina.weibo");
        APP_MAP.put("weibo", "com.sina.weibo");
        APP_MAP.put("qq", "com.tencent.mobileqq");
        APP_MAP.put("QQ", "com.tencent.mobileqq");
        APP_MAP.put("淘宝", "com.taobao.taobao");
        APP_MAP.put("taobao", "com.taobao.taobao");
        APP_MAP.put("美团", "com.sankuai.meituan");
        APP_MAP.put("meituan", "com.sankuai.meituan");
        APP_MAP.put("饿了么", "me.ele");
        APP_MAP.put("饿了么", "me.ele");
        APP_MAP.put("高德地图", "com.autonavi.minimap");
        APP_MAP.put("高德", "com.autonavi.minimap");
        APP_MAP.put("百度地图", "com.baidu.BaiduMap");
        APP_MAP.put("百度", "com.baidu.BaiduMap");
        APP_MAP.put("网易云音乐", "com.netease.cloudmusic");
        APP_MAP.put("网易云", "com.netease.cloudmusic");
        APP_MAP.put("qq音乐", "com.tencent.qqmusic");
        APP_MAP.put("QQ音乐", "com.tencent.qqmusic");
        APP_MAP.put("哔哩哔哩", "tv.danmaku.bili");
        APP_MAP.put("bilibili", "tv.danmaku.bili");
        APP_MAP.put("B站", "tv.danmaku.bili");
        APP_MAP.put("知乎", "com.zhihu.android");
        APP_MAP.put("zhihu", "com.zhihu.android");
        APP_MAP.put("今日头条", "com.ss.android.article.news");
        APP_MAP.put("头条", "com.ss.android.article.news");
        APP_MAP.put("京东", "com.jingdong.app.mall");
        APP_MAP.put("jd", "com.jingdong.app.mall");
        APP_MAP.put("拼多多", "com.xunmeng.pinduoduo");
        APP_MAP.put("pdd", "com.xunmeng.pinduoduo");
        APP_MAP.put("设置", "com.android.settings");
        APP_MAP.put("settings", "com.android.settings");
        APP_MAP.put("Settings", "com.android.settings");
        APP_MAP.put("计算器", "com.android.calculator2");
        APP_MAP.put("日历", "com.android.calendar");
        APP_MAP.put("时钟", "com.android.deskclock");
        APP_MAP.put("闹钟", "com.android.deskclock");
        APP_MAP.put("相机", "com.android.camera");
        APP_MAP.put("相册", "com.android.gallery3d");
        APP_MAP.put("电话", "com.android.dialer");
        APP_MAP.put("短信", "com.android.mms");
    }

    public AppLauncherHelper(Activity activity) {
        this.activity = activity;
    }

    /**
     * @param params {"appName": "抖音"}
     */
    public String openApp(Map<String, String> params) {
        String appName = params.getOrDefault("appName", "");

        // 时钟 App — 优先用系统标准 Intent，不用猜包名
        if ("时钟".equals(appName) || "闹钟".equals(appName)) {
            // 方式1：Android 标准 AlarmClock API
            try {
                Intent alarmIntent = new Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS);
                alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (alarmIntent.resolveActivity(activity.getPackageManager()) != null) {
                    final Intent fi = alarmIntent;
                    activity.runOnUiThread(() -> {
                    try { activity.startActivity(fi); } catch (Exception ignored) {}
                });
                    return "✅ 已打开时钟";
                }
            } catch (Exception ignored) {}

            // 方式2：回退到查包名启动（覆盖主流国产品牌）
            String[] pkgs = {
                "com.android.deskclock",           // AOSP / 小米 / 大部分国产
                "com.google.android.deskclock",    // Google / 摩托罗拉
                "com.sec.android.app.clockpackage",// 三星
                "com.oppo.alarmclock",             // OPPO
                "com.coloros.alarmclock",          // ColorOS (OPPO/Realme)
                "com.hihonor.deskclock",           // 荣耀
                "com.huawei.deskclock",            // 华为
                "com.oneplus.alarmclock",          // 一加
                "com.android.BBKClock",            // Vivo
                "com.vivo.daemonService",          // Vivo 备选
                "com.asus.deskclock",              // 华硕
                "com.lge.clock",                   // LG
                "com.sonyericsson.organizer",      // 索尼
                "com.meizu.flyme.clock",           // 魅族
                "com.zui.deskclock",               // 联想 ZUI
                "com.transsion.clock",             // 传音
            };
            for (String pkg : pkgs) {
                try {
                    Intent i = activity.getPackageManager().getLaunchIntentForPackage(pkg);
                    if (i != null) {
                        final Intent fi = i;
                        activity.runOnUiThread(() -> {
                    try { activity.startActivity(fi); } catch (Exception ignored) {}
                });
                        return "✅ 已打开时钟";
                    }
                } catch (Exception ignored) {}
            }
            return "❌ 未找到时钟应用，请确认手机已安装时钟 App";
        }

        // 先精确匹配
        String packageName = APP_MAP.get(appName);
        if (packageName == null) {
            for (Map.Entry<String, String> e : APP_MAP.entrySet()) {
                if (e.getKey().equalsIgnoreCase(appName)) {
                    packageName = e.getValue();
                    break;
                }
            }
        }
        if (packageName == null) {
            String lower = appName.toLowerCase();
            for (Map.Entry<String, String> e : APP_MAP.entrySet()) {
                if (lower.contains(e.getKey().toLowerCase()) || e.getKey().toLowerCase().contains(lower)) {
                    packageName = e.getValue();
                    break;
                }
            }
        }
        if (packageName == null) packageName = appName;

        try {
            Intent launchIntent = activity.getPackageManager().getLaunchIntentForPackage(packageName);
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.runOnUiThread(() -> activity.startActivity(launchIntent));
                return "✅ 已打开 " + appName;
            }
            return "❌ 未安装 " + appName;
        } catch (Exception e) {
            return "❌ 无法打开 " + appName + ": " + e.getMessage();
        }
    }
}
