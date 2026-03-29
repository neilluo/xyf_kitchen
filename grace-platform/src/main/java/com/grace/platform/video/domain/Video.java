package com.grace.platform.video.domain;

import com.grace.platform.shared.ErrorCode;
import com.grace.platform.shared.domain.id.VideoId;
import com.grace.platform.shared.infrastructure.exception.BusinessRuleViolationException;
import com.grace.platform.storage.domain.StorageProvider;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Video 聚合根
 * <p>
 * 负责视频实体的状态管理和业务规则验证。
 * </p>
 */
public class Video {

    // 常量定义
    public static final long MAX_FILE_SIZE = 5L * 1024 * 1024 * 1024; // 5GB in bytes
    public static final Set<VideoFormat> SUPPORTED_FORMATS = Set.of(
        VideoFormat.MP4, VideoFormat.MOV, VideoFormat.AVI, VideoFormat.MKV
    );

    // 状态机定义：当前状态 -> 允许的目标状态
    private static final Map<VideoStatus, Set<VideoStatus>> STATUS_TRANSITIONS;

    static {
        Map<VideoStatus, Set<VideoStatus>> transitions = new HashMap<>();
        // UPLOADED 可以转移到 METADATA_GENERATED
        transitions.put(VideoStatus.UPLOADED, Set.of(VideoStatus.METADATA_GENERATED));
        // METADATA_GENERATED 可以转移到 READY_TO_PUBLISH
        transitions.put(VideoStatus.METADATA_GENERATED, Set.of(VideoStatus.READY_TO_PUBLISH));
        // READY_TO_PUBLISH 可以转移到 PUBLISHING
        transitions.put(VideoStatus.READY_TO_PUBLISH, Set.of(VideoStatus.PUBLISHING));
        // PUBLISHING 可以转移到 PUBLISHED 或 PUBLISH_FAILED
        transitions.put(VideoStatus.PUBLISHING, Set.of(VideoStatus.PUBLISHED, VideoStatus.PUBLISH_FAILED));
        // PUBLISHED 可以转移到 PROMOTION_DONE
        transitions.put(VideoStatus.PUBLISHED, Set.of(VideoStatus.PROMOTION_DONE));
        // PUBLISH_FAILED 可以转移到 PUBLISHING（重试）
        transitions.put(VideoStatus.PUBLISH_FAILED, Set.of(VideoStatus.PUBLISHING));
        // PROMOTION_DONE 是终态，无转移
        transitions.put(VideoStatus.PROMOTION_DONE, Collections.emptySet());

        STATUS_TRANSITIONS = Collections.unmodifiableMap(transitions);
    }

    // 聚合根字段
    private VideoId id;
    private String fileName;
    private long fileSize;
    private VideoFormat format;
    private Duration duration;
    private String filePath;
    private String storageUrl;
    private StorageProvider storageProvider;
    private VideoStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 私有构造器，通过工厂方法创建
     */
    private Video() {
    }

    /**
     * 创建新的 Video 实例（本地存储）
     *
     * @param fileName  原始文件名
     * @param fileSize  文件字节数
     * @param format    视频格式
     * @param duration  视频时长
     * @param filePath  服务器存储路径
     * @return 新建的 Video 实例（状态为 UPLOADED）
     * @throws BusinessRuleViolationException 当验证失败时抛出
     * @deprecated 使用 {@link #createWithStorageUrl} 替代，支持 OSS 存储
     */
    @Deprecated
    public static Video create(String fileName, long fileSize, VideoFormat format,
                               Duration duration, String filePath) {
        return createInternal(fileName, fileSize, format, duration, filePath, null, StorageProvider.LOCAL);
    }

    /**
     * 创建新的 Video 实例（OSS 存储）
     *
     * @param fileName        原始文件名
     * @param fileSize        文件字节数
     * @param format          视频格式
     * @param duration        视频时长
     * @param storageUrl      OSS 存储 URL
     * @param storageProvider 存储提供者
     * @return 新建的 Video 实例（状态为 UPLOADED）
     * @throws BusinessRuleViolationException 当验证失败时抛出
     */
    public static Video createWithStorageUrl(String fileName, long fileSize, VideoFormat format,
                                              Duration duration, String storageUrl, StorageProvider storageProvider) {
        return createInternal(fileName, fileSize, format, duration, null, storageUrl, storageProvider);
    }

