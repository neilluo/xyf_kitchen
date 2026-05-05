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
LOG_DIR="$SCRIPT_DIR/bootstrap-logs"

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

# ---------- 健康检查：等待端口监听 ----------
# 参数: port  pid_file  log_file  label  timeout_sec
wait_for_service() {
    local port="$1"
    local pid_file="$2"
    local log_file="$3"
    local label="$4"
    local timeout="$5"

    local pid
    pid=$(cat "$pid_file" 2>/dev/null || true)
    if [ -z "$pid" ]; then
        log_error "$label 未记录 PID，启动异常"
        return 1
    fi

    log_info "等待 $label 在端口 $port 上监听 (最多 ${timeout}s)..."

    local elapsed=0
    while [ "$elapsed" -lt "$timeout" ]; do
        # 1) 进程已死 → 立刻失败，不再等
        if ! kill -0 "$pid" 2>/dev/null; then
            log_error "$label 进程 (PID=$pid) 已退出，启动失败"
            echo "------ $label 日志末尾 50 行 ------"
            tail -n 50 "$log_file" 2>/dev/null || true
            echo "----------------------------------"
            return 1
        fi

        # 2) 端口已监听 → 启动成功
        if lsof -ti tcp:"$port" >/dev/null 2>&1; then
            log_ok "$label 已在端口 $port 监听"
            return 0
        fi

        sleep 1
        elapsed=$((elapsed + 1))
    done

    log_error "$label 启动超时 (${timeout}s 内未监听端口 $port)"
    echo "------ $label 日志末尾 50 行 ------"
    tail -n 50 "$log_file" 2>/dev/null || true
    echo "----------------------------------"
    return 1
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

    # 加载 .env 中的环境变量（MYSQL_PASSWORD、QWEN_API_KEY 等）
    if [ -f "$SCRIPT_DIR/.env" ]; then
        set -a
        # shellcheck disable=SC1091
        source "$SCRIPT_DIR/.env"
        set +a
        log_ok "已加载 .env 环境变量"
    else
        log_warn ".env 不存在，可能导致敏感配置（如 MYSQL_PASSWORD）为空"
    fi

    # 锚定 logback 输出到项目根 logs/grace-platform（否则相对路径会跟随 JVM cwd）
    export GRACE_LOG_PATH="$SCRIPT_DIR/logs/grace-platform"

    cd "$BACKEND_DIR"
    nohup mvn spring-boot:run > "$BACKEND_LOG" 2>&1 &
    pid=$!
    echo "$pid" > "$BACKEND_PID_FILE"
    cd "$SCRIPT_DIR"

    log_ok "后端已启动 PID=${pid}，日志: $BACKEND_LOG"
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
    pid=$!
    echo "$pid" > "$FRONTEND_PID_FILE"
    cd "$SCRIPT_DIR"

    log_ok "前端已启动 PID=${pid}，日志: $FRONTEND_LOG"
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
    wait_for_service "$BACKEND_PORT" "$BACKEND_PID_FILE" "$BACKEND_LOG" "后端" 90 || exit 1

    log_info "Step 3/3: 启动前端服务..."
    start_frontend || exit 1
    wait_for_service "$FRONTEND_PORT" "$FRONTEND_PID_FILE" "$FRONTEND_LOG" "前端" 60 || exit 1

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
