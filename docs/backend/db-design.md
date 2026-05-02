# 数据库设计（Database Design）

---

## 1. 阿里云 RDS 连接信息

| 项目 | 值 |
|------|----|
| 云服务商 | 阿里云 RDS for MySQL |
| 实例连接地址 | `rm-2ze54x34bt9597z4ryo.mysql.rds.aliyuncs.com` |
| 端口 | `3306` |
| 数据库名 | `xyfkitchen` |
| 用户名 | `neilluo1220` |
| 字符集 | `utf8mb4` |
| 排序规则 | `utf8mb4_unicode_ci` |
| 存储引擎 | InnoDB |

### 1.1 Spring Boot 连接配置

```yaml
# application-dev.yml（本地开发）
spring:
  datasource:
    url: jdbc:mysql://rm-2ze54x34bt9597z4ryo.mysql.rds.aliyuncs.com:3306/xyfkitchen?useUnicode=true&characterEncoding=utf8mb4&useSSL=true&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: neilluo1220
    password: ${MYSQL_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
```

```yaml
# application-prod.yml（生产环境 — 所有敏感值走环境变量）
spring:
  datasource:
    url: jdbc:mysql://${MYSQL_HOST}:${MYSQL_PORT:3306}/${MYSQL_DATABASE:xyfkitchen}?useUnicode=true&characterEncoding=utf8mb4&useSSL=true&serverTimezone=Asia/Shanghai&requireSSL=true&verifyServerCertificate=true
    username: ${MYSQL_USERNAME}
    password: ${MYSQL_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
```

> 数据库密码禁止硬编码，统一通过环境变量 `MYSQL_PASSWORD` 注入。

---

## 2. 全量 DDL（含索引）

执行顺序：Phase 1 表无依赖；Phase 2 表通过应用层逻辑引用 Phase 1 的表（不使用物理外键）。

