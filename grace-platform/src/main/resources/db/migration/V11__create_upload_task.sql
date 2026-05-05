-- V11__create_upload_task.sql
CREATE TABLE upload_task (
    id                VARCHAR(36)  NOT NULL PRIMARY KEY,
    video_id          VARCHAR(36)  NOT NULL,
    current_step      VARCHAR(30)  NOT NULL DEFAULT 'PENDING_MERGE',
    status            VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    attempts          INT          NOT NULL DEFAULT 0,
    max_attempts      INT          NOT NULL DEFAULT 5,
    last_error        TEXT,
    owner_instance_id VARCHAR(64),
    heartbeat_at      TIMESTAMP    NULL,
    payload           TEXT,
    ext_info          TEXT,
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_upload_task_video_id (video_id),
    INDEX idx_upload_task_status_step (status, current_step),
    INDEX idx_upload_task_heartbeat (status, heartbeat_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
