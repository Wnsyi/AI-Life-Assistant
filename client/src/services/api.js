import axios from "axios";
import { getToken, getUserId, logout } from "./auth";

const api = axios.create({
  baseURL: "/",
  timeout: 60000,
  headers: { "Content-Type": "application/json" },
});

// 请求拦截：自动带 Token
api.interceptors.request.use((config) => {
  const token = getToken();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// 响应拦截：401 跳登录
api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      logout();
      if (window.location.pathname !== "/login") window.location.href = "/login";
    }
    return Promise.reject(err);
  }
);

// ==================== 认证 ====================

export function register(username, password) {
  return api.post("/api/register", { username, password });
}
export function login(username, password) {
  return api.post("/api/login", { username, password });
}

// ==================== 对话管理 ====================

/** 获取用户对话列表 */
export function getConversations() {
  const uid = getUserId();
  return api.get("/api/conversations", { params: { userId: uid } });
}

/** 创建新对话 */
export function createConversation(title) {
  const uid = getUserId();
  return api.post("/api/conversations", { userId: uid, title });
}

/** 更新对话标题 */
export function updateConversationTitle(convId, title) {
  return api.put(`/api/conversations/${convId}`, { title });
}

/** 删除对话 */
export function deleteConversation(convId) {
  const uid = getUserId();
  return api.delete(`/api/conversations/${convId}`, { params: { userId: uid } });
}

/** 获取对话历史消息 */
export function getConversationMessages(convId) {
  return api.get(`/api/conversations/${convId}/messages`);
}

// ==================== 聊天 ====================

/**
 * Agent 对话
 * @param {string} message 用户消息
 * @param {number|null} conversationId 对话ID（null 则后端自动创建）
 */
export function sendMessage(message, conversationId) {
  return api.post("/api/agent-chat", {
    message,
    conversationId: conversationId || 0, // 0 表示让后端自动创建
    deviceType: "web",
  });
}

/** 清除对话记忆 */
export function forgetMemory(conversationId) {
  return api.post("/api/forget", { conversationId });
}

export default api;
