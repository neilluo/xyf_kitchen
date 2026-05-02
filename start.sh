#!/usr/bin/env bash
# start.sh - 启动 Grace Platform 前后端
# 后端: grace-platform (Spring Boot, 端口 8000)
# 前端: grace-frontend (Vite, 端口 3000)

set -u

# ---------- 路径与常量 ----------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$SCRIPT_DIR/grace-platform"
FRONTEND_DIR="$SCRIPT_DIR/grace-frontend"
PID_DIR="$SCRIPT_DIR/.pids"
LOG_DIR="$SCRIPT_DIR/logs"

BACKEND_PORT=8000
FRONTEND_PORT=3000

BACKEND_PID_FILE="$PID_DIR/backend.pid"
FRONTEND_PID_FILE="$PID_DIR/frontend.pid"
BACKEND_LOG="$LOG_DIR/backend.log"
FRONTEND_LOG="$LOG_DIR/frontend.log"

mkdir -p "$PID_DIR" "$LOG_DIR"

# ---------- 颜色输出 ----------
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info()  { echo -e "${BLUE}[INFO]${NC}  $*"; }
log_ok()    { echo -e "${GREEN}[ OK ]${NC}  $*"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC} $*"; }
log_error() { echo -e "${RED}[FAIL]${NC} $*"; }

# ---------- 端口检查与释放 ----------
free_port() {
    local port="$1"
    local label="$2"

    # lsof -ti :PORT 列出占用该端口的 PID（仅 PID）
    local pids
    pids=$(lsof -ti tcp:"$port" 2>/dev/null || true)

    if [ -z "$pids" ]; then
        log_ok "端口 $port ($label) 空闲"
        return 0
    fi

    log_warn "端口 $port ($label) 被占用，PID: $(echo "$pids" | tr '\n' ' ')，准备 kill..."

    # 先温柔 kill
    echo "$pids" | xargs kill 2>/dev/null || true
    sleep 1

    # 仍然存活则强杀
    pids=$(lsof -ti tcp:"$port" 2>/dev/null || true)
    if [ -n "$pids" ]; then
        log_warn "进程未退出，强制 kill -9: $(echo "$pids" | tr '\n' ' ')"
        echo "$pids" | xargs kill -9 2>/dev/null || true
        sleep 1
    fi

    pids=$(lsof -ti tcp:"$port" 2>/dev/null || true)
    if [ -n "$pids" ]; then
        log_error "无法释放端口 $port，残留 PID: $pids"
        return 1
    fi

    log_ok "端口 $port ($label) 已释放"
    return 0
}

# ---------- 启动后端 ----------
start_backend() {
    log_info "启动后端 (Spring Boot)..."

    if [ ! -d "$BACKEND_DIR" ]; then
        log_error "后端目录不存在: $BACKEND_DIR"
        return 1
    fi

    if ! command -v mvn >/dev/null 2>&1; then
        log_error "未找到 mvn 命令，请先安装 Maven"
        return 1
    fi

    cd "$BACKEND_DIR"
    nohup mvn spring-boot:run > "$BACKEND_LOG" 2>&1 &
    local pid=$!
    echo "$pid" > "$BACKEND_PID_FILE"
    cd "$SCRIPT_DIR"

    log_ok "后端已启动 PID=$pid，日志: $BACKEND_LOG"
}

# ---------- 启动前端 ----------
start_frontend() {
    log_info "启动前端 (Vite)..."

    if [ ! -d "$FRONTEND_DIR" ]; then
        log_error "前端目录不存在: $FRONTEND_DIR"
        return 1
    fi

    if ! command -v npm >/dev/null 2>&1; then
        log_error "未找到 npm 命令，请先安装 Node.js"
        return 1
    fi

    # 自动安装依赖
    if [ ! -d "$FRONTEND_DIR/node_modules" ]; then
        log_warn "前端依赖未安装，执行 npm install..."
        (cd "$FRONTEND_DIR" && npm install) || {
            log_error "npm install 失败"
            return 1
        }
    fi

    cd "$FRONTEND_DIR"
    nohup npm run dev > "$FRONTEND_LOG" 2>&1 &
    local pid=$!
    echo "$pid" > "$FRONTEND_PID_FILE"
    cd "$SCRIPT_DIR"

    log_ok "前端已启动 PID=$pid，日志: $FRONTEND_LOG"
}

# ---------- 主流程 ----------
main() {
    echo "=========================================="
    echo "  Grace Platform 启动脚本"
    echo "=========================================="

    log_info "Step 1/3: 检查端口占用情况..."
    free_port "$BACKEND_PORT"  "backend"  || exit 1
    free_port "$FRONTEND_PORT" "frontend" || exit 1

    log_info "Step 2/3: 启动后端服务..."
    start_backend || exit 1

    log_info "Step 3/3: 启动前端服务..."
    start_frontend || exit 1

    echo ""
    echo "=========================================="
    log_ok "全部启动完成！"
    echo ""
    echo "  后端: http://localhost:$BACKEND_PORT   (日志: tail -f $BACKEND_LOG)"
    echo "  前端: http://localhost:$FRONTEND_PORT   (日志: tail -f $FRONTEND_LOG)"
    echo ""
    echo "  停止服务: ./stop.sh"
    echo "=========================================="
}

main "$@"
