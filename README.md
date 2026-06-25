# AI 生活助手 (AI Life Assistant)

> 一个具备**长记忆对话**、**个性化回答**、**自主规划与调用工具**能力的智能生活助手。

## ✨ 核心特性

- 🧠 **长记忆对话** — 记住用户上下文，跨会话保持记忆（如"三天前提到要考试，前一天自动提醒"）
- 🎯 **个性化回答** — 根据用户画像调整回复风格与内容
- ⏰ **系统工具调用** — 调用手机系统 API（闹钟、通知、弹窗）
- 📱 **应用控制** — 帮助用户打开手机 App（如"打开抖音"）
- 🤖 **自主规划** — ReAct 模式，多步骤任务自动拆解执行
- 🔄 **反思纠错** — 执行出错时自我纠正

## 🏗️ 技术架构

```
Web 桌面端 (React)  +  Android 移动端 (Java)
         │                    │
         └────────┬───────────┘
                  ▼
     Spring Boot 后端 (Java 21 + LangChain4j)
                  │
         ┌────────┼────────┐
         ▼        ▼        ▼
     DeepSeek   MySQL   pgvector
     (大模型)   (数据库)  (向量存储/RAG)
```

| 层级 | 技术 |
|------|------|
| **Web 前端** | React 18 + JavaScript |
| **Android 端** | Java 原生 |
| **后端框架** | Spring Boot 4.1.0 + Java 21 |
| **AI 框架** | LangChain4j 1.0.0 |
| **大模型** | DeepSeek API |
| **数据库** | MySQL 8.0 |
| **向量存储** | pgvector（RAG 长期记忆） |
| **部署** | Docker + 阿里云 |

## 📁 项目结构

```
AI-Life-Assistant/
├── client/          # Web 前端 (React + JavaScript)
├── server/          # 后端服务 (Spring Boot + Java)
├── android/         # Android 客户端 (Java)
├── docs/            # 项目文档
├── scripts/         # 构建/部署脚本
├── docs/            # 项目文档（含前后端计划）
└── README.md        # 本文件
```

## 🚀 快速开始

### 1. 前置条件

| 软件 | 用途 | 下载 |
|------|------|------|
| Node.js (LTS) | React 运行环境 | https://nodejs.org/ |
| JDK 21 | Java 运行环境 | https://adoptium.net/ |
| IntelliJ IDEA | 后端开发 IDE | https://www.jetbrains.com/idea/download/ |
| Android Studio | Android 开发 IDE | https://developer.android.com/studio |
| MySQL 8.0 | 数据库 | https://dev.mysql.com/downloads/ |

### 2. 启动后端（server）

```bash
cd server
./mvnw spring-boot:run
# 服务启动在 http://localhost:8082
```

> 后端已部署到远程服务器：`http://39.105.51.168:8082`

### 3. 启动前端（client）

```bash
cd client
npm install        # 安装依赖
npm start          # 启动开发服务器
# 自动打开 http://localhost:3000
```

### 4. 构建 Android（android）

用 Android Studio 打开 `android/` 目录，同步 Gradle 后运行。

## 📡 核心 API

### 发送消息

```bash
POST /api/chat
Content-Type: application/json

{
  "userId": "1",
  "conversationId": "1",
  "message": "帮我订明天早上10点的闹钟",
  "deviceType": "web"
}
```

响应：

```json
{
  "reply": "好的！已经帮你设置好了...",
  "actionCommands": [
    {
      "action": "set_alarm",
      "params": { "time": "2026-06-17 10:00:00" },
      "status": "pending"
    }
  ],
  "conversationId": "1"
}
```

## 📖 文档

- [前端开发计划](docs/client_plan.md) — Web 前端开发计划与进度
- [后端开发计划](docs/server_plan.md) — 后端架构与实现细节

## 🔧 开发进度

| 阶段 | 状态 | 说明 |
|------|------|------|
| 后端服务 | ✅ 已完成 | Spring Boot + Agent + Memory + Tools |
| Web 前端 | 🔴 进行中 | React 项目搭建中 |
| 对话联调 | ⬜ 待开始 | 前后端对接 |
| Android 端 | ⬜ 待开始 | 移动端开发 |

---

*最后更新：2026-06-16*
# AI-Life-Assistant
