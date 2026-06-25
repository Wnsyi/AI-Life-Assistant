# AI 生活助手 - 后端开发计划

## 一、项目概述

### 1.1 项目定位
开发一个具备**长记忆对话能力**的 AI 生活助手，能够：
- 记住用户的上下文（比如三天前说过的考试），并在恰当时间主动提醒
- 调用手机系统 API（闹钟、通知、打开 App）
- 自主规划任务、调用工具、反思纠错

### 1.2 技术栈
| 层级 | 技术 | 说明 |
|------|------|------|
| 后端框架 | Spring Boot 3.x + Java 17+ | RESTful API 服务 |
| AI 框架 | LangChain4j | Agent、Memory、Tool 管理 |
| 大模型 | DeepSeek API（OpenAI 兼容接口） | 对话、推理、函数调用 |
| 数据库 | MySQL 8.0 + pgvector（向量存储） | 用户画像 + 长期记忆（RAG） |
| 缓存 | Redis（可选） | 会话缓存、短期记忆 |
| 部署 | Docker + 阿里云/腾讯云 | 容器化部署 |
| 前端 | React + JavaScript | 桌面 Web 聊天界面 |
| 移动端 | Android 原生 Java | 手机端聊天 + 系统 API 调用 |

### 1.3 项目结构（server 部分）
```
server/
├── pom.xml                          # Maven 依赖管理
├── src/
│   ├── main/
│   │   ├── java/com/lifeassistant/
│   │   │   ├── LifeAssistantApplication.java    # 启动类
│   │   │   ├── config/               # 配置类
│   │   │   │   ├── AiConfig.java              # DeepSeek 模型配置
│   │   │   │   ├── DatabaseConfig.java        # 数据库配置
│   │   │   │   └── CorsConfig.java            # 跨域配置
│   │   │   ├── controller/           # 控制器层（接收HTTP请求）
│   │   │   │   ├── ChatController.java        # 聊天接口
│   │   │   │   └── UserController.java        # 用户管理接口
│   │   │   ├── service/              # 业务逻辑层
│   │   │   │   ├── ChatService.java           # 对话服务
│   │   │   │   ├── MemoryService.java         # 记忆管理服务
│   │   │   │   ├── AgentService.java          # Agent 规划服务
│   │   │   │   └── ReminderService.java       # 提醒调度服务
│   │   │   ├── agent/                # Agent 相关
│   │   │   │   ├── LifeAgent.java             # 核心 Agent
│   │   │   │   ├── tools/                     # 工具定义
│   │   │   │   │   ├── AlarmTool.java         # 闹钟工具
│   │   │   │   │   ├── NotificationTool.java  # 通知工具
│   │   │   │   │   ├── AppLauncherTool.java   # 打开App工具
│   │   │   │   │   ├── WebSearchTool.java     # 联网搜索工具
│   │   │   │   │   └── LearningTool.java      # 学习辅助工具
│   │   │   │   └── planner/                   # 规划器
│   │   │   │       └── ReActPlanner.java      # ReAct 模式规划器
│   │   │   ├── memory/               # 记忆模块
│   │   │   │   ├── ShortTermMemory.java       # 短期记忆（对话窗口）
│   │   │   │   ├── LongTermMemory.java        # 长期记忆（向量存储）
│   │   │   │   └── UserProfile.java           # 用户画像
│   │   │   ├── model/                # 数据模型
│   │   │   │   ├── ChatMessage.java           # 消息实体
│   │   │   │   ├── Conversation.java          # 对话会话
│   │   │   │   ├── User.java                  # 用户实体
│   │   │   │   └── Reminder.java              # 提醒实体
│   │   │   ├── repository/           # 数据访问层（JPA）
│   │   │   │   ├── MessageRepository.java
│   │   │   │   ├── ConversationRepository.java
│   │   │   │   ├── UserRepository.java
│   │   │   │   └── ReminderRepository.java
│   │   │   └── dto/                  # 数据传输对象
│   │   │       ├── ChatRequest.java
│   │   │       ├── ChatResponse.java
│   │   │       └── ActionCommand.java       # 发给Android的动作指令
│   │   └── resources/
│   │       ├── application.yml              # 应用配置
│   │       └── db/migration/               # 数据库迁移脚本（Flyway）
│   └── test/
│       └── java/com/lifeassistant/         # 单元测试
```

---

## 二、核心架构设计

### 2.1 整体数据流

