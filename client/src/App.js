import React, { useState, useEffect, useCallback, useRef } from "react";
import Sidebar from "./components/Sidebar";
import ChatArea from "./components/ChatArea";
import LoginPage from "./pages/LoginPage";
import { isLoggedIn } from "./services/auth";
import {
  loadConversations,
  createNewConversation,
  removeConversation,
  updateTitle,
  loadMessages,
} from "./services/conversations";
import "./App.css";

function App() {
  const [loggedIn, setLoggedIn] = useState(isLoggedIn());
  const [conversations, setConversations] = useState([]);
  const [activeConvId, setActiveConvId] = useState(null);
  const [messages, setMessages] = useState([]);
  const [loading, setLoading] = useState(false);
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const loadingRef = useRef(false);

  // 登录后加载对话列表
  useEffect(() => {
    if (!loggedIn) return;
    (async () => {
      try {
        const list = await loadConversations();
        setConversations(list);
        if (list.length > 0) {
          setActiveConvId(list[0].id);
          const msgs = await loadMessages(list[0].id);
          setMessages(msgs);
        }
      } catch (e) {
        console.error("加载对话列表失败", e);
      }
    })();
  }, [loggedIn]);

  /** 刷新对话列表（用于自动命名后更新标题） */
  const refreshConversations = useCallback(async () => {
    try {
      const list = await loadConversations();
      setConversations(list);
    } catch (e) { /* ignore */ }
  }, []);

  /** 开启新对话 */
  const handleNewChat = useCallback(async () => {
    if (loadingRef.current) return;
    loadingRef.current = true;
    setLoading(true);
    try {
      const conv = await createNewConversation("新对话");
      setConversations((prev) => [conv, ...prev]);
      setMessages([]);
      setActiveConvId(conv.id);
    } catch (e) {
      console.error("创建对话失败", e);
    } finally {
      setLoading(false);
      loadingRef.current = false;
    }
  }, []);

  /** 切换对话 — 关键修复：先加载消息，再切换 ID */
  const handleSwitchConv = useCallback(async (convId) => {
    if (convId === activeConvId || loadingRef.current) return;
    loadingRef.current = true;
    setLoading(true);
    try {
      const msgs = await loadMessages(convId);
      setMessages(msgs);
      setActiveConvId(convId);
    } catch (e) {
      console.error("加载消息失败", e);
      setMessages([]);
      setActiveConvId(convId);
    } finally {
      setLoading(false);
      loadingRef.current = false;
    }
  }, [activeConvId]);

  /** 删除对话 */
  const handleDeleteConv = useCallback(async (convId) => {
    try {
      await removeConversation(convId);
      const list = await loadConversations();
      setConversations(list);

      if (convId === activeConvId) {
        if (list.length > 0) {
          setActiveConvId(list[0].id);
          const msgs = await loadMessages(list[0].id);
          setMessages(msgs);
        } else {
          setActiveConvId(null);
          setMessages([]);
        }
      }
    } catch (e) {
      console.error("删除失败", e);
    }
  }, [activeConvId]);

  /** 修改对话标题 */
  const handleRenameConv = useCallback(async (convId, title) => {
    try {
      await updateTitle(convId, title);
      setConversations((prev) =>
        prev.map((c) => (c.id === convId ? { ...c, title } : c))
      );
    } catch (e) {
      console.error("改名失败", e);
    }
  }, []);

  /** 消息变化 — 如果是新对话的第一条消息，刷新列表拿自动标题 */
  const handleMessagesChange = useCallback((newMessages) => {
    setMessages(newMessages);
    // 对话标题为"新对话"且刚发了第一条用户消息 → 刷新拿后端自动标题
    const conv = conversations.find((c) => c.id === activeConvId);
    if (conv && conv.title === "新对话" && newMessages.some((m) => m.role === "user")) {
      refreshConversations();
    }
  }, [activeConvId, conversations, refreshConversations]);

  const handleLoginSuccess = () => setLoggedIn(true);

  if (!loggedIn) {
    return <LoginPage onLoginSuccess={handleLoginSuccess} />;
  }

  return (
    <div className="app-container">
      <Sidebar
        conversations={conversations}
        activeConvId={activeConvId}
        isOpen={sidebarOpen}
        onToggle={() => setSidebarOpen((v) => !v)}
        onLogout={() => setLoggedIn(false)}
        onNewChat={handleNewChat}
        onSwitchConv={handleSwitchConv}
        onDeleteConv={handleDeleteConv}
        onRenameConv={handleRenameConv}
      />
      {activeConvId ? (
        <ChatArea
          key={activeConvId}
          convId={activeConvId}
          initialMessages={messages}
          onMessagesChange={handleMessagesChange}
        />
      ) : (
        <div className="chat-area">
          <div className="chat-welcome">
            <h1>🤖 AI 生活助手</h1>
            <p>点击左侧「＋ 新对话」开始聊天</p>
          </div>
        </div>
      )}
    </div>
  );
}

export default App;
