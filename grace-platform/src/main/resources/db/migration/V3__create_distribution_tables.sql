-- V3: Create Distribution Context tables
-- PublishRecord table: Tracks video publishing status across platforms
CREATE TABLE publish_record (
    id              VARCHAR(64)   PRIMARY KEY,
    video_id        VARCHAR(64)   NOT NULL,
    metadata_id     VARCHAR(64)   NOT NULL,
    platform        VARCHAR(30)   NOT NULL,
    status          VARCHAR(30)   NOT NULL DEFAULT 'PENDING',
    video_url       VARCHAR(500),
    upload_task_id  VARCHAR(200),
    progress_percent INT          NOT NULL DEFAULT 0,
    error_message   TEXT,
    retry_count     INT           NOT NULL DEFAULT 0,
    published_at    TIMESTAMP     NULL,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_pr_video FOREIGN KEY (video_id) REFERENCES video(id),
    CONSTRAINT fk_pr_metadata FOREIGN KEY (metadata_id) REFERENCES video_metadata(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- OAuthToken table: Stores encrypted OAuth tokens for platform authentication
CREATE TABLE oauth_token (
    id                      VARCHAR(64)   PRIMARY KEY,
    platform                VARCHAR(30)   NOT NULL UNIQUE,
    encrypted_access_token  TEXT          NOT NULL,
    encrypted_refresh_token TEXT          NOT NULL,
    expires_at              TIMESTAMP     NULL DEFAULT NULL,
    created_at              TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