```
用户（Android/Web）
    │
    ▼
┌─────────────┐    HTTP/WebSocket    ┌──────────────────┐
│  前端客户端   │ ◄──────────────────► │  Spring Boot 后端 │
│  (Web/Android)│                     │  (REST API)       │
└─────────────┘                      │                    │
                                     │  ┌──────────────┐ │
                                     │  │  LifeAgent   │ │
                                     │  │  (ReAct模式)  │ │
                                     │  │              │ │
                                     │  │ Thought→Act  │ │
                                     │  │   →Observe   │ │
                                     │  └──────┬───────┘ │
                                     │         │          │
                                     │    ┌────▼───────┐ │
                                     │    │   Tools     │ │
                                     │    │ - 闹钟       │ │
                                     │    │ - 通知       │ │
                                     │    │ - 打开App    │ │
                                     │    │ - 搜索       │ │
                                     │    │ - 学习资料   │ │
                                     │    └────┬───────┘ │
                                     │         │          │
                                     │  ┌──────▼───────┐ │
                                     │  │   Memory     │ │
                                     │  │ - 短期记忆    │ │
                                     │  │ - 长期记忆    │ │
                                     │  │ - 用户画像    │ │
                                     │  └──────────────┘ │
                                     └──────────────────┘
                                              │
                                    ┌─────────┴─────────┐
                                    ▼                   ▼
                              ┌──────────┐      ┌──────────────┐
                              │  MySQL   │      │ DeepSeek API │
                              │  + pgvector│     └──────────────┘
                              └──────────┘
```

### 2.2 Agent 工作流程（ReAct 模式）

```
用户说："帮我订明天早上10点的闹钟，然后打开抖音"

Step 1 — Thought（思考）：
  Agent 分析：用户有两个任务：1) 订闹钟 2) 打开抖音
  需要调用两个工具：setAlarm 和 openApp

Step 2 — Action（行动）：
  调用 setAlarm(time="2026-01-17 10:00", label="闹钟")
  → 后端返回 ActionCommand {action: "set_alarm", params: {...}}
  → Android 执行后回传结果 {"success": true}

Step 3 — Observation（观察）：
  闹钟设置成功，继续处理下一个任务

Step 4 — Action（行动）：
  调用 openApp(appName="抖音")
  → 后端返回 ActionCommand {action: "open_app", params: {...}}
  → Android 执行后回传结果 {"success": true}

Step 5 — Observation（观察）：
  所有任务完成，生成最终回复给用户

Step 6 — 最终回复：
  "好的！已经帮你设置好了明天上午10:00的闹钟，并且打开了抖音。"
```

### 2.3 后端与 Android 的协作模式（初版：同步请求-响应）

```
Android 发送消息 → 后端处理（可能包含ActionCommand）→ 返回给 Android
                                                    │
                                                    ▼
                                            Android 解析 ActionCommand
                                            执行系统API（闹钟/通知/打开App）
                                                    │
                                                    ▼
                                            结果回传给后端（可选）
```

---

## 三、分阶段开发计划

### 第一阶段：项目搭建 & 基础对话（预计 1-2 周）

**目标：** 搭建 Spring Boot 项目，跑通基础对话

- [ ] 1.1 安装开发环境（JDK 17、Maven、IDEA）
- [ ] 1.2 创建 Spring Boot 项目（Spring Initializr）
- [ ] 1.3 配置 pom.xml，引入必要依赖
- [ ] 1.4 配置 application.yml（DeepSeek API Key、数据库连接等）
- [ ] 1.5 创建 ChatController，实现 /api/chat 接口
- [ ] 1.6 集成 DeepSeek API（使用 LangChain4j 或直接 HTTP 调用）
- [ ] 1.7 实现最简单的对话功能：用户发消息 → 后端转给 DeepSeek → 返回回复
- [ ] 1.8 用 Postman 测试接口

**依赖清单（pom.xml 初版核心依赖）：**
```xml
<!-- Spring Boot Web -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- LangChain4j -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-open-ai</artifactId>
    <version>1.0.0-beta1</version>
</dependency>

<!-- MySQL 驱动 -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
</dependency>

<!-- Spring Data JPA -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- Lombok（简化Java代码） -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```

### 第二阶段：工具系统 & Agent 核心（预计 2-3 周）

**目标：** 让 Agent 能够自主规划并调用工具

- [ ] 2.1 定义 Tool 接口和注解（AlarmTool、NotificationTool、AppLauncherTool）
- [ ] 2.2 实现 ReAct 规划器（Thought → Action → Observation 循环）
- [ ] 2.3 实现 ActionCommand 机制：后端生成指令 → Android 执行
- [ ] 2.4 编写 Agent 核心类 LifeAgent，串联对话+工具+规划
- [ ] 2.5 测试多工具组合场景（如"订闹钟+打开App"）

### 第三阶段：记忆系统（预计 2-3 周）

**目标：** 实现短期记忆和长期记忆

- [ ] 3.1 引入数据库，创建用户表、对话表、消息表
- [ ] 3.2 实现短期记忆（MessageWindowChatMemory：保留最近 20 轮对话）
- [ ] 3.3 实现用户画像：从对话中提炼用户信息（职业、爱好、考试日期等）
- [ ] 3.4 引入向量存储（pgvector），实现长期记忆
- [ ] 3.5 实现 RAG：根据用户当前问题，检索相关的历史记忆
- [ ] 3.6 测试「三天前提到的考试，今天主动提醒」场景

### 第四阶段：提醒调度 & 主动推送（预计 1-2 周）

