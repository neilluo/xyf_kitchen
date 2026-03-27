-- V2: Create Metadata Context tables
-- VideoMetadata table: Stores AI-generated and manually edited video metadata
CREATE TABLE video_metadata (
    id          VARCHAR(64)   PRIMARY KEY,
    video_id    VARCHAR(64)   NOT NULL,
    title       VARCHAR(200)  NOT NULL,
    description TEXT,
    tags_json   TEXT          NOT NULL,
    source      VARCHAR(20)   NOT NULL DEFAULT 'AI_GENERATED',
    confirmed   BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_metadata_video FOREIGN KEY (video_id) REFERENCES video(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
