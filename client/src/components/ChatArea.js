import React, { useState, useRef, useEffect } from "react";
import { sendMessage } from "../services/api";
import "./ChatArea.css";
import "./MessageInput.css";

/**
 * ChatArea — 右侧聊天主区域
 * 包含：消息列表 + 底部输入框
 */
function ChatArea({ initialMessages, onMessagesChange, convId }) {
  // 消息列表（由父组件传入初始值）
  const [messages, setMessages] = useState(initialMessages || []);
  // 输入框里的文字
  const [input, setInput] = useState("");
  // 是否正在等待 AI 回复
  const [loading, setLoading] = useState(false);

  // 每当 convId 变化，重新加载初始消息
  useEffect(() => {
    setMessages(initialMessages || []);
  }, [convId]); // eslint-disable-line react-hooks/exhaustive-deps

  // 消息变化时通知父组件（支持函数式更新）
  const updateMessages = (newMessages) => {
    setMessages((prev) => {
      const next = typeof newMessages === "function" ? newMessages(prev) : newMessages;
      if (onMessagesChange) onMessagesChange(next);
      return next;
    });
  };

  // 指向消息列表底部的引用，用来自动滚到底部
  const bottomRef = useRef(null);

  // 每当消息变化，自动滚到底部
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  /** 处理发送消息 */
  const handleSend = async () => {
    const text = input.trim();
    if (!text || loading) return; // 空消息或正在加载时不允许发送

    // 1. 把用户消息加到列表里
    const userMsg = { role: "user", content: text };
    updateMessages((prev) => [...prev, userMsg]);
    setInput("");     // 清空输入框
    setLoading(true); // 显示加载状态

    try {
      // 2. 调用后端 API
      const res = await sendMessage(text, convId);
      const data = res.data;

      // 3. 把 AI 回复加到列表里（包含动作指令）
      const aiMsg = {
        role: "assistant",
        content: data.reply,
        actions: data.actionCommands || [],  // 后端返回的动作指令
      };
      updateMessages((prev) => [...prev, aiMsg]);
    } catch (err) {
      // 4. 出错时显示错误消息
      const errorMsg = {
        role: "assistant",
        content: "❌ 连接失败，请确认后端服务是否启动。",
      };
      updateMessages((prev) => [...prev, errorMsg]);
      console.error("发送消息失败：", err);
    } finally {
      setLoading(false);
    }
  };

  /** 处理键盘事件：Enter 发送，Shift+Enter 换行 */
  const handleKeyDown = (e) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault(); // 阻止默认的换行行为
      handleSend();
    }
  };

  return (
    <div className="chat-area">
      {/* 消息列表区域 */}
      <div className="chat-messages">
        {/* 如果没有任何消息，显示欢迎页 */}
        {messages.length === 0 && (
          <div className="chat-welcome">
            <h1>🤖 AI 生活助手</h1>
            <p>有什么可以帮你的？试试说：</p>
            <div className="chat-suggestions">
              <button onClick={() => setInput("帮我定明天早上10点的闹钟")}>
                帮我定明天早上10点的闹钟
              </button>
              <button onClick={() => setInput("我今天心情不好，陪我聊聊")}>
                我今天心情不好，陪我聊聊
              </button>
              <button onClick={() => setInput("帮我规划一下这周的学习计划")}>
                帮我规划一下这周的学习计划
              </button>
            </div>
          </div>
        )}

        {/* 消息列表 */}
        {messages.map((msg, index) => (
          <div key={index} className={`message-row ${msg.role}`}>
            {/* 头像 */}
            <div className="message-avatar">
              {msg.role === "user" ? "👤" : "🤖"}
            </div>
            {/* 消息内容 */}
            <div className="message-content">
              <div className="message-bubble">{msg.content}</div>

              {/* 如果有动作指令，展示出来 */}
              {msg.actions && msg.actions.length > 0 && (
                <div className="action-cards">
                  {msg.actions.map((act, i) => (
                    <div key={i} className="action-card">
                      <span className="action-icon">
                        {act.action === "set_alarm" && "⏰"}
                        {act.action === "send_notification" && "🔔"}
                        {act.action === "open_app" && "📱"}
                        {!["set_alarm","send_notification","open_app"].includes(act.action) && "🔧"}
                      </span>
                      <span className="action-label">
                        {act.action === "set_alarm" && `闹钟: ${act.params?.time || ""}`}
                        {act.action === "send_notification" && `通知: ${act.params?.title || ""}`}
                        {act.action === "open_app" && `打开: ${act.params?.appName || ""}`}
                        {!["set_alarm","send_notification","open_app"].includes(act.action) && act.action}
                      </span>
                      <span className="action-status">⏳ 等待手机执行</span>
                    </div>
                  ))}
                  <p className="action-hint">
                    💡 以上动作需要在 Android 手机上执行，Web 端暂不支持
                  </p>
                </div>
              )}
            </div>
          </div>
        ))}

        {/* 加载动画 */}
        {loading && (
          <div className="message-row assistant">
            <div className="message-avatar">🤖</div>
            <div className="message-bubble typing">
              <span className="dot"></span>
              <span className="dot"></span>
              <span className="dot"></span>
            </div>
          </div>
        )}

        {/* 不可见的锚点，用来滚动到底部 */}
        <div ref={bottomRef} />
      </div>

      {/* 底部输入区 */}
      <div className="input-area">
        <div className="input-wrapper">
          <textarea
            className="input-box"
            placeholder="输入消息... (Enter 发送, Shift+Enter 换行)"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={handleKeyDown}
            rows={1}
            disabled={loading}
          />
          <button
            className="send-btn"
            onClick={handleSend}
            disabled={loading || !input.trim()}
          >
            发送
          </button>
        </div>
      </div>
    </div>
  );
}

export default ChatArea;
