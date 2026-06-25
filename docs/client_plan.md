# AI 生活助手 — 项目总体规划

> **项目目标：** 构建一个具备长记忆对话、个性化回答、自主规划与调用工具能力的 AI 智能体。
> **总负责人：** Claude Code（统领全局）
> **最后更新：** 2026-06-16

---

## 一、项目概述

### 1.1 核心功能

| 功能 | 说明 | 示例 |
|------|------|------|
| 🧠 长记忆对话 | 记住用户说过的事情，跨会话保持上下文 | 用户三天前提到考试 → 前一天主动提醒 |
| 🎯 个性化回答 | 根据用户画像调整回复风格和内容 | 了解用户是学生 → 用学生能理解的方式回答 |
| ⏰ 系统工具调用 | 调用手机系统 API（闹钟、通知） | "帮我定明天10点的闹钟" → 自动设置 |
| 📱 应用控制 | 帮助用户打开手机上的 App | "打开抖音" → 自动启动抖音 |
| 🤖 自主规划 | 多步骤任务自动拆解执行（ReAct 模式） | "订闹钟+打开抖音+搜资料" → 一步步完成 |
| 🔄 反思纠错 | 执行出错时自我纠正 | 工具调用失败 → 换种方式重试 |

### 1.2 UI 设计参考

