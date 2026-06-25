/**
 * 对话管理 — 全部走后端 API
 */
import {
  getConversations as fetchConversations,
  createConversation as apiCreate,
  deleteConversation as apiDelete,
  updateConversationTitle as apiUpdateTitle,
  getConversationMessages as apiGetMessages,
} from "./api";

export async function loadConversations() {
  const res = await fetchConversations();
  return res.data;
}

export async function createNewConversation(title) {
  const res = await apiCreate(title || "新对话");
  return res.data;
}

export async function removeConversation(convId) {
  await apiDelete(convId);
}

export async function updateTitle(convId, title) {
  const res = await apiUpdateTitle(convId, title);
  return res.data;
}

export async function loadMessages(convId) {
  const res = await apiGetMessages(convId);
  return (res.data || []).map((m) => ({
    role: m.role,
    content: m.content,
  }));
}
