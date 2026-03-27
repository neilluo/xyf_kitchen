package com.grace.platform.distribution.domain;

/**
 * 发布状态枚举
 * <p>
 * 状态流转：
 * PENDING → UPLOADING → COMPLETED
 * PENDING → UPLOADING → FAILED
 * PENDING → UPLOADING → QUOTA_EXCEEDED → UPLOADING (配额恢复后重试)
 * PENDING → UPLOADING → QUOTA_EXCEEDED → FAILED (超过最大重试次数)
 * UPLOADING → FAILED → UPLOADING (手动重试)
 * </p>
 */
public enum PublishStatus {
    PENDING,
    UPLOADING,
    COMPLETED,
    FAILED,
    QUOTA_EXCEEDED
}