- **桌面端（Web）：** 仿 [DeepSeek 网页版](https://chat.deepseek.com/) 风格
- **移动端（Android）：** 仿 DeepSeek 手机 App 风格

### 1.3 技术栈总览

```
┌─────────────────────────────────────────────────────┐
│                    用户接触层                         │
│  ┌──────────────┐              ┌──────────────────┐ │
│  │  Web 桌面端   │              │  Android 移动端   │ │
│  │ React + JS   │              │  Java 原生        │ │
│  └──────┬───────┘              └────────┬─────────┘ │
│         │                               │           │
│         └───────────┬───────────────────┘           │
│                     ▼                               │
│  ┌──────────────────────────────────────────────┐   │
│  │          Spring Boot 后端 (Java 21)          │   │
│  │  ┌─────────┐ ┌────────┐ ┌────────────────┐  │   │
│  │  │ Agent   │ │ Memory │ │ Tools          │  │   │
│  │  │ (ReAct) │ │ (RAG)  │ │ (闹钟/通知/App) │  │   │
│  │  └─────────┘ └────────┘ └────────────────┘  │   │
│  └────────────────────┬─────────────────────────┘   │
│                       │                             │
│         ┌─────────────┼─────────────┐               │
│         ▼             ▼             ▼               │
│  ┌──────────┐ ┌──────────┐ ┌──────────────┐        │
│  │ DeepSeek │ │  MySQL   │ │ pgvector     │        │
│  │ 大模型    │ │  数据库   │ │ 向量存储(RAG) │        │
│  └──────────┘ └──────────┘ └──────────────┘        │
└─────────────────────────────────────────────────────┘
```

---

## 二、项目目录结构

```
AI-Life-Assistant/                ← 📁 项目根目录
│
├── plan.md                       ← 📄 本文件：项目总规划
├── README.md                     ← 📄 项目说明
├── .gitignore                    ← 📄 Git 忽略规则
│
├── client/                       ← 📁 Web 前端（React + JavaScript）
│   ├── node_modules/             ← 依赖包（git忽略）
│   ├── public/                   ← 静态资源（HTML模板、图标等）
│   ├── src/                      ← 源代码
│   │   ├── components/           ← React 组件
│   │   ├── pages/                ← 页面
│   │   ├── services/             ← API 调用封装
│   │   ├── App.js                ← 根组件
│   │   └── index.js              ← 入口文件
│   ├── package.json              ← 项目配置与依赖
│   └── .gitignore                ← 前端自己的 gitignore
│
├── server/                       ← 📁 后端（Spring Boot + Java 21）
│   ├── pom.xml                   ← Maven 依赖管理
│   ├── Dockerfile                ← Docker 镜像构建
│   └── src/
│       ├── main/java/com/lifeassistant/
│       │   ├── ServerApplication.java   ← 启动入口
│       │   ├── config/                  ← 配置类
│       │   ├── controller/              ← REST 控制器
│       │   ├── service/                 ← 业务逻辑
│       │   ├── agent/                   ← Agent 核心
│       │   ├── model/                   ← 数据模型
│       │   ├── repository/             ← 数据访问层
│       │   └── dto/                     ← 数据传输对象
│       └── resources/
│           └── application.yml          ← 应用配置
│
├── android/                      ← 📁 Android 客户端（Java 原生）
│   └── (用 Android Studio 打开此目录)
│
├── docs/                         ← 📁 文档
│   └── server_plan.md            ← 后端详细开发计划
│
└── scripts/                      ← 📁 脚本
    └── deploy.sh                 ← 部署脚本
```

---

## 三、分阶段开发计划

```
第一阶段（当前）      第二阶段            第三阶段            第四阶段
┌──────────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│ Web 前端搭建  │ ──▶│ 对话功能联调  │ ──▶│ 记忆系统     │ ──▶│ Android 开发 │
│ React 项目    │    │ 前后端对接    │    │ RAG 检索     │    │ 系统API调用  │
│ 基础UI       │    │ 流式输出     │    │ 用户画像     │    │ 闹钟/通知    │
└──────────────┘    └──────────────┘    └──────────────┘    └──────────────┘
    第1-2周             第3-4周             第5-7周             第8-10周
```

### 第一阶段：Web 前端搭建（当前 🔴）

**目标：** 创建一个 React 项目，搭建类似 DeepSeek 风格的聊天界面

- [x] 1.1 安装 Node.js（前端运行环境）✅ 已完成 (v22.12.0)
- [x] 1.2 用 `create-react-app` 创建 React 项目到 `client/` 目录 ✅ 已完成
- [x] 1.3 安装 UI 依赖（axios、react-markdown、remark-gfm）✅
- [x] 1.4 搭建页面布局（Sidebar + ChatArea）✅
- [x] 1.5 实现消息列表组件（用户消息 + AI 回复气泡）✅
- [x] 1.6 实现输入框组件（支持 Enter 发送）✅
- [x] 1.7 添加基础样式（参考 DeepSeek 风格）✅

### 第二阶段：对话功能联调

**目标：** 前后端对接，实现完整对话流程

- [x] 2.1 封装后端 API 调用（`/api/chat`）✅
- [x] 2.2 实现消息发送与接收 ✅
- [ ] 2.3 实现流式输出（SSE / 打字机效果）
- [ ] 2.4 错误处理与重试
- [ ] 2.5 Markdown 渲染支持

### 第三阶段：记忆与个性化

**目标：** 实现长记忆和个性化

- [ ] 3.1 前端显示用户画像面板
- [ ] 3.2 历史对话列表
- [ ] 3.3 提醒消息展示

### 第四阶段：Android 客户端

**目标：** 开发 Android App，对接后端 API

- [ ] 4.1 创建 Android 项目
- [ ] 4.2 实现聊天界面
- [ ] 4.3 实现系统 API 调用（闹钟、通知、打开 App）
- [ ] 4.4 前后端联调

---

## 四、后端现状（已完成 ✅）

后端 server 已经完成了核心框架搭建和大部分功能实现：

| 模块 | 状态 | 说明 |
|------|------|------|
| Spring Boot 项目 | ✅ 已完成 | Java 21 + Spring Boot 4.1.0 |
| LangChain4j 集成 | ✅ 已完成 | DeepSeek 模型接入 |
| ChatController | ✅ 已完成 | `/api/chat` 接口 |
| Agent 核心 | ✅ 已完成 | ToolRegistry + AgentService |
| 记忆系统 | ✅ 已完成 | MemorySearchService + RAG |
| 提醒调度 | ✅ 已完成 | ReminderScheduler + WebSocket 推送 |
| 工具定义 | ✅ 已完成 | 闹钟/通知/App启动/搜索/天气/百科 |
| 数据模型 | ✅ 已完成 | User/Conversation/Message/Reminder |
| Docker 部署 | ✅ 已完成 | Dockerfile + deploy.sh |
| 云端部署 | ✅ 已部署 | 阿里云 39.105.51.168:8082 |

> 详细信息见 [docs/server_plan.md](docs/server_plan.md)

---

## 五、API 接口速查（前后端对接必看）

### 5.1 发送消息（核心接口）

```
POST /api/chat
Content-Type: application/json

请求体：
{
  "userId": "1",
  "conversationId": "1",
  "message": "帮我订明天早上10点的闹钟",
  "deviceType": "web"
}

响应体：
{
  "reply": "好的！已经帮你设置好明天10:00的闹钟。...",
  "actionCommands": [
    {
      "action": "set_alarm",
      "params": { "time": "2026-06-17 10:00:00", "label": "闹钟" },
      "status": "pending"
    }
  ],
  "conversationId": "1"
}
```

### 5.2 动作结果回调

```
POST /api/action-result
Content-Type: application/json

{
  "commandId": "xxx",
  "success": true,
  "message": "completed"
}
```

### 5.3 后端服务地址

| 环境 | 地址 |
|------|------|
| 远程服务器 | `http://39.105.51.168:8082` |
| 本地开发 | `http://localhost:8082` |

---

## 六、开发环境速查

| 软件 | 用途 | 下载 |
|------|------|------|
| **VS Code** | Web 前端开发 | https://code.visualstudio.com/ |
| **Node.js** | React 运行环境 | https://nodejs.org/ (下载 LTS 版) |
| **IntelliJ IDEA** | 后端 Java 开发 | https://www.jetbrains.com/idea/download/ |
| **Android Studio** | Android 开发 | https://developer.android.com/studio |
| **Git** | 版本管理 | https://git-scm.com/ |

---

## 七、开发规范

### 7.1 角色分工

```
Claude Code（你）
  │
  ├── 统领全局：制定计划、分配任务、跟踪进度
  ├── 负责 client/（VS Code 里开发 React 前端）
  ├── 编写 plan.md、README.md、.gitignore 等文档
  │
  └── 你（开发者）
      ├── 在 IDEA 中打开 server/ 协助后端开发
      ├── 在 Android Studio 中打开 android/ 协助移动端开发
      └── 执行 Claude Code 给出的操作步骤
```

### 7.2 协作流程

```
1. Claude Code 制定计划 → 写入 plan.md
2. Claude Code 详细指导每一步操作（像现在这样）
3. 开发者按指导操作 VS Code / IDEA / Android Studio
4. 遇到问题 → 在 Claude Code 中描述问题 → Claude Code 帮助排查
5. 完成一步 → Claude Code 更新 plan.md 勾选进度
```

### 7.3 Git 提交规范

```
feat: 新功能
fix: 修复bug
docs: 文档更新
style: UI样式调整
refactor: 代码重构
test: 测试相关
```

---

*最后更新：2026-06-16*
