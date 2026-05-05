#!/bin/bash
# Grace Platform 数据库只读查询脚本
# 用途: 排查问题时快速查询数据库状态,无需 MySQL CLI
# 
# 使用方式:
#   ./scripts/grace-db-query.sh "SELECT * FROM video LIMIT 5;"
#   ./scripts/grace-db-query.sh "video" "vid_xxx"  # 快捷查询视频
#   ./scripts/grace-db-query.sh "status"            # 查看所有视频状态分布

set -euo pipefail

# 从 .env 或环境变量读取数据库配置
if [ -f ".env" ]; then
    export $(grep -v '^#' .env | xargs)
fi

DB_HOST="${MYSQL_HOST:-rm-2ze54x34bt9597z4ryo.mysql.rds.aliyuncs.com}"
DB_PORT="${MYSQL_PORT:-3306}"
DB_NAME="${MYSQL_DATABASE:-xyfkitchen}"
DB_USER="${MYSQL_USER:-neilluo1220}"
DB_PASS="${MYSQL_PASSWORD:-}"

# 检查 curl 是否可用
if ! command -v curl &> /dev/null; then
    echo "❌ Error: curl is required but not installed"
    exit 1
fi

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 打印帮助信息
print_help() {
    echo -e "${BLUE}Grace Platform Database Query Tool${NC}"
    echo ""
    echo "Usage:"
    echo "  $0 <sql_query>                    Execute SQL query"
    echo "  $0 video <video_id>               Query video by ID"
    echo "  $0 status                         Show video status distribution"
    echo "  $0 recent [count]                 Show recent videos (default: 10)"
    echo "  $0 trace <trace_id>               Query logs by traceId"
    echo ""
    echo "Examples:"
    echo "  $0 \"SELECT COUNT(*) FROM video;\""
    echo "  $0 video vid_abc123"
    echo "  $0 status"
    echo "  $0 recent 20"
    exit 0
}

# 执行 SQL 查询 (通过应用层的 debug 端点)
execute_sql() {
    local query="$1"
    local endpoint="http://localhost:8000/api/debug/query"
    
    # 安全检查: 只允许 SELECT
    if echo "$query" | grep -qiE "(INSERT|UPDATE|DELETE|DROP|ALTER|CREATE|TRUNCATE)"; then
        echo -e "${RED}❌ Error: Only SELECT queries are allowed${NC}"
        exit 1
    fi
    
    echo -e "${BLUE}📊 Executing query:${NC} $query"
    echo ""
    
    # 调用后端 debug 端点 (需要先在后端实现)
    response=$(curl -s -X POST "$endpoint" \
        -H "Content-Type: application/json" \
        -d "{\"query\": \"$query\"}" \
        --max-time 10 2>&1) || {
        echo -e "${YELLOW}⚠️  Backend debug endpoint not available.${NC}"
        echo -e "${YELLOW}   Please start the backend server first, or use direct MySQL connection.${NC}"
        echo ""
        echo -e "${BLUE}Alternative: Use MySQL CLI directly:${NC}"
        echo "  mysql -h $DB_HOST -P $DB_PORT -u $DB_USER -p $DB_NAME"
        exit 1
    }
    
    echo "$response" | python3 -m json.tool 2>/dev/null || echo "$response"
}

# 查询视频详情
query_video() {
    local video_id="$1"
    
    echo -e "${BLUE}🔍 Querying video:${NC} $video_id"
    echo ""
    
    local query="SELECT id, file_name, file_size, format, duration_seconds, status, storage_url, created_at, updated_at FROM video WHERE id='$video_id';"
    execute_sql "$query"
}

# 查看视频状态分布
query_status_distribution() {
    echo -e "${BLUE}📈 Video Status Distribution:${NC}"
    echo ""
    
    local query="SELECT status, COUNT(*) as count FROM video GROUP BY status ORDER BY count DESC;"
    execute_sql "$query"
}

# 查看最近视频
query_recent_videos() {
    local count="${1:-10}"
    
    echo -e "${BLUE}🕐 Recent $count Videos:${NC}"
    echo ""
    
    local query="SELECT id, file_name, status, created_at FROM video ORDER BY created_at DESC LIMIT $count;"
    execute_sql "$query"
}

# 根据 traceId 查询日志
query_trace_logs() {
    local trace_id="$1"
    local log_dir="logs/grace-platform"
    
    echo -e "${BLUE}🔍 Searching logs for traceId:${NC} $trace_id"
    echo ""
    
    if [ ! -d "$log_dir" ]; then
        echo -e "${RED}❌ Log directory not found: $log_dir${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}=== Access Log ===${NC}"
    grep "$trace_id" "$log_dir/access.log" 2>/dev/null || echo "Not found in access.log"
    
    echo ""
    echo -e "${GREEN}=== Application Log ===${NC}"
    grep "$trace_id" "$log_dir/application.log" 2>/dev/null || echo "Not found in application.log"
    
    echo ""
    echo -e "${GREEN}=== Error Log ===${NC}"
    grep "$trace_id" "$log_dir/error.log" 2>/dev/null || echo "Not found in error.log"
    
    echo ""
    echo -e "${GREEN}=== Digest Logs ===${NC}"
    for digest_file in "$log_dir"/*-digest.log; do
        if [ -f "$digest_file" ]; then
            result=$(grep "$trace_id" "$digest_file" 2>/dev/null)
            if [ -n "$result" ]; then
                echo -e "Found in $(basename $digest_file):"
                echo "$result"
                echo ""
            fi
        fi
    done
}

# 主逻辑
main() {
    if [ $# -eq 0 ]; then
        print_help
    fi
    
    case "$1" in
        video)
            [ -z "${2:-}" ] && { echo "❌ Error: video_id required"; exit 1; }
            query_video "$2"
            ;;
        status)
            query_status_distribution
            ;;
        recent)
            query_recent_videos "${2:-10}"
            ;;
        trace)
            [ -z "${2:-}" ] && { echo "❌ Error: trace_id required"; exit 1; }
            query_trace_logs "$2"
            ;;
        help|--help|-h)
            print_help
            ;;
        *)
            execute_sql "$1"
            ;;
    esac
}

main "$@"
