-- V5: Create User & Settings Context tables
-- UserProfile table: Stores user profile information (MVP single-user mode)
CREATE TABLE user_profile (
    id           VARCHAR(36)  NOT NULL PRIMARY KEY,
    display_name VARCHAR(100) NOT NULL,
    email        VARCHAR(255),
    avatar_url   VARCHAR(500),
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- NotificationPreference table: Stores user notification preferences
CREATE TABLE notification_preference (
    id                VARCHAR(36) NOT NULL PRIMARY KEY,
    upload_complete   BOOLEAN     NOT NULL DEFAULT TRUE,
    promotion_success BOOLEAN     NOT NULL DEFAULT TRUE,
    system_updates    BOOLEAN     NOT NULL DEFAULT FALSE,
    updated_at        TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ApiKey table: Stores BCrypt-hashed API keys for external access
CREATE TABLE api_key (
    id           VARCHAR(36)  NOT NULL PRIMARY KEY,
    name         VARCHAR(100) NOT NULL,
    hashed_key   VARCHAR(255) NOT NULL,
    prefix       VARCHAR(50)  NOT NULL,
    expires_at   TIMESTAMP    NULL,
    last_used_at TIMESTAMP    NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
