package com.grace.platform.video.domain;

/**
 * 上传任务步骤枚举
 * <p>
 * 5 个大步骤：MERGE → INSPECT → OSS → METADATA → CLEANUP
 * 每个步骤有 PENDING_X 和 RUNNING_X 两种状态
 */
public enum UploadTaskStep {
    PENDING_MERGE, RUNNING_MERGE,
    PENDING_INSPECT, RUNNING_INSPECT,
    PENDING_OSS, RUNNING_OSS,
    PENDING_METADATA, RUNNING_METADATA,
    PENDING_CLEANUP, RUNNING_CLEANUP,
    DONE;

    /**
     * 根据当前 PENDING_X 返回 RUNNING_X
     */
    public UploadTaskStep toRunning() {
        return switch (this) {
            case PENDING_MERGE -> RUNNING_MERGE;
            case PENDING_INSPECT -> RUNNING_INSPECT;
            case PENDING_OSS -> RUNNING_OSS;
            case PENDING_METADATA -> RUNNING_METADATA;
            case PENDING_CLEANUP -> RUNNING_CLEANUP;
            default -> throw new IllegalStateException("Cannot convert to RUNNING: " + this);
        };
    }

    /**
     * 根据当前 RUNNING_X 返回下一个 PENDING_{next}，RUNNING_CLEANUP 返回 DONE
     */
    public UploadTaskStep nextPending() {
        return switch (this) {
            case RUNNING_MERGE -> PENDING_INSPECT;
            case RUNNING_INSPECT -> PENDING_OSS;
            case RUNNING_OSS -> PENDING_METADATA;
            case RUNNING_METADATA -> PENDING_CLEANUP;
            case RUNNING_CLEANUP -> DONE;
            default -> throw new IllegalStateException("Cannot get next PENDING: " + this);
        };
    }

    public boolean isPending() {
        return name().startsWith("PENDING_");
    }

    public boolean isRunning() {
        return name().startsWith("RUNNING_");
    }
}
