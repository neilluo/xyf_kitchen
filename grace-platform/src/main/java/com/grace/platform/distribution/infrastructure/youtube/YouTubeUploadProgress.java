package com.grace.platform.distribution.infrastructure.youtube;

/**
 * YouTube 上传进度
 * <p>
 * 表示 YouTube 视频上传的当前进度和状态。
 * </p>
 *
 * @param uploadUri      上传会话URI
 * @param bytesUploaded  已上传字节数
 * @param totalBytes     总字节数
 * @param status         上传状态
 * @param videoUrl       视频链接（仅在上传完成后）
 * @param errorMessage   错误信息（仅在失败时）
 */
public record YouTubeUploadProgress(
    String uploadUri,
    long bytesUploaded,
    long totalBytes,
    YouTubeUploadStatus status,
    String videoUrl,
    String errorMessage
) {

    /**
     * 计算上传进度百分比
     *
     * @return 进度百分比（0-100）
     */
    public int getProgressPercent() {
        if (totalBytes <= 0) {
            return 0;
        }
        return (int) ((bytesUploaded * 100) / totalBytes);
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
