#!/bin/bash
# ============================================
# AI 生活助手 — 部署脚本
# 用法: ./deploy.sh [docker|direct]
# 前置条件: 在 scripts/ 同级目录创建 .env 文件（参考 .env.example）
# ============================================
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

# 加载 .env 文件
if [ -f "$PROJECT_DIR/.env" ]; then
    set -a
    source "$PROJECT_DIR/.env"
    set +a
    echo "✅ 已加载 .env 配置"
else
    echo "⚠ 未找到 .env 文件，请从 .env.example 复制并填写真实值"
fi

SERVER_IP="${DEPLOY_HOST:-39.105.51.168}"
SERVER_USER="${DEPLOY_USER:-root}"
APP_DIR="${DEPLOY_PATH:-/app/life-assistant}"
JAR_NAME="server-0.0.1-SNAPSHOT.jar"

echo "=== 1. 本地编译 ==="
cd "$PROJECT_DIR/server"
./mvnw clean package -DskipTests -q
echo "✅ 编译完成"

MODE=${1:-direct}

if [ "$MODE" = "docker" ]; then
    echo "=== 2. Docker 模式 ==="
    echo "请在服务器上执行:"
    echo ""
    echo "  mkdir -p $APP_DIR"
    echo "  cd $APP_DIR"
    echo "  docker build -t life-assistant ."
    echo "  docker run -d --name life-assistant \\"
    echo "    -e DB_PASSWORD='...' \\"
    echo "    -e DEEPSEEK_API_KEY='...' \\"
    echo "    -e JWT_SECRET='...' \\"
    echo "    -p 8082:8082 life-assistant"
else
    echo "=== 2. 直接部署 ==="
    echo "上传 jar 到服务器..."
    scp "target/$JAR_NAME" "$SERVER_USER@$SERVER_IP:$APP_DIR/$JAR_NAME"

    # 上传 .env 到服务器（如果本地有且远程没有）
    if [ -f "$PROJECT_DIR/.env" ]; then
        scp "$PROJECT_DIR/.env" "$SERVER_USER@$SERVER_IP:$APP_DIR/.env"
        echo "✅ .env 已上传"
    fi

    echo "=== 3. 启动服务 ==="
    ssh "$SERVER_USER@$SERVER_IP" << 'CMD'
        cd /app/life-assistant

        # 加载环境变量
        if [ -f .env ]; then
            set -a
            source .env
            set +a
        fi

        # 停掉旧进程
        pkill -f "server-0.0.1-SNAPSHOT.jar" 2>/dev/null || true
        sleep 2
        # 启动新进程（环境变量从当前 shell 继承）
        nohup java -jar server-0.0.1-SNAPSHOT.jar > app.log 2>&1 &
        sleep 5
        # 检查是否启动成功
        if pgrep -f "server-0.0.1-SNAPSHOT.jar" > /dev/null; then
            echo "✅ 服务启动成功！http://$SERVER_IP:8082"
        else
            echo "❌ 启动失败，查看日志: tail -f $APP_DIR/app.log"
        fi
CMD
fi