**目标：** 让助手能主动提醒用户

- [ ] 4.1 实现定时任务扫描（Spring @Scheduled）
- [ ] 4.2 实现提醒触发逻辑（匹配用户画像中的事件日期）
- [ ] 4.3 WebSocket 实时推送（后端主动推消息给客户端）
- [ ] 4.4 测试主动提醒场景

### 第五阶段：优化 & 部署（预计 1-2 周）

**目标：** 完善项目，部署上线

- [ ] 5.1 异常处理和错误恢复
- [ ] 5.2 添加日志（Logback/SLF4J）
- [ ] 5.3 编写单元测试和集成测试
- [ ] 5.4 Docker 容器化
- [ ] 5.5 部署到云服务器

---

## 四、数据库设计（初步）

### 4.1 ER 图核心表

```
┌──────────┐     ┌───────────────┐     ┌────────────┐
│   user   │────→│  conversation │────→│   message   │
└──────────┘     └───────────────┘     └────────────┘
     │                                        │
     │              ┌──────────────┐          │
     └─────────────→│   reminder   │          │
                    └──────────────┘          │
                                              │
                    ┌──────────────────┐      │
                    │  user_profile    │←─────┘
                    │  (用户画像/向量)  │
                    └──────────────────┘
```

### 4.2 核心表字段

**user 表：**
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 用户ID |
| username | VARCHAR(50) | 用户名 |
| created_at | DATETIME | 创建时间 |

**message 表：**
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 消息ID |
| conversation_id | BIGINT FK | 所属对话 |
| role | VARCHAR(20) | user / assistant / system |
| content | TEXT | 消息内容 |
| action_commands | JSON | 动作指令（如设闹钟） |
| created_at | DATETIME | 创建时间 |

**reminder 表：**
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 提醒ID |
| user_id | BIGINT FK | 用户ID |
| event | VARCHAR(255) | 事件描述（如"考试"） |
| remind_at | DATETIME | 提醒时间 |
| status | VARCHAR(20) | pending / sent / done |

---

## 五、API 设计（初版）

### 5.1 /api/chat — 核心聊天接口

**请求：**
```json
POST /api/chat
{
  "userId": "123",
  "conversationId": "456",
  "message": "帮我订明天早上10点的闹钟",
  "deviceType": "android"
}
```

**响应：**
```json
{
  "reply": "好的！",
  "actionCommands": [
    {
      "action": "set_alarm",
      "params": { "time": "2026-06-17 10:00:00", "label": "闹钟" },
      "status": "pending"
    }
  ],
  "conversationId": "456"
}
```

### 5.2 /api/action-result — 动作结果回调

**请求：**
```json
POST /api/action-result
{
  "commandId": "789",
  "success": true,
  "message": "闹钟已设置"
}
```

---

## 六、开发环境准备清单

### 6.1 需要安装的软件

| 软件 | 版本 | 用途 | 下载地址 |
|------|------|------|----------|
| JDK | 17 或 21 | Java 开发运行环境 | https://adoptium.net/ |
| IntelliJ IDEA | 2024.x Community（免费） | Java IDE | https://www.jetbrains.com/idea/download/ |
| Maven | 3.9+ | 项目构建管理 | IDEA 自带，无需单独安装 |
| MySQL | 8.0 | 数据库 | https://dev.mysql.com/downloads/ |
| Postman | 最新版 | API 测试工具 | https://www.postman.com/ |
| Git | 最新版 | 版本管理 | https://git-scm.com/ |

### 6.2 需要注册的服务

| 服务 | 用途 | 地址 |
|------|------|------|
| DeepSeek API | 大模型对话 | https://platform.deepseek.com/ |
| GitHub | 代码托管 | https://github.com/ |

---

## 七、关键技术点详解

### 7.1 DeepSeek 模型接入
DeepSeek 完全兼容 OpenAI 的 API 格式，可以直接用 OpenAI SDK 或 LangChain4j 的 OpenAI 模块来调用。

```
baseUrl: https://api.deepseek.com
model: deepseek-chat
```

### 7.2 LangChain4j 核心概念

| 概念 | 说明 |
|------|------|
| ChatLanguageModel | 大语言模型接口（调用 DeepSeek） |
| ChatMemory | 对话记忆（短期记忆，保留最近 N 轮） |
| ToolSpecification | 工具定义（闹钟、通知等） |
| Agent | 智能体（组合 模型+工具+记忆+规划） |
| EmbeddingStore | 向量存储（长期记忆，RAG 检索） |

### 7.3 RAG（检索增强生成）流程
```
1. 用户说："我下周三有考试"
2. 提取信息 → 向量化 → 存入 pgvector（长期记忆）
3. ... 三天后 ...
4. 定时任务触发 → 检测到明天是"考试日"
5. 检索相关记忆 → 拼入 Prompt → AI 生成提醒消息
6. 通过 WebSocket/推送 发送给用户
```

---

*最后更新：2026-06-16*
