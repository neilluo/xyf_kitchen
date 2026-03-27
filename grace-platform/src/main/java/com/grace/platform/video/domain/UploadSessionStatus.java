package com.grace.platform.video.domain;

/**
 * 上传会话状态枚举
 * <p>
 * ACTIVE: 会话活跃，可以上传分片
 * COMPLETED: 上传完成，已合并为完整视频
 * EXPIRED: 会话已过期，无法继续上传
 * </p>
 */
public enum UploadSessionStatus {
    ACTIVE,
    COMPLETED,
    EXPIRED
}
