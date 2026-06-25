import React, { useState } from "react";
import { getUser, logout } from "../services/auth";
import "./Sidebar.css";

function Sidebar({
  conversations,
  activeConvId,
  isOpen,
  onToggle,
  onLogout,
  onNewChat,
  onSwitchConv,
  onDeleteConv,
  onRenameConv,
}) {
  const user = getUser();
  const [editingId, setEditingId] = useState(null);
  const [editTitle, setEditTitle] = useState("");
  const [deleteConfirmId, setDeleteConfirmId] = useState(null); // 确认删除的对话ID

  const handleLogout = () => {
    logout();
    onLogout();
  };

  /** 开始编辑标题 */
  const startEdit = (conv, e) => {
    e.stopPropagation();
    setEditingId(conv.id);
    setEditTitle(conv.title);
  };

  /** 保存标题 */
  const saveEdit = (convId) => {
    const trimmed = editTitle.trim();
    if (trimmed && onRenameConv) {
      onRenameConv(convId, trimmed);
    }
    setEditingId(null);
  };

  /** 键盘事件 */
  const handleEditKeyDown = (e, convId) => {
    if (e.key === "Enter") {
      saveEdit(convId);
    } else if (e.key === "Escape") {
      setEditingId(null);
    }
  };

  const formatTime = (iso) => {
    if (!iso) return "";
    const d = new Date(iso);
    const now = new Date();
    const diff = now - d;
    if (diff < 86400000) {
      return d.toLocaleTimeString("zh-CN", { hour: "2-digit", minute: "2-digit" });
    }
    return d.toLocaleDateString("zh-CN", { month: "short", day: "numeric" });
  };

  return (
    <div className={`sidebar ${isOpen ? "open" : "collapsed"}`}>
      <div className="sidebar-brand">
        <span className="sidebar-logo">🤖</span>
        {isOpen && <span className="sidebar-title">AI 生活助手</span>}
        <button className="sidebar-toggle" onClick={onToggle} title={isOpen ? "收起侧边栏" : "展开侧边栏"}>
          {isOpen ? "◀" : "▶"}
        </button>
      </div>

      {isOpen && (
        <>
          <button className="sidebar-new-chat" onClick={onNewChat}>
            ＋ 新对话
          </button>

          <div className="sidebar-list">
            {conversations.map((conv) => (
              <div
                key={conv.id}
                className={`sidebar-item ${conv.id === activeConvId ? "active" : ""}`}
                onClick={() => onSwitchConv(conv.id)}
              >
                {editingId === conv.id ? (
                  <input
                    className="sidebar-item-edit"
                    value={editTitle}
                    onChange={(e) => setEditTitle(e.target.value)}
                    onBlur={() => saveEdit(conv.id)}
                    onKeyDown={(e) => handleEditKeyDown(e, conv.id)}
                    autoFocus
                    onClick={(e) => e.stopPropagation()}
                  />
                ) : (
                  <>
                    <span
                      className="sidebar-item-title"
                      onDoubleClick={(e) => startEdit(conv, e)}
                      title="双击修改标题"
                    >
                      {conv.title}
                    </span>
                    <span className="sidebar-item-time">{formatTime(conv.createdAt)}</span>
                    {deleteConfirmId === conv.id ? (
                      <span className="sidebar-delete-confirm">
                        <button
                          className="sidebar-confirm-yes"
                          onClick={(e) => {
                            e.stopPropagation();
                            onDeleteConv(conv.id);
                            setDeleteConfirmId(null);
                          }}
                        >
                          确认
                        </button>
                        <button
                          className="sidebar-confirm-no"
                          onClick={(e) => {
                            e.stopPropagation();
                            setDeleteConfirmId(null);
                          }}
                        >
                          取消
                        </button>
                      </span>
                    ) : (
                      <button
                        className="sidebar-item-delete"
                        onClick={(e) => {
                          e.stopPropagation();
                          setDeleteConfirmId(conv.id);
                        }}
                        title="删除对话"
                      >
                        🗑
                      </button>
                    )}
                  </>
                )}
              </div>
            ))}
            {conversations.length === 0 && (
              <div className="sidebar-empty">暂无对话</div>
            )}
          </div>

          <div className="sidebar-footer">
            <div className="sidebar-user">👤 {user?.username || "用户"}</div>
            <button className="sidebar-logout" onClick={handleLogout}>
              退出
            </button>
          </div>
        </>
      )}
    </div>
  );
}

export default Sidebar;