    /**
     * 内部创建方法，支持本地和 OSS 存储
     */
    private static Video createInternal(String fileName, long fileSize, VideoFormat format,
                                        Duration duration, String filePath, String storageUrl,
                                        StorageProvider storageProvider) {
        // 验证文件名
        if (fileName == null || fileName.isBlank()) {
            throw new BusinessRuleViolationException(
                ErrorCode.INTERNAL_SERVER_ERROR,
                "File name must not be blank"
            );
        }

        // 验证文件大小
        if (fileSize <= 0) {
            throw new BusinessRuleViolationException(
                ErrorCode.VIDEO_FILE_SIZE_EXCEEDED,
                "File size must be greater than 0"
            );
        }
        if (fileSize > MAX_FILE_SIZE) {
            throw new BusinessRuleViolationException(
                ErrorCode.VIDEO_FILE_SIZE_EXCEEDED,
                String.format("File size %d exceeds maximum allowed size %d bytes (5GB)", fileSize, MAX_FILE_SIZE)
            );
        }

        // 验证格式
        if (format == null) {
            throw new BusinessRuleViolationException(
                ErrorCode.UNSUPPORTED_VIDEO_FORMAT,
                "Video format must not be null"
            );
        }
        if (!SUPPORTED_FORMATS.contains(format)) {
            throw new BusinessRuleViolationException(
                ErrorCode.UNSUPPORTED_VIDEO_FORMAT,
                String.format("Unsupported video format: %s. Supported formats: MP4, MOV, AVI, MKV", format)
            );
        }

        // 验证存储路径/URL（本地或 OSS）
        boolean hasLocalPath = filePath != null && !filePath.isBlank();
        boolean hasStorageUrl = storageUrl != null && !storageUrl.isBlank();
        if (!hasLocalPath && !hasStorageUrl) {
            throw new BusinessRuleViolationException(
                ErrorCode.INTERNAL_SERVER_ERROR,
                "Either filePath or storageUrl must be provided"
            );
        }

        // 验证存储提供者
        if (storageProvider == null) {
            storageProvider = StorageProvider.LOCAL;
        }

        // 验证时长
        if (duration == null || duration.isNegative()) {
            throw new BusinessRuleViolationException(
                ErrorCode.INTERNAL_SERVER_ERROR,
                "Duration must not be null or negative"
            );
        }

        Video video = new Video();
        video.id = VideoId.generate();
        video.fileName = fileName;
        video.fileSize = fileSize;
        video.format = format;
        video.duration = duration;
        video.filePath = filePath;
        video.storageUrl = storageUrl;
        video.storageProvider = storageProvider;
        video.status = VideoStatus.UPLOADED;
        video.createdAt = LocalDateTime.now();
        video.updatedAt = video.createdAt;

        return video;
    }

    /**
     * 状态转换
     *
     * @param targetStatus 目标状态
     * @throws BusinessRuleViolationException 当状态转换不合法时抛出
     */
    public void transitionTo(VideoStatus targetStatus) {
        if (targetStatus == null) {
            throw new BusinessRuleViolationException(
                ErrorCode.INTERNAL_SERVER_ERROR,
                "Target status must not be null"
            );
        }

        if (this.status == targetStatus) {
            // 相同状态，无需转换
            return;
        }

        Set<VideoStatus> allowedTransitions = STATUS_TRANSITIONS.get(this.status);
        if (allowedTransitions == null || !allowedTransitions.contains(targetStatus)) {
            throw new BusinessRuleViolationException(
                ErrorCode.INTERNAL_SERVER_ERROR,
                String.format("Invalid status transition from %s to %s", this.status, targetStatus)
            );
        }

        this.status = targetStatus;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 更新视频信息
     *
     * @param fileName 新文件名（可选）
     * @param duration 新时长（可选）
     */
    public void updateInfo(String fileName, Duration duration) {
        if (fileName != null && !fileName.isBlank()) {
            this.fileName = fileName;
        }
        if (duration != null && !duration.isNegative()) {
            this.duration = duration;
        }
        this.updatedAt = LocalDateTime.now();
    }

    // Getters
    public VideoId getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public VideoFormat getFormat() {
        return format;
    }

    public Duration getDuration() {
        return duration;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getStorageUrl() {
        return storageUrl;
    }

    public StorageProvider getStorageProvider() {
        return storageProvider;
    }

    public VideoStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // Setters for persistence layer (package-private)
    void setId(VideoId id) {
        this.id = id;
    }

    void setFileName(String fileName) {
        this.fileName = fileName;
    }

    void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    void setFormat(VideoFormat format) {
        this.format = format;
    }

    void setDuration(Duration duration) {
        this.duration = duration;
    }

    void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    void setStorageUrl(String storageUrl) {
        this.storageUrl = storageUrl;
    }

    void setStorageProvider(StorageProvider storageProvider) {
        this.storageProvider = storageProvider;
    }

    void setStatus(VideoStatus status) {
        this.status = status;
    }

    void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return String.format("Video[id=%s, fileName=%s, format=%s, status=%s, size=%d, provider=%s]",
            id != null ? id.value() : "null", fileName, format, status, fileSize, storageProvider);
    }
}
