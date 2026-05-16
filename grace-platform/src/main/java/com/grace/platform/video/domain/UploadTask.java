package com.grace.platform.video.domain;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * UploadTask 聚合根
 * <p>
 * 负责管理异步视频上传任务的状态机流转。
 * 一视频一行，current_step 粗粒度状态机驱动 5 个大步骤。
 * </p>
 */
public class UploadTask {

    private String id;
    private String videoId;
    private UploadTaskStep currentStep;
    private UploadTaskStatus status;
    private int attempts;
    private int maxAttempts;
    private String lastError;
    private String ownerInstanceId;
    private LocalDateTime heartbeatAt;
    private Map<String, Object> payload;
    private Map<String, Object> extInfo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 静态工厂：创建初始任务
     */
    public static UploadTask createInitial(String videoId, Map<String, Object> payload, Map<String, Object> extInfo) {
        UploadTask t = new UploadTask();
        t.id = "task_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        t.videoId = videoId;
        t.currentStep = UploadTaskStep.PENDING_MERGE;
        t.status = UploadTaskStatus.PENDING;
        t.attempts = 0;
        t.maxAttempts = 5;
        t.payload = payload == null ? new LinkedHashMap<>() : new LinkedHashMap<>(payload);
        t.extInfo = extInfo == null ? new LinkedHashMap<>() : new LinkedHashMap<>(extInfo);
        t.createdAt = LocalDateTime.now();
        t.updatedAt = t.createdAt;
        return t;
    }

    /**
     * 标记任务为运行中
     */
    public void markRunning(String instanceId) {
        this.status = UploadTaskStatus.RUNNING;
        this.currentStep = this.currentStep.toRunning();
        this.ownerInstanceId = instanceId;
        this.heartbeatAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 推进到下一个 PENDING 步骤
     */
    public void advanceToNextPending() {
        this.currentStep = this.currentStep.nextPending();
        if (this.currentStep == UploadTaskStep.DONE) {
            this.status = UploadTaskStatus.DONE;
        } else {
            this.status = UploadTaskStatus.PENDING;
        }
        this.attempts = 0;
        this.ownerInstanceId = null;
        this.heartbeatAt = null;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 标记步骤失败
     * <p>
     * 注意：attempts 已在 claim SQL 中 +1，此处不重复计数。
     * </p>
     */
    public void markFailed(Throwable e) {
        this.lastError = truncateStackTrace(e, 2000);
        this.updatedAt = LocalDateTime.now();

        if (this.attempts >= this.maxAttempts) {
            // 终态失败，保持在 RUNNING_* 便于定位失败阶段
            this.status = UploadTaskStatus.FAILED;
        } else {
            // 可重试，回滚到对应 PENDING_*
            this.status = UploadTaskStatus.PENDING;
            this.currentStep = rollbackToPending(this.currentStep);
        }
    }

    /**
     * 心跳更新
     */
    public void touchHeartbeat() {
        this.heartbeatAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 手动重试重置（用于 retry-metadata 接口）
     */
    public void resetForManualRetry() {
        this.currentStep = UploadTaskStep.PENDING_METADATA;
        this.status = UploadTaskStatus.PENDING;
        this.attempts = 0;
        this.lastError = null;
        this.ownerInstanceId = null;
        this.heartbeatAt = null;
        this.updatedAt = LocalDateTime.now();
    }

    // ==================== Getters ====================

    public String getId() {
        return id;
    }

    public String getVideoId() {
        return videoId;
    }

    public UploadTaskStep getCurrentStep() {
        return currentStep;
    }

    public UploadTaskStatus getStatus() {
        return status;
    }

    public int getAttempts() {
        return attempts;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public String getLastError() {
        return lastError;
    }

    public String getOwnerInstanceId() {
        return ownerInstanceId;
    }

    public LocalDateTime getHeartbeatAt() {
        return heartbeatAt;
    }

    /**
     * 返回 payload 的不可变视图（Effective Java Item 17 & 50）
     */
    public Map<String, Object> getPayload() {
        return payload == null ? Collections.emptyMap() : Collections.unmodifiableMap(payload);
    }

    /**
     * 返回 extInfo 的不可变视图（Effective Java Item 17 & 50）
     */
    public Map<String, Object> getExtInfo() {
        return extInfo == null ? Collections.emptyMap() : Collections.unmodifiableMap(extInfo);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // ==================== Setters for persistence layer (package-private) ====================

    void setId(String id) {
        this.id = id;
    }

    void setVideoId(String videoId) {
        this.videoId = videoId;
    }

    void setCurrentStep(UploadTaskStep currentStep) {
        this.currentStep = currentStep;
    }

    void setStatus(UploadTaskStatus status) {
        this.status = status;
    }

    void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    void setLastError(String lastError) {
        this.lastError = lastError;
    }

    void setOwnerInstanceId(String ownerInstanceId) {
        this.ownerInstanceId = ownerInstanceId;
    }

    void setHeartbeatAt(LocalDateTime heartbeatAt) {
        this.heartbeatAt = heartbeatAt;
    }

    void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    void setExtInfo(Map<String, Object> extInfo) {
        this.extInfo = extInfo;
    }

    void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // ==================== Private Helpers ====================

    private String truncateStackTrace(Throwable e, int maxLength) {
        String stack = e.toString();
        if (e.getCause() != null) {
            stack += "\nCaused by: " + e.getCause().toString();
        }
        return stack.length() > maxLength ? stack.substring(0, maxLength) : stack;
    }

    private UploadTaskStep rollbackToPending(UploadTaskStep runningStep) {
        return switch (runningStep) {
            case RUNNING_MERGE -> UploadTaskStep.PENDING_MERGE;
            case RUNNING_INSPECT -> UploadTaskStep.PENDING_INSPECT;
            case RUNNING_OSS -> UploadTaskStep.PENDING_OSS;
            case RUNNING_METADATA -> UploadTaskStep.PENDING_METADATA;
            case RUNNING_CLEANUP -> UploadTaskStep.PENDING_CLEANUP;
            default -> throw new IllegalStateException("Cannot rollback to PENDING: " + runningStep);
        };
    }
}
