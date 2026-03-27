-- V4: Create Promotion Context tables
-- PromotionChannel table: Stores promotion channel configurations
CREATE TABLE promotion_channel (
    id              VARCHAR(64)   PRIMARY KEY,
    name            VARCHAR(200)  NOT NULL,
    type            VARCHAR(30)   NOT NULL,
    channel_url     VARCHAR(500)  NOT NULL,
    encrypted_api_key TEXT,
    priority        INT           NOT NULL DEFAULT 1,
    status          VARCHAR(20)   NOT NULL DEFAULT 'ENABLED',
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- PromotionRecord table: Tracks promotion execution status
CREATE TABLE promotion_record (
    id              VARCHAR(64)   PRIMARY KEY,
    video_id        VARCHAR(64)   NOT NULL,
    channel_id      VARCHAR(64)   NOT NULL,
    promotion_copy  TEXT          NOT NULL,
    method          VARCHAR(20)   NOT NULL,
    status          VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    result_url      VARCHAR(500),
    error_message   TEXT,
    executed_at     TIMESTAMP     NULL,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_promo_video FOREIGN KEY (video_id) REFERENCES video(id),
    CONSTRAINT fk_promo_channel FOREIGN KEY (channel_id) REFERENCES promotion_channel(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
