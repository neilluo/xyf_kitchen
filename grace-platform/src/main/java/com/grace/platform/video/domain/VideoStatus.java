package com.grace.platform.video.domain;

/**
 * 视频状态枚举
 * <p>
 * 状态流转：
 * UPLOADED → METADATA_GENERATED → READY_TO_PUBLISH → PUBLISHING → PUBLISHED → PROMOTION_DONE
 * PUBLISHING → PUBLISH_FAILED (可重试) → PUBLISHING
 * </p>
 */
public enum VideoStatus {
    UPLOADED,
    METADATA_GENERATED,
    READY_TO_PUBLISH,
    PUBLISHING,
    PUBLISHED,
    PUBLISH_FAILED,
    PROMOTION_DONE
}
