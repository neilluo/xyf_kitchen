package com.grace.platform.distribution.domain;

/**
 * 上传状态值对象
 * <p>
 * 表示视频上传到平台的状态信息，用于查询上传进度。
 * </p>
 *
 * @param taskId 上传任务ID
 * @param status 当前发布状态
 * @param progressPercent 上传进度百分比（0-100）
 * @param videoUrl 发布后的视频链接（可选，仅在上传完成后可用）
 * @param errorMessage 错误信息（可选，仅在失败状态时提供）
 */
public record UploadStatus(
    String taskId,
    PublishStatus status,
    int progressPercent,
    String videoUrl,
    String errorMessage
) {
    /**
     * 构造上传状态（不带错误信息）
     *
     * @param taskId 上传任务ID
     * @param status 发布状态
     * @param progressPercent 上传进度百分比
     */
    public UploadStatus(String taskId, PublishStatus status, int progressPercent) {
        this(taskId, status, progressPercent, null, null);
    }

    /**
     * 构造上传状态（带视频链接）
     *
     * @param taskId 上传任务ID
     * @param status 发布状态
     * @param progressPercent 上传进度百分比
     * @param videoUrl 发布后的视频链接
     */
    public UploadStatus(String taskId, PublishStatus status, int progressPercent, String videoUrl) {
        this(taskId, status, progressPercent, videoUrl, null);
    }

    /**
     * 验证构造参数
     */
    public UploadStatus {
        if (taskId == null || taskId.isBlank()) {
            throw new IllegalArgumentException("taskId must not be blank");
        }
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        if (progressPercent < 0 || progressPercent > 100) {
            throw new IllegalArgumentException("progressPercent must be between 0 and 100");
        }
    }
}
