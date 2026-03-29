#!/bin/bash
# 更新 .ai/progress.md 脚本
# 用法: ./scripts/update-progress.sh "任务ID" "状态" "备注"

PROGRESS_FILE=".ai/progress.md"

# 检查参数
if [ $# -lt 2 ]; then
    echo "用法: $0 <任务ID> <状态> [备注]"
    echo "示例: $0 P11-05 完成 '实现视频帧提取集成'"
    exit 1
fi

TASK_ID=$1
STATUS=$2
NOTES=${3:-""}
TIMESTAMP=$(date '+%Y-%m-%d %H:%M')

# 确保文件存在
if [ ! -f "$PROGRESS_FILE" ]; then
    echo "# 进度日志" > "$PROGRESS_FILE"
    echo "" >> "$PROGRESS_FILE"
    echo "| 时间 | 任务 ID | 状态 | 备注 |" >> "$PROGRESS_FILE"
    echo "|------|---------|------|------|" >> "$PROGRESS_FILE"
fi

# 添加新记录
NEW_LINE="| $TIMESTAMP | $TASK_ID | $STATUS | $NOTES |"

# 在表头后插入新行（第4行后）
head -4 "$PROGRESS_FILE" > "$PROGRESS_FILE.tmp"
echo "$NEW_LINE" >> "$PROGRESS_FILE.tmp"
tail -n +5 "$PROGRESS_FILE" >> "$PROGRESS_FILE.tmp"
mv "$PROGRESS_FILE.tmp" "$PROGRESS_FILE"

echo "✅ 已更新进度: $TASK_ID -> $STATUS"
