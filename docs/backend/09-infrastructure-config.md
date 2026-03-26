# 基础设施与配置（Infrastructure & Configuration）

> 依赖文档：[01-project-scaffolding.md](./01-project-scaffolding.md)
> 相关文档：所有限界上下文（[03](./03-context-video.md)-[07](./07-context-user-settings.md)）
> 扩展文档：[db-design.md](./db-design.md)（数据库设计规范、RDS 连接、Flyway）、[log-design.md](./log-design.md)（日志规范、Trace ID、按天滚动）
> 需求映射：需求 8（8.4, 8.5）— 基础设施层解耦

---

## A. 数据库配置

> 完整的数据库设计规范（命名规范、索引策略、Flyway 版本管理、RDS 安全配置）参见 [db-design.md](./db-design.md)。本节仅包含 Spring Boot 连接配置和 DDL。

### A1. MySQL 连接配置

```yaml
spring:
  datasource:
    url: jdbc:mysql://${MYSQL_HOST:localhost}:${MYSQL_PORT:3306}/${MYSQL_DATABASE:grace}?useUnicode=true&characterEncoding=utf8mb4&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: ${MYSQL_USERNAME:grace}
    password: ${MYSQL_PASSWORD:grace}
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: ${HIKARI_MAX_POOL:10}
      minimum-idle: ${HIKARI_MIN_IDLE:5}
      idle-timeout: 300000
      max-lifetime: 600000
      connection-timeout: 30000

# MyBatis 配置
mybatis:
  mapper-locations: classpath:mapper/**/*.xml
  type-aliases-package: com.grace.platform
  type-handlers-package: com.grace.platform.shared.infrastructure.persistence.typehandler
  configuration:
    map-underscore-to-camel-case: true
    default-enum-type-handler: org.apache.ibatis.type.EnumTypeHandler
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl
```

### A2. 全量 DDL 汇总

以下按限界上下文合并所有 DDL，按执行顺序排列（考虑外键依赖）：

