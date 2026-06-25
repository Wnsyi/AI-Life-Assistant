# Android 移动端开发计划 (AI Life Assistant)

> **目标**: 开发一个 Android 原生（Java）客户端，作为 AI 生活助手的移动端界面。
> 
> **设计参考**: DeepSeek 网页版 + 移动端（白色/浅色系、简洁 Chat 式交互）
>
> **协作后端**: Spring Boot 服务端（已部署 `39.105.51.168:8082`）

---

## 一、项目概览

### 1.1 基本信息

| 项目 | 内容 |
|------|------|
| 项目名称 | AI-Life-Assistant (Android) |
| 开发语言 | Java |
| 最低 SDK | Android 8.0 (API 26) |
| 目标 SDK | Android 14 (API 34) |
| 构建工具 | Gradle (Groovy DSL) |
| 开发工具 | Android Studio |
| 包名 | `com.ailife.assistant` |
| 项目路径 | `D:\Projects\AI-Life-Assistant\android\` |

### 1.2 核心功能

```
┌─────────────────────────────────────────────────────┐
│                   Android App                        │
│                                                      │
│  🏠 登录页 (LoginActivity) ✅ 已完成                 │
│  ├── 用户名 + 密码输入                               │
│  ├── 注册 / 登录 → 调后端 API → 存 Token            │
│  └── 已登录自动跳转主页                              │
│                                                      │
│  💬 聊天页 (ChatActivity) 📋 待开发                  │
│  ├── 对话列表 (RecyclerView)                         │
│  ├── 消息气泡 (用户 / AI)                            │
│  ├── 输入框 + 发送按钮                               │
│  └── 动作卡片 (闹钟/通知/打开App 执行状态)           │
│                                                      │
│  🔧 系统能力 (解析后端 actionCommands 触发)          │
│  ├── ⏰ AlarmManager — 设置闹钟                      │
│  ├── 📢 NotificationManager — 推送通知弹窗           │
│  └── 📱 Intent — 打开第三方 App                      │
│                                                      │
│  🌐 网络层 ✅ 已完成                                 │
│  ├── OkHttp — HTTP 请求                              │
│  ├── Gson — JSON 解析                                │
│  └── TokenManager — 本地 Token 存储                  │
└─────────────────────────────────────────────────────┘
```

### 1.3 数据流（完整闭环）

```
用户在 Android App 输入 "明天早上10点叫我起床，顺便打开抖音"
         │
         ▼
    POST /api/agent-chat  ──────────>  Spring Boot 后端
    {                                     │
      "message": "明天...",              ▼
      "conversationId": 1,           AgentService.processMessage()
      "deviceType": "android"        ├─ 拼 Prompt + 对话记忆
    }                                ├─ 调 DeepSeek API
                                     ├─ 解析 ---ACTIONS--- 块
                                     ├─ 分离 服务端/Android 工具
                                     ├─ 执行服务端工具
                                     ├─ 结果喂回 AI 生成最终回复
                                     └─ Android 工具分配 commandId
                                              │
                                              ▼
                                    返回 ChatResponse:
    Android 收到响应  <──────────    {
    {                                 "reply": "好的...",
      "reply": "好的...",             "actionCommands": [
      "actionCommands": [              {
        {                                "commandId": "cmd_xxx",
          "commandId": "cmd_xxx",        "action": "set_alarm",
          "action": "set_alarm",         "params": {"time":"10:00","label":"起床"}
          "params": {...}              },
        }                              {
      ]                                  "commandId": "cmd_yyy",
    }                                    "action": "open_app",
                                         "params": {"appName":"抖音"}
    Android 解析 actionCommands         }
         │                            ]
         ├──> AlarmManager 设闹钟     }
         ├──> Intent 打开抖音
         └──> 回调 POST /api/action-result
              {"commandId":"cmd_xxx", "success":true, "message":"闹钟已设置"}
