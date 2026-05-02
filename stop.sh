#!/usr/bin/env bash
# stop.sh - 停止 Grace Platform 前后端

set -u

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PID_DIR="$SCRIPT_DIR/.pids"

BACKEND_PORT=8000
FRONTEND_PORT=3000

BACKEND_PID_FILE="$PID_DIR/backend.pid"
FRONTEND_PID_FILE="$PID_DIR/frontend.pid"

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

# ---------- 通过 PID 文件停止 ----------
stop_by_pid_file() {
    local pid_file="$1"
    local label="$2"

    if [ ! -f "$pid_file" ]; then
        return 0
    fi

    local pid
    pid=$(cat "$pid_file" 2>/dev/null || true)

    if [ -z "$pid" ]; then
        rm -f "$pid_file"
        return 0
    fi

    # 杀掉进程及其子进程（mvn / npm 会 fork 出实际服务进程）
    if kill -0 "$pid" 2>/dev/null; then
        log_info "停止 $label 进程树 PID=$pid ..."
        # 杀整个进程组
        pkill -TERM -P "$pid" 2>/dev/null || true
        kill -TERM "$pid" 2>/dev/null || true
        sleep 2

        if kill -0 "$pid" 2>/dev/null; then
            log_warn "$label 未退出，强制 kill -9"
            pkill -KILL -P "$pid" 2>/dev/null || true
            kill -KILL "$pid" 2>/dev/null || true
        fi
        log_ok "$label 已停止"
    else
        log_info "$label PID=$pid 已不存在"
    fi

    rm -f "$pid_file"
}

# ---------- 通过端口兜底清理 ----------
free_port() {
    local port="$1"
    local label="$2"

    local pids
    pids=$(lsof -ti tcp:"$port" 2>/dev/null || true)

    if [ -z "$pids" ]; then
        log_ok "端口 $port ($label) 已空闲"
        return 0
    fi

    log_warn "端口 $port ($label) 仍被占用，PID: $(echo "$pids" | tr '\n' ' ')，强制清理..."
    echo "$pids" | xargs kill 2>/dev/null || true
    sleep 1

    pids=$(lsof -ti tcp:"$port" 2>/dev/null || true)
    if [ -n "$pids" ]; then
        echo "$pids" | xargs kill -9 2>/dev/null || true
        sleep 1
    fi

    pids=$(lsof -ti tcp:"$port" 2>/dev/null || true)
    if [ -n "$pids" ]; then
        log_error "端口 $port 仍未释放，残留 PID: $pids"
        return 1
    fi

    log_ok "端口 $port 已释放"
}

# ---------- 主流程 ----------
main() {
    echo "=========================================="
    echo "  Grace Platform 停止脚本"
    echo "=========================================="

    log_info "Step 1/2: 通过 PID 文件停止服务..."
    stop_by_pid_file "$FRONTEND_PID_FILE" "前端"
    stop_by_pid_file "$BACKEND_PID_FILE"  "后端"

    log_info "Step 2/2: 端口兜底清理..."
    free_port "$FRONTEND_PORT" "frontend"
    free_port "$BACKEND_PORT"  "backend"

    echo ""
    echo "=========================================="
    log_ok "全部服务已停止"
    echo "=========================================="
}

main "$@"
