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
    private UploadSessionStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    private UploadSession() {
    }

    public static UploadSession create(String fileName, long fileSize, VideoFormat format,
                                       String storageKey, String ossBucket, Long chunkSize) {
        if (fileName == null || fileName.isBlank()) {
            throw new BusinessRuleViolationException(
                ErrorCode.INTERNAL_SERVER_ERROR,
                "File name must not be blank"
            );
        }

        if (fileSize <= 0) {
            throw new BusinessRuleViolationException(
                ErrorCode.VIDEO_FILE_SIZE_EXCEEDED,
                "File size must be greater than 0"
            );
        }

        if (format == null) {
            throw new BusinessRuleViolationException(
                ErrorCode.UNSUPPORTED_VIDEO_FORMAT,
                "Video format must not be null"
            );
        }

        if (storageKey == null || storageKey.isBlank()) {
            throw new BusinessRuleViolationException(
                ErrorCode.INTERNAL_SERVER_ERROR,
                "Storage key must not be blank"
            );
        }

        if (ossBucket == null || ossBucket.isBlank()) {
            throw new BusinessRuleViolationException(
                ErrorCode.INTERNAL_SERVER_ERROR,
                "OSS bucket must not be blank"
            );
        }

        long actualChunkSize = chunkSize != null && chunkSize > 0 ? chunkSize : DEFAULT_CHUNK_SIZE;
        int totalChunks = calculateTotalChunks(fileSize, actualChunkSize);

        UploadSession session = new UploadSession();
        session.uploadId = generateUploadId();
        session.fileName = fileName;
        session.fileSize = fileSize;
        session.format = format;
        session.totalChunks = totalChunks;
        session.uploadedChunks = 0;
        session.storageKey = storageKey;
        session.ossBucket = ossBucket;
        session.tempDirectory = null;
        session.status = UploadSessionStatus.ACTIVE;
        session.createdAt = LocalDateTime.now();
        session.expiresAt = session.createdAt.plus(DEFAULT_SESSION_TTL_HOURS, ChronoUnit.HOURS);

        return session;
    }

    @Deprecated
    public static UploadSession create(String fileName, long fileSize, VideoFormat format,
                                       String tempDirectory, Long chunkSize) {
        if (fileName == null || fileName.isBlank()) {
            throw new BusinessRuleViolationException(
                ErrorCode.INTERNAL_SERVER_ERROR,
                "File name must not be blank"
            );
        }

        if (fileSize <= 0) {
            throw new BusinessRuleViolationException(
                ErrorCode.VIDEO_FILE_SIZE_EXCEEDED,
                "File size must be greater than 0"
            );
        }

        if (format == null) {
            throw new BusinessRuleViolationException(
                ErrorCode.UNSUPPORTED_VIDEO_FORMAT,
                "Video format must not be null"
            );
        }

        if (tempDirectory == null || tempDirectory.isBlank()) {
            throw new BusinessRuleViolationException(
                ErrorCode.INTERNAL_SERVER_ERROR,
                "Temp directory must not be blank"
            );
        }

        long actualChunkSize = chunkSize != null && chunkSize > 0 ? chunkSize : DEFAULT_CHUNK_SIZE;
        int totalChunks = calculateTotalChunks(fileSize, actualChunkSize);

        UploadSession session = new UploadSession();
        session.uploadId = generateUploadId();
        session.fileName = fileName;
        session.fileSize = fileSize;
        session.format = format;
        session.totalChunks = totalChunks;
        session.uploadedChunks = 0;
        session.storageKey = "local/" + session.uploadId;
        session.ossBucket = "local";
        session.tempDirectory = tempDirectory;
        session.status = UploadSessionStatus.ACTIVE;
        session.createdAt = LocalDateTime.now();
        session.expiresAt = session.createdAt.plus(DEFAULT_SESSION_TTL_HOURS, ChronoUnit.HOURS);

        return session;
    }

    public static UploadSession createWithId(String uploadId, String fileName, long fileSize,
                                              VideoFormat format, String storageKey, String ossBucket, Long chunkSize) {
        if (uploadId == null || uploadId.isBlank()) {
            throw new BusinessRuleViolationException(
                ErrorCode.INTERNAL_SERVER_ERROR,
                "Upload ID must not be blank"
            );
        }

        UploadSession session = create(fileName, fileSize, format, storageKey, ossBucket, chunkSize);
        session.uploadId = uploadId;
        return session;
    }

    @Deprecated
    public static UploadSession createWithId(String uploadId, String fileName, long fileSize,
                                              VideoFormat format, String tempDirectory, Long chunkSize) {
        if (uploadId == null || uploadId.isBlank()) {
            throw new BusinessRuleViolationException(
                ErrorCode.INTERNAL_SERVER_ERROR,
                "Upload ID must not be blank"
            );
        }

        if (fileName == null || fileName.isBlank()) {
            throw new BusinessRuleViolationException(
                ErrorCode.INTERNAL_SERVER_ERROR,
                "File name must not be blank"
            );
        }

        if (fileSize <= 0) {
            throw new BusinessRuleViolationException(
                ErrorCode.VIDEO_FILE_SIZE_EXCEEDED,
                "File size must be greater than 0"
            );
        }

        if (format == null) {
            throw new BusinessRuleViolationException(
                ErrorCode.UNSUPPORTED_VIDEO_FORMAT,
                "Video format must not be null"
            );
        }

        if (tempDirectory == null || tempDirectory.isBlank()) {
            throw new BusinessRuleViolationException(
                ErrorCode.INTERNAL_SERVER_ERROR,
                "Temp directory must not be blank"
            );
        }

        long actualChunkSize = chunkSize != null && chunkSize > 0 ? chunkSize : DEFAULT_CHUNK_SIZE;
        int totalChunks = calculateTotalChunks(fileSize, actualChunkSize);

        UploadSession session = new UploadSession();
        session.uploadId = uploadId;
        session.fileName = fileName;
        session.fileSize = fileSize;
        session.format = format;
        session.totalChunks = totalChunks;
        session.uploadedChunks = 0;
        session.storageKey = "local/" + uploadId;
        session.ossBucket = "local";
        session.tempDirectory = tempDirectory;
        session.status = UploadSessionStatus.ACTIVE;
        session.createdAt = LocalDateTime.now();
        session.expiresAt = session.createdAt.plus(DEFAULT_SESSION_TTL_HOURS, ChronoUnit.HOURS);

        return session;
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

    @Override
    public String toString() {
        return String.format("UploadSession[id=%s, file=%s, chunks=%d/%d, status=%s, progress=%d%%, bucket=%s]",
            uploadId, fileName, uploadedChunks, totalChunks, status, getProgressPercentage(), ossBucket);
    }
}