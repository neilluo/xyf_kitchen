package com.grace.platform.video.domain;

import com.grace.platform.shared.ErrorCode;
import com.grace.platform.shared.infrastructure.exception.BusinessRuleViolationException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class UploadSession {

    public static final long DEFAULT_CHUNK_SIZE = 5L * 1024 * 1024;
    public static final int DEFAULT_SESSION_TTL_HOURS = 24;

    private String uploadId;
    private String fileName;
    private long fileSize;
    private VideoFormat format;
    private int totalChunks;
    private int uploadedChunks;
    private String storageKey;
    private String ossBucket;
    private String tempDirectory;
    private UploadMode uploadMode;
    private UploadSessionStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    private UploadSession() {
    }

    /**
     * 创建 Builder 实例，用于构造 UploadSession。
     * <p>
     * 使用示例：
     * <pre>
     * UploadSession session = UploadSession.builder()
     *     .fileName("video.mp4")
     *     .fileSize(10_000_000L)
     *     .format(VideoFormat.MP4)
     *     .storageKey("oss/key")
     *     .ossBucket("my-bucket")
     *     .build();
     * </pre>
     *
     * @see Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public static UploadSession create(String fileName, long fileSize, VideoFormat format,
                                       String storageKey, String ossBucket, Long chunkSize) {
        return builder()
            .fileName(fileName)
            .fileSize(fileSize)
            .format(format)
            .storageKey(storageKey)
            .ossBucket(ossBucket)
            .chunkSize(chunkSize)
            .build();
    }

    @Deprecated
    public static UploadSession create(String fileName, long fileSize, VideoFormat format,
                                       String tempDirectory, Long chunkSize) {
        return builder()
            .fileName(fileName)
            .fileSize(fileSize)
            .format(format)
            .tempDirectory(tempDirectory)
            .chunkSize(chunkSize)
            .serverUploadMode()
            .build();
    }

    public static UploadSession createWithId(String uploadId, String fileName, long fileSize,
                                              VideoFormat format, String storageKey, String ossBucket, Long chunkSize) {
        return builder()
            .uploadId(uploadId)
            .fileName(fileName)
            .fileSize(fileSize)
            .format(format)
            .storageKey(storageKey)
            .ossBucket(ossBucket)
            .chunkSize(chunkSize)
            .build();
    }

    @Deprecated
    public static UploadSession createWithId(String uploadId, String fileName, long fileSize,
                                              VideoFormat format, String tempDirectory, Long chunkSize) {
        return builder()
            .uploadId(uploadId)
            .fileName(fileName)
            .fileSize(fileSize)
            .format(format)
            .tempDirectory(tempDirectory)
            .chunkSize(chunkSize)
            .serverUploadMode()
            .build();
    }

    public static int calculateTotalChunks(long fileSize, long chunkSize) {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("Chunk size must be greater than 0");
        }
        return (int) ((fileSize + chunkSize - 1) / chunkSize);
    }

    private static String generateUploadId() {
        return "upl_" + UUID.randomUUID().toString().replace("-", "");
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isValidChunkIndex(int chunkIndex) {
        return chunkIndex >= 0 && chunkIndex < totalChunks;
    }

    public boolean isUploadComplete() {
        return uploadedChunks >= totalChunks;
    }

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
     * 通过 OSS 回调标记上传已完成。
     * <p>
     * OSS 分片上传完成回调时使用，不需要检查 uploadedChunks 数量，
     * 因为 OSS 服务端已经完成了分片合并。
     *
     * @throws BusinessRuleViolationException 当会话状态不是 ACTIVE 或已过期时
     */
    public void markAsCompletedByOssCallback() {
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

        this.uploadedChunks = this.totalChunks;
        this.status = UploadSessionStatus.COMPLETED;
    }

    public void markAsExpired() {
        this.status = UploadSessionStatus.EXPIRED;
    }

    public int getProgressPercentage() {
        if (totalChunks == 0) {
            return 0;
        }
        return (int) ((uploadedChunks * 100L) / totalChunks);
    }

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

    public String getStorageKey() {
        return storageKey;
    }

    public String getOssBucket() {
        return ossBucket;
    }

    @Deprecated
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

    public UploadMode getUploadMode() {
        return uploadMode;
    }

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

    void setStorageKey(String storageKey) {
        this.storageKey = storageKey;
    }

    void setOssBucket(String ossBucket) {
        this.ossBucket = ossBucket;
    }

    @Deprecated
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

    void setUploadMode(UploadMode uploadMode) {
        this.uploadMode = uploadMode;
    }

    @Override
    public String toString() {
        return String.format("UploadSession[id=%s, file=%s, chunks=%d/%d, status=%s, progress=%d%%, bucket=%s]",
            uploadId, fileName, uploadedChunks, totalChunks, status, getProgressPercentage(), ossBucket);
    }

    /**
     * UploadSession 的 Builder，用于替代多参数的静态工厂方法。
     * <p>
     * 必填参数：fileName、fileSize、format。
     * OSS 直传模式（默认）需要额外设置 storageKey 和 ossBucket；
     * 服务端上传模式需要调用 {@link #serverUploadMode()} 并设置 tempDirectory。
     */
    public static class Builder {

        private String uploadId;
        private String fileName;
        private long fileSize;
        private VideoFormat format;
        private String storageKey;
        private String ossBucket;
        private String tempDirectory;
        private Long chunkSize;
        private boolean serverUpload;

        Builder() {
        }

        public Builder uploadId(String uploadId) {
            this.uploadId = uploadId;
            return this;
        }

        public Builder fileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public Builder fileSize(long fileSize) {
            this.fileSize = fileSize;
            return this;
        }

        public Builder format(VideoFormat format) {
            this.format = format;
            return this;
        }

        public Builder storageKey(String storageKey) {
            this.storageKey = storageKey;
            return this;
        }

        public Builder ossBucket(String ossBucket) {
            this.ossBucket = ossBucket;
            return this;
        }

        public Builder tempDirectory(String tempDirectory) {
            this.tempDirectory = tempDirectory;
            return this;
        }

        public Builder chunkSize(Long chunkSize) {
            this.chunkSize = chunkSize;
            return this;
        }

        /**
         * 标记为服务端上传模式（SERVER_UPLOAD），需配合 {@link #tempDirectory(String)} 使用。
         */
        public Builder serverUploadMode() {
            this.serverUpload = true;
            return this;
        }

        public UploadSession build() {
            validateCommonFields();

            String actualUploadId = (uploadId != null && !uploadId.isBlank())
                ? uploadId : generateUploadId();
            long actualChunkSize = (chunkSize != null && chunkSize > 0) ? chunkSize : DEFAULT_CHUNK_SIZE;
            int totalChunks = calculateTotalChunks(fileSize, actualChunkSize);

            UploadSession session = new UploadSession();
            session.uploadId = actualUploadId;
            session.fileName = fileName;
            session.fileSize = fileSize;
            session.format = format;
            session.totalChunks = totalChunks;
            session.uploadedChunks = 0;
            session.status = UploadSessionStatus.ACTIVE;
            session.createdAt = LocalDateTime.now();
            session.expiresAt = session.createdAt.plus(DEFAULT_SESSION_TTL_HOURS, ChronoUnit.HOURS);

            if (serverUpload) {
                validateServerUploadFields();
                session.storageKey = "local/" + actualUploadId;
                session.ossBucket = "local";
                session.tempDirectory = tempDirectory;
                session.uploadMode = UploadMode.SERVER_UPLOAD;
            } else {
                validateOssFields();
                session.storageKey = storageKey;
                session.ossBucket = ossBucket;
                session.tempDirectory = null;
                session.uploadMode = UploadMode.DIRECT_OSS;
            }

            return session;
        }

        private void validateCommonFields() {
            if (fileName == null || fileName.isBlank()) {
                throw new BusinessRuleViolationException(
                    ErrorCode.INTERNAL_SERVER_ERROR, "File name must not be blank");
            }
            if (fileSize <= 0) {
                throw new BusinessRuleViolationException(
                    ErrorCode.VIDEO_FILE_SIZE_EXCEEDED, "File size must be greater than 0");
            }
            if (format == null) {
                throw new BusinessRuleViolationException(
                    ErrorCode.UNSUPPORTED_VIDEO_FORMAT, "Video format must not be null");
            }
        }

        private void validateOssFields() {
            if (storageKey == null || storageKey.isBlank()) {
                throw new BusinessRuleViolationException(
                    ErrorCode.INTERNAL_SERVER_ERROR, "Storage key must not be blank");
            }
            if (ossBucket == null || ossBucket.isBlank()) {
                throw new BusinessRuleViolationException(
                    ErrorCode.INTERNAL_SERVER_ERROR, "OSS bucket must not be blank");
            }
        }

        private void validateServerUploadFields() {
            if (tempDirectory == null || tempDirectory.isBlank()) {
                throw new BusinessRuleViolationException(
                    ErrorCode.INTERNAL_SERVER_ERROR, "Temp directory must not be blank");
            }
        }
    }
}
