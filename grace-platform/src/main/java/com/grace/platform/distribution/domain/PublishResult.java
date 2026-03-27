package com.grace.platform.distribution.domain;

/**
 * 发布结果值对象
 * <p>
 * 表示视频发布操作的结果，包含上传任务ID和当前状态。
 * </p>
 *
 * @param taskId 上传任务ID（平台返回的任务标识）
 * @param status 发布状态
 * @param videoUrl 发布后的视频链接（可选，仅在上传完成后可用）
 */
public record PublishResult(
    String taskId,
    PublishStatus status,
    String videoUrl
) {
    /**
     * 构造发布结果（不带视频链接）
     *
     * @param taskId 上传任务ID
     * @param status 发布状态
     */
    public PublishResult(String taskId, PublishStatus status) {
        this(taskId, status, null);
    }

    /**
     * 验证构造参数
     */
    public PublishResult {
        if (taskId == null || taskId.isBlank()) {
            throw new IllegalArgumentException("taskId must not be blank");
        }
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
    }
}
