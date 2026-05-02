#!/usr/bin/env bash
# restart.sh - 一键重启 Grace Platform 前后端
# 实现：调用 stop.sh 停止 → 等待端口释放 → 调用 start.sh 启动

set -u

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# ---------- 颜色输出 ----------
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

log_info() { echo -e "${BLUE}[INFO]${NC}  $*"; }
log_ok()   { echo -e "${GREEN}[ OK ]${NC}  $*"; }
log_err()  { echo -e "${RED}[FAIL]${NC} $*"; }

# ---------- 校验依赖脚本 ----------
if [ ! -x "$SCRIPT_DIR/stop.sh" ]; then
    log_err "找不到可执行的 stop.sh，请先确保其存在并 chmod +x"
    exit 1
fi
if [ ! -x "$SCRIPT_DIR/start.sh" ]; then
    log_err "找不到可执行的 start.sh，请先确保其存在并 chmod +x"
    exit 1
fi

echo "=========================================="
echo "  Grace Platform 重启脚本"
echo "=========================================="

log_info "Phase 1/2: 停止现有服务..."
"$SCRIPT_DIR/stop.sh" || {
    log_err "stop.sh 执行失败，终止重启"
    exit 1
}

# 等待 1 秒，确保系统层面端口完全释放
sleep 1

echo ""
log_info "Phase 2/2: 启动服务..."
"$SCRIPT_DIR/start.sh" || {
    log_err "start.sh 执行失败"
    exit 1
}

echo ""
log_ok "重启完成 🎉"
