package com.grace.platform.distribution.infrastructure.youtube;

/**
 * YouTube 上传状态枚举
 * <p>
 * 表示 YouTube 视频上传的各个阶段状态。
 * </p>
 */
public enum YouTubeUploadStatus {

    /** 上传中 */
    UPLOADING,

    /** 上传完成 */
    COMPLETED,

    /** 上传失败 */
    FAILED,

    /** 配额超限 */
    QUOTA_EXCEEDED
}