```sql
-- ============================================
-- Phase 1: 无外键依赖的表
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
-- Phase 2: 有外键逻辑依赖的表（不使用物理外键，通过应用层保证一致性）
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

**设计决策 — 不使用物理外键：**

| 原因 | 说明 |
|------|------|
| DDD 上下文隔离 | 各限界上下文的表逻辑上独立，跨上下文不建物理外键 |
| 部署灵活性 | 未来可按上下文拆分数据库 |
| 数据一致性 | 通过领域事件和应用层逻辑保证引用完整性 |

---

## B. 外部服务配置

### B1. 阿里云 LLM（通义千问）

```yaml
grace:
  llm:
    provider: qwen
    api-key: ${QWEN_API_KEY}
    model: ${QWEN_MODEL:qwen-plus}
    base-url: ${QWEN_BASE_URL:https://dashscope.aliyuncs.com/compatible-mode/v1}
    temperature: ${QWEN_TEMPERATURE:0.7}
    max-tokens: ${QWEN_MAX_TOKENS:2000}
    timeout-seconds: ${QWEN_TIMEOUT:30}
    retry:
      max-attempts: 3
      backoff-multiplier: 2     # 指数退避：1s, 2s, 4s
      initial-interval-ms: 1000
```

**配置类：**

```java
@Configuration
@ConfigurationProperties(prefix = "grace.llm")
public class LlmProperties {
    private String provider;
    private String apiKey;
    private String model;
    private String baseUrl;
    private double temperature;
    private int maxTokens;
    private int timeoutSeconds;
    private RetryProperties retry;
    
    @Data
    public static class RetryProperties {
        private int maxAttempts;
        private int backoffMultiplier;
        private long initialIntervalMs;
    }
}
```

### B2. YouTube Data API v3

```yaml
grace:
  youtube:
    client-id: ${YOUTUBE_CLIENT_ID}
    client-secret: ${YOUTUBE_CLIENT_SECRET}
    redirect-uri: ${YOUTUBE_REDIRECT_URI:http://localhost:8080/api/distribution/auth/youtube/callback}
    scopes:
      - https://www.googleapis.com/auth/youtube.upload
      - https://www.googleapis.com/auth/youtube.readonly
    api-base-url: https://www.googleapis.com/youtube/v3
    upload-url: https://www.googleapis.com/upload/youtube/v3/videos
    quota-retry:
      enabled: true
      fixed-delay-ms: 1800000    # 30 分钟
      max-retry-count: 5
```

**配置类：**

```java
@Configuration
@ConfigurationProperties(prefix = "grace.youtube")
public class YouTubeProperties {
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private List<String> scopes;
    private String apiBaseUrl;
    private String uploadUrl;
    private QuotaRetryProperties quotaRetry;
    
    @Data
    public static class QuotaRetryProperties {
        private boolean enabled;
        private long fixedDelayMs;
        private int maxRetryCount;
    }
}
```

### B3. OpenCrawl Agentic API

```yaml
grace:
  opencrawl:
    base-url: ${OPENCRAWL_BASE_URL:https://api.opencrawl.io/v1}
    api-key: ${OPENCRAWL_API_KEY}
    timeout-seconds: ${OPENCRAWL_TIMEOUT:60}
    retry:
      max-attempts: 1            # 不自动重试，失败由用户手动触发
```

**配置类：**

```java
@Configuration
@ConfigurationProperties(prefix = "grace.opencrawl")
public class OpenCrawlProperties {
    private String baseUrl;
    private String apiKey;
    private int timeoutSeconds;
    private RetryProperties retry;
    
    @Data
    public static class RetryProperties {
        private int maxAttempts;
    }
}
```

---

## C. 加密配置

### C1. AES-256-GCM 加密

用于加密 OAuth Token 和 Promotion Channel API Key。

```yaml
grace:
  encryption:
    algorithm: AES/GCM/NoPadding
    key: ${GRACE_ENCRYPTION_KEY}    # 32 字节 Base64 编码的 AES 密钥
    iv-length: 12                    # GCM 推荐 IV 长度
    tag-length: 128                  # GCM 认证标签长度（bit）
```

**密钥生成指南（环境准备时执行一次）：**

```bash
# 生成 256-bit AES 密钥（Base64 编码）
openssl rand -base64 32
# 输出示例：K7gNU3sdo+OL0wNhqoVWhr3g6s1xYv72ol/pe/Unols=
```

**配置类：**

```java
@Configuration
@ConfigurationProperties(prefix = "grace.encryption")
public class EncryptionProperties {
    private String algorithm;
    private String key;          // Base64 编码的 AES 密钥
    private int ivLength;
    private int tagLength;
}
```

### C2. BCrypt 配置

用于 API Key 的单向哈希。使用 Spring Security 的 BCryptPasswordEncoder。

```java
@Configuration
public class SecurityConfig {
    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder(12); // strength = 12
    }
}
```

---

## D. 文件存储配置

```yaml
grace:
  storage:
    # 视频文件存储根目录
    video-dir: ${GRACE_VIDEO_DIR:./storage/videos}
    # 分片上传临时目录
    temp-dir: ${GRACE_TEMP_DIR:./storage/temp}
    # 用户头像目录
    avatar-dir: ${GRACE_AVATAR_DIR:./storage/avatars}
    # 头像文件限制
    avatar-max-size: 2MB
    avatar-allowed-types:
      - image/jpeg
      - image/png
```

**配置类：**

```java
@Configuration
@ConfigurationProperties(prefix = "grace.storage")
public class StorageProperties {
    private String videoDir;
    private String tempDir;
    private String avatarDir;
    private String avatarMaxSize;
    private List<String> avatarAllowedTypes;
    
    @PostConstruct
    public void init() {
        // 确保存储目录存在
        createDirectoryIfNotExists(videoDir);
        createDirectoryIfNotExists(tempDir);
        createDirectoryIfNotExists(avatarDir);
    }
}
```

**Spring multipart 配置：**

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 5GB           # 视频文件上限
      max-request-size: 5GB
      file-size-threshold: 10MB    # 超过此值写入临时文件
```

---

## E. CORS 配置

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Value("${grace.cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins(allowedOrigins.split(","))
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600);
    }
}
```

---

## F. Jackson 序列化配置

```java
@Configuration
public class JacksonConfig {
    
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
}
```

---

## G. 日志配置

> 完整的日志设计规范（Trace ID 链路追踪、请求出入参日志、logback-spring.xml 配置、按天滚动策略、各层日志级别）参见 [log-design.md](./log-design.md)。以下为 application.yml 中的基础日志配置。

```yaml
logging:
  level:
    root: INFO
    com.grace.platform: ${LOG_LEVEL:DEBUG}
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: ${LOG_FILE:./logs/grace-platform.log}
    max-size: 100MB
    max-history: 30