```

---

## 二、项目结构（当前实际状态）

```
android/
├── app/
│   ├── build.gradle                   # OkHttp + Gson 已添加
│   ├── src/main/
│   │   ├── java/com/ailife/assistant/
│   │   │   ├── LoginActivity.java     ✅ 登录/注册页（已对接后端 API）
│   │   │   ├── MainActivity.java      ✅ 主页面（壳子，待改为聊天页）
│   │   │   ├── network/
│   │   │   │   ├── ApiClient.java     ✅ HTTP 客户端（所有 API 封装）
│   │   │   │   └── model/
│   │   │   │       ├── LoginRequest.java     ✅
│   │   │   │       ├── LoginResponse.java    ✅
│   │   │   │       ├── ChatRequest.java      ✅
│   │   │   │       ├── ChatResponse.java     ✅
│   │   │   │       ├── ActionCommand.java    ✅
│   │   │   │       └── ActionResult.java     ✅
│   │   │   └── util/
│   │   │       └── TokenManager.java  ✅ Token 本地存储
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   │   ├── activity_login.xml ✅ 登录页布局
│   │   │   │   └── activity_main.xml  ✅ 主页布局（待替换）
│   │   │   ├── drawable/
│   │   │   │   └── input_bg.xml       ✅ 输入框圆角背景
│   │   │   └── values/
│   │   │       ├── colors.xml         ✅ 浅色系配色
│   │   │       ├── strings.xml        ✅
│   │   │       └── themes.xml         ✅ Light 主题
│   │   └── AndroidManifest.xml        ✅ 权限 + Activity 注册
├── build.gradle                       ✅ 项目级
├── settings.gradle                    ✅
├── gradle/wrapper/
│   ├── gradle-wrapper.properties      ✅ 腾讯镜像
│   └── gradle-wrapper.jar             ✅
├── gradlew / gradlew.bat              ✅
└── local.properties                   ✅
```

---

## 三、开发进度

### ✅ 已完成

| 步骤 | 内容 |
|------|------|
| 环境搭建 | Android Studio、SDK、模拟器 |
| Gradle 配置 | 腾讯镜像、wrapper 补全 |
| 登录页 UI | 白底 + 输入框 + 登录/注册按钮 |
| 网络层 | OkHttp + Gson + ApiClient（8 个 API） |
| 登录/注册 | 对接后端 /api/login、/api/register，Token 本地存储 |
| 设计规范 | 浅色系配色（白底 #FFF、主色 #4E6EF2、文字 #1F2937） |

### 📋 待开发

**阶段一：聊天页** 🐣

| 步骤 | 功能 | 涉及技术 |
|------|------|----------|
| 1.1 | MainActivity 改为聊天页布局 | RecyclerView + LinearLayout |
| 1.2 | 消息气泡（用户/AI 两种样式） | 两种 item layout |
| 1.3 | 消息输入框 + 发送 | EditText + 调 ApiClient.sendMessage() |
| 1.4 | 对接 /api/agent-chat | 新线程发请求，回主线程刷新 UI |
| 1.5 | 对话列表管理 | 新建/切换/删除对话 |

**阶段二：系统能力** 🐥

| 步骤 | 功能 | 涉及系统 API |
|------|------|--------------|
| 2.1 | 闹钟 | AlarmManager + BroadcastReceiver |
| 2.2 | 通知弹窗 | NotificationManager + NotificationChannel |
| 2.3 | 打开 App | PackageManager 查包名 + Intent |
| 2.4 | 结果回传 | POST /api/action-result |

**阶段三：润色** 🐔

| 步骤 | 功能 |
|------|------|
| 3.1 | 流式输出 (SSE，打字机效果) |
| 3.2 | Markdown 消息渲染 |
| 3.3 | 错误处理 + 重连 |
| 3.4 | 启动页 + App 图标 |
| 3.5 | 适配 Android 各版本权限 |

---

## 四、后端 API 速查表

### Base URL: `http://39.105.51.168:8082`

