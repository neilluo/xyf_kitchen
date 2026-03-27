package com.grace.platform.video.domain;

import com.grace.platform.shared.ErrorCode;
import com.grace.platform.shared.infrastructure.exception.BusinessRuleViolationException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * 上传会话实体
 * <p>
 * 管理视频分片上传的会话状态，包括分片计数、过期检查等。
 * </p>
 */
public class UploadSession {

    // 默认分片大小：5MB
    public static final long DEFAULT_CHUNK_SIZE = 5L * 1024 * 1024; // 5MB in bytes
    // 默认会话过期时间：24小时
    public static final int DEFAULT_SESSION_TTL_HOURS = 24;

    // 实体字段
    private String uploadId;
    private String fileName;
    private long fileSize;
    private VideoFormat format;
    private int totalChunks;
    private int uploadedChunks;
    private String tempDirectory;
    private UploadSessionStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    /**
     * 私有构造器，通过工厂方法创建
     */
    private UploadSession() {
    }

    /**
     * 创建新的上传会话
     *
     * @param fileName      文件名
     * @param fileSize      文件字节数
     * @param format        视频格式
     * @param tempDirectory 临时存储目录路径
     * @param chunkSize     分片大小（字节），如果为null则使用默认值
     * @return 新建的 UploadSession 实例
     * @throws BusinessRuleViolationException 当验证失败时抛出
     */
    public static UploadSession create(String fileName, long fileSize, VideoFormat format,
                                       String tempDirectory, Long chunkSize) {
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

        // 验证格式
        if (format == null) {
            throw new BusinessRuleViolationException(
                ErrorCode.UNSUPPORTED_VIDEO_FORMAT,
                "Video format must not be null"
            );
        }

        // 验证临时目录
        if (tempDirectory == null || tempDirectory.isBlank()) {
            throw new BusinessRuleViolationException(
                ErrorCode.INTERNAL_SERVER_ERROR,
                "Temp directory must not be blank"
            );
        }

        // 使用默认分片大小
        long actualChunkSize = chunkSize != null && chunkSize > 0 ? chunkSize : DEFAULT_CHUNK_SIZE;

        // 计算总分片数：ceil(fileSize / chunkSize)
        int totalChunks = calculateTotalChunks(fileSize, actualChunkSize);

        UploadSession session = new UploadSession();
        session.uploadId = generateUploadId();
        session.fileName = fileName;
        session.fileSize = fileSize;
        session.format = format;
        session.totalChunks = totalChunks;
        session.uploadedChunks = 0;
        session.tempDirectory = tempDirectory;
        session.status = UploadSessionStatus.ACTIVE;
        session.createdAt = LocalDateTime.now();
        session.expiresAt = session.createdAt.plus(DEFAULT_SESSION_TTL_HOURS, ChronoUnit.HOURS);

        return session;
    }

    /**
     * 计算总分片数
     *
     * @param fileSize  文件总大小
     * @param chunkSize 每个分片的大小
     * @return 总分片数（向上取整）
     */
    public static int calculateTotalChunks(long fileSize, long chunkSize) {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("Chunk size must be greater than 0");
        }
        // 使用整数除法向上取整：ceil(a/b) = (a + b - 1) / b
        return (int) ((fileSize + chunkSize - 1) / chunkSize);
    }

    /**
     * 生成上传会话 ID（upl_ 前缀 + UUID）
     *
     * @return 上传会话 ID
     */
    private static String generateUploadId() {
        return "upl_" + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 检查会话是否已过期
     *
     * @return true 如果会话已过期
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * 检查分片索引是否有效
     *
     * @param chunkIndex 分片索引
     * @return true 如果索引在有效范围内
     */
    public boolean isValidChunkIndex(int chunkIndex) {
        return chunkIndex >= 0 && chunkIndex < totalChunks;
    }

    /**
     * 检查上传是否已完成（所有分片已上传）
     *
     * @return true 如果所有分片已上传
     */
    public boolean isUploadComplete() {
        return uploadedChunks >= totalChunks;
    }

    /**
     * 递增已上传分片数
     *
     * @throws BusinessRuleViolationException 当会话状态不是 ACTIVE 或分片已全部上传时抛出
     */
    public void incrementUploadedChunks() {
        if (status != UploadSessionStatus.ACTIVE) {
            throw new BusinessRuleViolationException(
                ErrorCode.UPLOAD_NOT_COMPLETE,
                "Cannot upload chunks to a session with status: " + status
            );
        }

        if (isExpired()) {
            throw new BusinessRuleViolationException(
                ErrorCode.UPLOAD_SESSION_EXPIRED,
                "Upload session has expired"
            );
        }

        if (uploadedChunks >= totalChunks) {
            throw new BusinessRuleViolationException(
                ErrorCode.UPLOAD_NOT_COMPLETE,
                "All chunks have already been uploaded"
            );
        }

        this.uploadedChunks++;
    }

    /**
     * 标记会话为已完成
     *
     * @throws BusinessRuleViolationException 当会话未全部上传完成时抛出
     */
    public void markAsCompleted() {
        if (status != UploadSessionStatus.ACTIVE) {
            throw new BusinessRuleViolationException(
                ErrorCode.UPLOAD_NOT_COMPLETE,
                "Cannot complete a session with status: " + status
            );
        }

        if (isExpired()) {
            throw new BusinessRuleViolationException(
                ErrorCode.UPLOAD_SESSION_EXPIRED,
                "Upload session has expired"
            );
        }

        if (!isUploadComplete()) {
            throw new BusinessRuleViolationException(
                ErrorCode.UPLOAD_NOT_COMPLETE,
                String.format("Upload incomplete: %d of %d chunks uploaded", uploadedChunks, totalChunks)
            );
        }

        this.status = UploadSessionStatus.COMPLETED;
    }

    /**
     * 标记会话为已过期
     */
    public void markAsExpired() {
        this.status = UploadSessionStatus.EXPIRED;
    }

    /**
     * 获取上传进度百分比
     *
     * @return 进度百分比（0-100）
     */
    public int getProgressPercentage() {
        if (totalChunks == 0) {
            return 0;
        }
        return (int) ((uploadedChunks * 100L) / totalChunks);
    }

    // Getters
    public String getUploadId() {
        return uploadId;
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

    public int getTotalChunks() {
        return totalChunks;
    }

    public int getUploadedChunks() {
        return uploadedChunks;
    }

    public String getTempDirectory() {
        return tempDirectory;
    }

    public UploadSessionStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    // Setters for persistence layer (package-private)
    void setUploadId(String uploadId) {
        this.uploadId = uploadId;
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

    void setTotalChunks(int totalChunks) {
        this.totalChunks = totalChunks;
    }

    void setUploadedChunks(int uploadedChunks) {
        this.uploadedChunks = uploadedChunks;
    }

    void setTempDirectory(String tempDirectory) {
        this.tempDirectory = tempDirectory;
    }

    void setStatus(UploadSessionStatus status) {
        this.status = status;
    }

    void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    @Override
    public String toString() {
        return String.format("UploadSession[id=%s, file=%s, chunks=%d/%d, status=%s, progress=%d%%]",
            uploadId, fileName, uploadedChunks, totalChunks, status, getProgressPercentage());
    }
}
