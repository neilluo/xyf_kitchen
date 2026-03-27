-- V1: Create Video Context tables
-- Video table: Stores video metadata and file information
CREATE TABLE video (
    id              VARCHAR(64)   PRIMARY KEY,
    file_name       VARCHAR(500)  NOT NULL,
    file_size       BIGINT        NOT NULL,
    format          VARCHAR(10)   NOT NULL,
    duration_seconds BIGINT       DEFAULT 0,
    file_path       VARCHAR(1000) NOT NULL,
    status          VARCHAR(30)   NOT NULL DEFAULT 'UPLOADED',
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- UploadSession table: Tracks chunked upload sessions
CREATE TABLE upload_session (
    upload_id       VARCHAR(64)   PRIMARY KEY,
    file_name       VARCHAR(500)  NOT NULL,
    file_size       BIGINT        NOT NULL,
    format          VARCHAR(10)   NOT NULL,
    total_chunks    INT           NOT NULL,
    uploaded_chunks INT           NOT NULL DEFAULT 0,
    temp_directory  VARCHAR(1000) NOT NULL,
    status          VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at      TIMESTAMP     NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