```sql
-- ============================================
-- Phase 1: 无依赖的表
-- ============================================

-- Video Context
CREATE TABLE video (
    id                VARCHAR(36)  NOT NULL PRIMARY KEY,
    file_name         VARCHAR(255) NOT NULL,
    file_size         BIGINT       NOT NULL,
    format            VARCHAR(10)  NOT NULL,
    duration_seconds  BIGINT,
    file_path         VARCHAR(500) NOT NULL,
    status            VARCHAR(30)  NOT NULL DEFAULT 'UPLOADED',
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_video_status (status),
    INDEX idx_video_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE upload_session (
    upload_id         VARCHAR(36)  NOT NULL PRIMARY KEY,
    file_name         VARCHAR(255) NOT NULL,
    file_size         BIGINT       NOT NULL,
    format            VARCHAR(10)  NOT NULL,
    total_chunks      INT          NOT NULL,
    uploaded_chunks   INT          NOT NULL DEFAULT 0,
    temp_directory    VARCHAR(500) NOT NULL,
    status            VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at        TIMESTAMP    NOT NULL,
    INDEX idx_upload_session_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Distribution Context
CREATE TABLE oauth_token (
    id                       VARCHAR(36)  NOT NULL PRIMARY KEY,
    platform                 VARCHAR(30)  NOT NULL,
    encrypted_access_token   TEXT         NOT NULL,
    encrypted_refresh_token  TEXT,
    expires_at               TIMESTAMP,
    created_at               TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_oauth_token_platform (platform)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Promotion Context
CREATE TABLE promotion_channel (
    id                VARCHAR(36)  NOT NULL PRIMARY KEY,
    name              VARCHAR(100) NOT NULL,
    type              VARCHAR(30)  NOT NULL,
    channel_url       VARCHAR(500),
    encrypted_api_key TEXT,
    priority          INT          NOT NULL DEFAULT 0,
    status            VARCHAR(20)  NOT NULL DEFAULT 'ENABLED',
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_promotion_channel_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- User & Settings Context
CREATE TABLE user_profile (
    id            VARCHAR(36)  NOT NULL PRIMARY KEY,
    display_name  VARCHAR(100) NOT NULL,
    email         VARCHAR(255),
    avatar_url    VARCHAR(500),
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE notification_preference (
    id                VARCHAR(36) NOT NULL PRIMARY KEY,
    upload_complete   BOOLEAN     NOT NULL DEFAULT TRUE,
    promotion_success BOOLEAN     NOT NULL DEFAULT TRUE,
    system_updates    BOOLEAN     NOT NULL DEFAULT FALSE,
    updated_at        TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE api_key (
    id           VARCHAR(36)  NOT NULL PRIMARY KEY,
    name         VARCHAR(100) NOT NULL,
    hashed_key   VARCHAR(255) NOT NULL,
    prefix       VARCHAR(50)  NOT NULL,
    expires_at   TIMESTAMP    NULL,
    last_used_at TIMESTAMP    NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_api_key_prefix (prefix)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Phase 2: 有逻辑依赖的表（不使用物理外键，通过应用层保证一致性）
-- ============================================

-- Metadata Context
CREATE TABLE video_metadata (
    id          VARCHAR(36)  NOT NULL PRIMARY KEY,
    video_id    VARCHAR(36)  NOT NULL,
    title       VARCHAR(200) NOT NULL,
    description TEXT,
    tags_json   TEXT,
    source      VARCHAR(20)  NOT NULL DEFAULT 'AI_GENERATED',
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_video_metadata_video_id (video_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Distribution Context
CREATE TABLE publish_record (
    id               VARCHAR(36)  NOT NULL PRIMARY KEY,
    video_id         VARCHAR(36)  NOT NULL,
    metadata_id      VARCHAR(36)  NOT NULL,
    platform         VARCHAR(30)  NOT NULL,
    status           VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
    video_url        VARCHAR(500),
    upload_task_id   VARCHAR(100),
    progress_percent INT          NOT NULL DEFAULT 0,
    error_message    TEXT,
    published_at     TIMESTAMP    NULL,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_publish_record_video_id (video_id),
    INDEX idx_publish_record_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Promotion Context
CREATE TABLE promotion_record (
    id              VARCHAR(36)  NOT NULL PRIMARY KEY,
    video_id        VARCHAR(36)  NOT NULL,
    channel_id      VARCHAR(36)  NOT NULL,
    promotion_copy  TEXT,
    method          VARCHAR(20)  NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    result_url      VARCHAR(500),
    error_message   TEXT,
    executed_at     TIMESTAMP    NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_promotion_record_video_id (video_id),
    INDEX idx_promotion_record_channel_id (channel_id),
    INDEX idx_promotion_record_status (status),
    INDEX idx_promotion_record_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

## 3. 索引清单

| 表 | 索引名 | 索引列 | 类型 |
|----|--------|--------|------|
| `video` | `idx_video_status` | `status` | 普通 |
| `video` | `idx_video_created_at` | `created_at` | 普通 |
| `upload_session` | `idx_upload_session_status` | `status` | 普通 |
| `oauth_token` | `uk_oauth_token_platform` | `platform` | UNIQUE |
| `promotion_channel` | `idx_promotion_channel_status` | `status` | 普通 |
| `api_key` | `idx_api_key_prefix` | `prefix` | 普通 |
| `video_metadata` | `idx_video_metadata_video_id` | `video_id` | 普通 |
| `publish_record` | `idx_publish_record_video_id` | `video_id` | 普通 |
| `publish_record` | `idx_publish_record_status` | `status` | 普通 |
| `promotion_record` | `idx_promotion_record_video_id` | `video_id` | 普通 |
| `promotion_record` | `idx_promotion_record_channel_id` | `channel_id` | 普通 |
| `promotion_record` | `idx_promotion_record_status` | `status` | 普通 |
| `promotion_record` | `idx_promotion_record_created_at` | `created_at` | 普通 |


