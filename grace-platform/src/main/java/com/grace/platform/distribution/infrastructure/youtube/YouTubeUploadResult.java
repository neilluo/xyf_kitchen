package com.grace.platform.distribution.infrastructure.youtube;

/**
 * YouTube 上传结果
 * <p>
 * 表示 YouTube 视频上传操作的结果，包含上传任务ID、上传URI和视频链接。
 * </p>
 *
 * @param taskId    上传任务ID（视频ID）
 * @param uploadUri 上传会话URI（用于断点续传）
 * @param videoUrl  发布后的视频链接
 * @param status    上传状态
 */
public record YouTubeUploadResult(
    String taskId,
    String uploadUri,
    String videoUrl,
    YouTubeUploadStatus status
) {

    /**
     * 构造上传结果（进行中状态）
     *
     * @param taskId    上传任务ID
     * @param uploadUri 上传会话URI
     */
    public YouTubeUploadResult(String taskId, String uploadUri) {
        this(taskId, uploadUri, null, YouTubeUploadStatus.UPLOADING);
    }

    /**
     * 构造上传结果（已完成）
     *
     * @param taskId   视频ID
     * @param videoUrl 视频链接
     */
    public static YouTubeUploadResult completed(String taskId, String videoUrl) {
        return new YouTubeUploadResult(taskId, null, videoUrl, YouTubeUploadStatus.COMPLETED);
    }

    /**
     * 构造上传结果（失败）
     *
     * @param taskId       任务ID
     * @param errorMessage 错误信息
     */
    public static YouTubeUploadResult failed(String taskId, String errorMessage) {
        return new YouTubeUploadResult(taskId, null, null, YouTubeUploadStatus.FAILED);
    }

    /**
     * 构造上传结果（配额超限）
     *
     * @param taskId    任务ID
     * @param uploadUri 上传会话URI
     */
    public static YouTubeUploadResult quotaExceeded(String taskId, String uploadUri) {
        return new YouTubeUploadResult(taskId, uploadUri, null, YouTubeUploadStatus.QUOTA_EXCEEDED);
    }

    public YouTubeUploadResult {
        if (taskId == null || taskId.isBlank()) {
            throw new IllegalArgumentException("taskId must not be blank");
        }
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
    }

    /**
     * 检查上传是否已完成
     */
    public boolean isCompleted() {
        return status == YouTubeUploadStatus.COMPLETED;
    }

    /**
     * 检查上传是否失败
     */
    public boolean isFailed() {
        return status == YouTubeUploadStatus.FAILED;
    }

    /**
     * 检查是否配额超限
     */
    public boolean isQuotaExceeded() {
        return status == YouTubeUploadStatus.QUOTA_EXCEEDED;
    }
}
