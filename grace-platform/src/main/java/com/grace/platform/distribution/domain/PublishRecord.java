package com.grace.platform.distribution.domain;

import com.grace.platform.shared.domain.id.MetadataId;
import com.grace.platform.shared.domain.id.PublishRecordId;
import com.grace.platform.shared.domain.id.VideoId;

import java.time.LocalDateTime;

/**
 * 发布记录聚合根
 * <p>
 * 负责跟踪视频分发到各平台的状态和进度。
 * 包含重试机制支持配额超限后的自动重试。
 * </p>
 */
public class PublishRecord {

    // 聚合根字段
    private PublishRecordId id;
    private VideoId videoId;
    private MetadataId metadataId;
    private String platform;
    private PublishStatus status;
    private String videoUrl;
    private String uploadTaskId;
    private int progressPercent;
    private String errorMessage;
    private int retryCount;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;

    /**
     * 私有构造器，通过工厂方法创建
     */
    private PublishRecord() {
    }

    /**
     * 创建新的 PublishRecord 实例
     *
     * @param videoId    视频 ID
     * @param metadataId 元数据 ID
     * @param platform   平台标识
     * @return 新建的 PublishRecord 实例（状态为 PENDING）
     */
    public static PublishRecord create(VideoId videoId, MetadataId metadataId, String platform) {
        if (videoId == null) {
            throw new IllegalArgumentException("VideoId must not be null");
        }
        if (metadataId == null) {
            throw new IllegalArgumentException("MetadataId must not be null");
        }
        if (platform == null || platform.isBlank()) {
            throw new IllegalArgumentException("Platform must not be blank");
        }

        PublishRecord record = new PublishRecord();
        record.id = PublishRecordId.generate();
        record.videoId = videoId;
        record.metadataId = metadataId;
        record.platform = platform;
        record.status = PublishStatus.PENDING;
        record.progressPercent = 0;
        record.retryCount = 0;
        record.createdAt = LocalDateTime.now();

        return record;
    }

    /**
     * 更新状态为 UPLOADING
     *
     * @param uploadTaskId 平台上传任务 ID
     */
    public void markAsUploading(String uploadTaskId) {
        this.status = PublishStatus.UPLOADING;
        this.uploadTaskId = uploadTaskId;
    }

    /**
     * 更新上传进度
     *
     * @param progressPercent 进度百分比 (0-100)
     */
    public void updateProgress(int progressPercent) {
        if (progressPercent < 0) {
            this.progressPercent = 0;
        } else if (progressPercent > 100) {
            this.progressPercent = 100;
        } else {
            this.progressPercent = progressPercent;
        }
    }

    /**
     * 标记为发布完成
     *
     * @param videoUrl 发布后视频链接
     */
    public void markAsCompleted(String videoUrl) {
        this.status = PublishStatus.COMPLETED;
        this.videoUrl = videoUrl;
        this.progressPercent = 100;
        this.publishedAt = LocalDateTime.now();
    }

    /**
     * 标记为发布失败
     *
     * @param errorMessage 错误信息
     */
    public void markAsFailed(String errorMessage) {
        this.status = PublishStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    /**
     * 标记为配额超限
     */
    public void markAsQuotaExceeded() {
        this.status = PublishStatus.QUOTA_EXCEEDED;
    }

    /**
     * 增加重试次数
     */
    public void incrementRetryCount() {
        this.retryCount++;
    }

    /**
     * 从配额超限状态恢复为上传中
     */
    public void resumeFromQuotaExceeded() {
        if (this.status != PublishStatus.QUOTA_EXCEEDED) {
            throw new IllegalStateException("Can only resume from QUOTA_EXCEEDED status");
        }
        this.status = PublishStatus.UPLOADING;
    }

    // Getters
    public PublishRecordId getId() {
        return id;
    }

    public VideoId getVideoId() {
        return videoId;
    }

    public MetadataId getMetadataId() {
        return metadataId;
    }

    public String getPlatform() {
        return platform;
    }

    public PublishStatus getStatus() {
        return status;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public String getUploadTaskId() {
        return uploadTaskId;
    }

    public int getProgressPercent() {
        return progressPercent;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // Setters for persistence layer (package-private)
    void setId(PublishRecordId id) {
        this.id = id;
    }

    void setVideoId(VideoId videoId) {
        this.videoId = videoId;
    }

    void setMetadataId(MetadataId metadataId) {
        this.metadataId = metadataId;
    }

    void setPlatform(String platform) {
        this.platform = platform;
    }

    void setStatus(PublishStatus status) {
        this.status = status;
    }

    void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    void setUploadTaskId(String uploadTaskId) {
        this.uploadTaskId = uploadTaskId;
    }

    void setProgressPercent(int progressPercent) {
        this.progressPercent = progressPercent;
    }

    void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return String.format("PublishRecord[id=%s, videoId=%s, platform=%s, status=%s, progress=%d%%]",
            id != null ? id.value() : "null",
            videoId != null ? videoId.value() : "null",
            platform,
            status,
            progressPercent);
    }
}
