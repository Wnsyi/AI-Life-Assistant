import React, { useState } from "react";
import { register, login } from "../services/api";
import { saveAuth } from "../services/auth";
import "./LoginPage.css";

/**
 * LoginPage — 登录 & 注册（同一个页面，Tab 切换）
 */
function LoginPage({ onLoginSuccess }) {
  const [tab, setTab] = useState("login"); // "login" | "register"
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  /** 提交表单 */
  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");

    if (!username.trim() || !password.trim()) {
      setError("用户名和密码不能为空");
      return;
    }
    if (password.length < 6) {
      setError("密码至少 6 位");
      return;
    }

    setLoading(true);
    try {
      const res = tab === "login"
        ? await login(username, password)
        : await register(username, password);

      const data = res.data;

      if (data.code === 200) {
        saveAuth(data.token, data.userId, username);
        onLoginSuccess();
      } else {
        setError(data.message || "操作失败");
      }
    } catch (err) {
      setError("网络错误，请确认后端是否启动");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-page">
      <div className="login-card">
        {/* Logo */}
        <div className="login-logo">🤖</div>
        <h1 className="login-title">AI 生活助手</h1>
        <p className="login-subtitle">登录后享受个性化 AI 服务</p>

        {/* Tab 切换 */}
        <div className="login-tabs">
          <button
            className={`login-tab ${tab === "login" ? "active" : ""}`}
            onClick={() => { setTab("login"); setError(""); }}
          >
            登录
          </button>
          <button
            className={`login-tab ${tab === "register" ? "active" : ""}`}
            onClick={() => { setTab("register"); setError(""); }}
          >
            注册
          </button>
        </div>

        {/* 表单 */}
        <form onSubmit={handleSubmit} className="login-form">
          <input
            className="login-input"
            type="text"
            placeholder="用户名"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            autoFocus
          />
          <input
            className="login-input"
            type="password"
            placeholder="密码（至少6位）"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />

          {error && <div className="login-error">{error}</div>}

          <button className="login-btn" type="submit" disabled={loading}>
            {loading ? "处理中..." : tab === "login" ? "登 录" : "注 册"}
          </button>
        </form>

        <p className="login-hint">
          {tab === "login" ? "没有账号？" : "已有账号？"}
          <span
            className="login-switch"
            onClick={() => { setTab(tab === "login" ? "register" : "login"); setError(""); }}
          >
            {tab === "login" ? "去注册" : "去登录"}
          </span>
        </p>
      </div>
    </div>
  );
}

export default LoginPage;
