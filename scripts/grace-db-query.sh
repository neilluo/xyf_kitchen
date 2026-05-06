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

# 执行 SQL 查询 (优先直连 MySQL，降级到后端 debug 端点)
execute_sql() {
    local query="$1"
    
    # 安全检查: 只允许 SELECT (匹配独立 SQL 关键字，避免 created_at 等误拦截)
    if echo "$query" | grep -qiE "\b(INSERT|UPDATE|DELETE|DROP|ALTER|CREATE|TRUNCATE|REPLACE|MERGE)\b"; then
        echo -e "${RED}❌ Error: Only SELECT queries are allowed${NC}"
        exit 1
    fi
    
    echo -e "${BLUE}📊 Executing query:${NC} $query"
    echo ""
    
    # 方案 1: Python pymysql 直连 (不依赖后端服务)
    python3 -c "
import pymysql, sys, json

try:
    conn = pymysql.connect(
        host='${DB_HOST}',
        port=${DB_PORT},
        user='${DB_USER}',
        password='${DB_PASS}',
        database='${DB_NAME}',
        charset='utf8mb4',
        cursorclass=pymysql.cursors.DictCursor,
        connect_timeout=5
    )
    
    with conn.cursor() as cursor:
        cursor.execute('''${query}''')
        rows = cursor.fetchall()
        
        if not rows:
            print('(empty set)')
        else:
            # 打印表头
            headers = list(rows[0].keys())
            print(' | '.join(str(h) for h in headers))
            print('-' * 80)
            # 打印数据
            for row in rows:
                print(' | '.join(str(row.get(h, '')) for h in headers))
            print(f'\n({len(rows)} row(s))')
    
    conn.close()
    
except Exception as e:
    print(f'DIRECT_DB_ERROR: {e}', file=sys.stderr)
    sys.exit(1)
" 2>&1
    
    local direct_exit=$?
    
    # 如果直连成功，直接返回
    if [ $direct_exit -eq 0 ]; then
        return 0
    fi
    
    # 方案 2: 降级到后端 debug API
    echo -e "${YELLOW}⚠️  Direct MySQL connection failed, trying backend API...${NC}"
    echo ""
    
    local endpoint="http://localhost:8000/api/debug/query"
    
    response=$(curl -s -X POST "$endpoint" \
        -H "Content-Type: application/json" \
        -d "{\"query\": \"$(echo "$query" | sed 's/"/\\"/g')}\"}" \
        --max-time 10 2>&1) || {
        echo -e "${RED}❌ Both direct DB connection and backend API failed.${NC}"
        echo -e "${YELLOW}   Check: 1) MySQL credentials  2) Backend server running${NC}"
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