```

---

## H. 环境变量总表

| 变量 | 必填 | 默认值 | 说明 | 来源上下文 |
|------|------|--------|------|-----------|
| `MYSQL_HOST` | 否 | localhost | MySQL 主机 | 全局 |
| `MYSQL_PORT` | 否 | 3306 | MySQL 端口 | 全局 |
| `MYSQL_DATABASE` | 否 | grace | 数据库名 | 全局 |
| `MYSQL_USERNAME` | 否 | grace | 数据库用户 | 全局 |
| `MYSQL_PASSWORD` | 否 | grace | 数据库密码 | 全局 |
| `GRACE_ENCRYPTION_KEY` | **是** | - | AES-256 密钥（Base64） | Distribution, Promotion |
| `QWEN_API_KEY` | **是** | - | 通义千问 API Key | Metadata, Promotion |
| `QWEN_MODEL` | 否 | qwen-plus | LLM 模型名称 | Metadata, Promotion |
| `QWEN_BASE_URL` | 否 | dashscope URL | LLM API 地址 | Metadata, Promotion |
| `YOUTUBE_CLIENT_ID` | **是** | - | YouTube OAuth Client ID | Distribution |
| `YOUTUBE_CLIENT_SECRET` | **是** | - | YouTube OAuth Client Secret | Distribution |
| `YOUTUBE_REDIRECT_URI` | 否 | localhost callback | OAuth 回调 URL | Distribution |
| `OPENCRAWL_BASE_URL` | 否 | api.opencrawl.io | OpenCrawl API 地址 | Promotion |
| `OPENCRAWL_API_KEY` | **是** | - | OpenCrawl API Key | Promotion |
| `GRACE_VIDEO_DIR` | 否 | ./storage/videos | 视频存储目录 | Video |
| `GRACE_TEMP_DIR` | 否 | ./storage/temp | 临时文件目录 | Video |
| `GRACE_AVATAR_DIR` | 否 | ./storage/avatars | 头像存储目录 | User & Settings |
| `LOG_LEVEL` | 否 | DEBUG | 应用日志级别 | 全局 |
| `LOG_FILE` | 否 | ./logs/grace-platform.log | 日志文件路径 | 全局 |

---

## I. Spring Boot 完整 application.yml 模板

```yaml
server:
  port: ${SERVER_PORT:8080}

spring:
  application:
    name: grace-platform
  datasource:
    url: jdbc:mysql://${MYSQL_HOST:localhost}:${MYSQL_PORT:3306}/${MYSQL_DATABASE:grace}?useUnicode=true&characterEncoding=utf8mb4&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
    username: ${MYSQL_USERNAME:grace}
    password: ${MYSQL_PASSWORD:grace}
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: ${HIKARI_MAX_POOL:10}
      minimum-idle: ${HIKARI_MIN_IDLE:5}
      idle-timeout: 300000
      max-lifetime: 600000
      connection-timeout: 30000
  servlet:
    multipart:
      max-file-size: 5GB
      max-request-size: 5GB
      file-size-threshold: 10MB

# MyBatis 配置
mybatis:
  mapper-locations: classpath:mapper/**/*.xml
  type-aliases-package: com.grace.platform
  type-handlers-package: com.grace.platform.shared.infrastructure.persistence.typehandler
  configuration:
    map-underscore-to-camel-case: true
    default-enum-type-handler: org.apache.ibatis.type.EnumTypeHandler
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl

grace:
  cors:
    allowed-origins: ${CORS_ORIGINS:http://localhost:5173}
  storage:
    video-dir: ${GRACE_VIDEO_DIR:./storage/videos}
    temp-dir: ${GRACE_TEMP_DIR:./storage/temp}
    avatar-dir: ${GRACE_AVATAR_DIR:./storage/avatars}
    avatar-max-size: 2MB
    avatar-allowed-types:
      - image/jpeg
      - image/png
  encryption:
    algorithm: AES/GCM/NoPadding
    key: ${GRACE_ENCRYPTION_KEY}
    iv-length: 12
    tag-length: 128
  llm:
    provider: qwen
    api-key: ${QWEN_API_KEY}
    model: ${QWEN_MODEL:qwen-plus}
    base-url: ${QWEN_BASE_URL:https://dashscope.aliyuncs.com/compatible-mode/v1}
    temperature: ${QWEN_TEMPERATURE:0.7}
    max-tokens: ${QWEN_MAX_TOKENS:2000}
    timeout-seconds: ${QWEN_TIMEOUT:30}
    retry:
      max-attempts: 3
      backoff-multiplier: 2
      initial-interval-ms: 1000
  youtube:
    client-id: ${YOUTUBE_CLIENT_ID}
    client-secret: ${YOUTUBE_CLIENT_SECRET}
    redirect-uri: ${YOUTUBE_REDIRECT_URI:http://localhost:8080/api/distribution/auth/youtube/callback}
    scopes:
      - https://www.googleapis.com/auth/youtube.upload
      - https://www.googleapis.com/auth/youtube.readonly
    api-base-url: https://www.googleapis.com/youtube/v3
    upload-url: https://www.googleapis.com/upload/youtube/v3/videos
    quota-retry:
      enabled: true
      fixed-delay-ms: 1800000
      max-retry-count: 5
  opencrawl:
    base-url: ${OPENCRAWL_BASE_URL:https://api.opencrawl.io/v1}
    api-key: ${OPENCRAWL_API_KEY}
    timeout-seconds: ${OPENCRAWL_TIMEOUT:60}
    retry:
      max-attempts: 1

logging:
  level:
    root: INFO
    com.grace.platform: ${LOG_LEVEL:DEBUG}
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: ${LOG_FILE:./logs/grace-platform.log}
    max-size: 100MB
    max-history: 30
```