| 接口 | 方法 | 用途 | Android 对应方法 |
|------|------|------|-----------------|
| `/api/register` | POST | 注册，返回 `{token, userId}` | `ApiClient.register()` |
| `/api/login` | POST | 登录，返回 `{token, userId}` | `ApiClient.login()` |
| `/api/agent-chat` | POST | 核心：发消息给 AI | `ApiClient.sendMessage()` |
| `/api/action-result` | POST | 回传手机执行结果 | `ApiClient.reportActionResult()` |
| `/api/conversations?userId=` | GET | 获取对话列表 | `ApiClient.getConversations()` |
| `/api/conversations/{id}/messages` | GET | 获取消息历史 | `ApiClient.getMessages()` |
| `/api/conversations` | POST | 创建新对话 | 复用 sendMessage(conversationId=0) |
| `/api/conversations/{id}` | DELETE | 删除对话 | 待实现 |
| `/api/forget` | POST | 清除对话记忆 | 待实现 |

---

## 五、动作指令类型

| action 值 | 说明 | params | Android 要做的事 |
|-----------|------|--------|-----------------|
| `set_alarm` | 设置闹钟 | `{"time":"08:30","label":"起床"}` | AlarmManager.setExact() |
| `send_notification` | 发通知弹窗 | `{"title":"提醒","content":"该学习了"}` | NotificationManager.notify() |
| `open_app` | 打开 App | `{"appName":"抖音"}` | 包名映射 → startActivity() |

---

## 六、设计规范

### 6.1 配色（浅色系）

| 颜色 | 用途 | 色值 |
|------|------|------|
| 主背景 | 全局背景 | `# What?? #f5f`
| 卡片/输入框 | 次级背景 | `#F7F7F7` |
| 主色调 | 按钮、发送键、用户气泡 | `#4E6EF2` |
| 主文字 | 标题、正文 | `#1F2937` |
| 次要文字 | 时间戳、提示 | `#9CA3AF` |
| AI 气泡 | AI 消息背景 | `#F3F4F6` |
| 边框 | 输入框边框 | `#E5E7EB` |

### 6.2 聊天界面布局

```
┌──────────────────────────┐
│  ← 返回   对话标题        │  ← TopBar (#FFF 白底 + 下边框)
├──────────────────────────┤
│  ┌─────────────────────┐ │
│  │ 用户气泡 (蓝 #4E6EF2)│ │  ← RecyclerView
│  └─────────────────────┘ │
│     ┌───────────────┐    │
│     │ AI 气泡       │    │    灰色底 #F3F4F6，黑色字
│     │ (灰 #F3F4F6)  │    │
│     │               │    │
│     │ ┌──────────┐  │    │  ← 动作卡片
│     │ │ ⏰ 闹钟   │  │    │
│     │ │ 10:00    │  │    │
│     │ └──────────┘  │    │
│     └───────────────┘    │
├──────────────────────────┤
│ ┌──────────────────────┐ │  ← 输入区域
│ │ 输入消息...    [发送] │ │    白底 + 灰色边框
│ └──────────────────────┘ │
└──────────────────────────┘
```

---

## 七、权限清单

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
<uses-permission android:name="android.permission.SET_ALARM" />
<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" />
```

---

## 八、依赖库

```groovy
dependencies {
    // AndroidX 基础
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.11.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'

    // 网络
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
    implementation 'com.google.code.gson:gson:2.10.1'
}
```

---

## 九、知识点速查表

| 概念 | 一句话解释 |
|------|-----------|
| **Activity** | 一个屏幕页面（LoginActivity = 登录，ChatActivity = 聊天） |
| **RecyclerView** | 高性能可滚动列表，聊天消息用它 |
| **Adapter** | RecyclerView 的数据源适配器 |
| **Intent** | 页面跳转、打开外部 App、系统广播 |
| **OkHttp** | HTTP 网络库，调后端接口 |
| **Gson** | JSON ↔ Java 对象互转 |
| **SharedPreferences** | 轻量键值对存储（Token、userId） |
| **Thread + runOnUiThread** | 网络请求放子线程，结果刷新 UI 放主线程 |
| **BroadcastReceiver** | 闹钟到时间后系统回调它 |
| **NotificationChannel** | Android 8.0+ 通知渠道，必须创建 |

---

*创建日期：2026-06-17*
*最后更新：2026-06-17*
