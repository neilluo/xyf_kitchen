# 数据库设计规范（Database Design）

> 依赖文档：[09-infrastructure-config.md](./09-infrastructure-config.md)（完整 DDL 与配置）
> 相关文档：所有限界上下文（[03](./03-context-video.md)-[07](./07-context-user-settings.md)）

---

## 1. 数据库实例信息

| 项目 | 值 |
|------|----|
| 云服务商 | 阿里云 RDS for MySQL |
| 实例连接地址 | `rm-2ze54x34bt9597z4ryo.mysql.rds.aliyuncs.com` |
| 端口 | `3306` |
| 数据库名 | `grace` |
| 用户名 | `neilluo1220` |
| 字符集 | `utf8mb4` |
| 排序规则 | `utf8mb4_unicode_ci` |
| 存储引擎 | InnoDB |

### 1.1 多环境连接配置

通过 Spring Boot Profile 和环境变量隔离各环境的数据库连接：

```yaml
# application-dev.yml（本地开发）
spring:
  datasource:
    url: jdbc:mysql://rm-2ze54x34bt9597z4ryo.mysql.rds.aliyuncs.com:3306/grace?useUnicode=true&characterEncoding=utf8mb4&useSSL=true&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: neilluo1220
    password: ${MYSQL_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver

# application-prod.yml（生产环境 — 所有敏感值走环境变量）
spring:
  datasource:
    url: jdbc:mysql://${MYSQL_HOST}:${MYSQL_PORT:3306}/${MYSQL_DATABASE:grace}?useUnicode=true&characterEncoding=utf8mb4&useSSL=true&serverTimezone=Asia/Shanghai&requireSSL=true&verifyServerCertificate=true
    username: ${MYSQL_USERNAME}
    password: ${MYSQL_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
```

> **安全提醒**：数据库密码 **禁止** 硬编码在 `application.yml` 或代码中。开发环境使用 `application-dev.yml`（已加入 `.gitignore`），生产环境通过环境变量 `MYSQL_PASSWORD` 注入。

### 1.2 连接池配置（HikariCP）

```yaml
spring:
  datasource:
    hikari:
      pool-name: GraceHikariPool
      maximum-pool-size: ${HIKARI_MAX_POOL:10}
      minimum-idle: ${HIKARI_MIN_IDLE:5}
      idle-timeout: 300000          # 5 分钟空闲后回收
      max-lifetime: 600000          # 10 分钟强制回收（需小于 RDS wait_timeout）
      connection-timeout: 30000     # 30 秒获取连接超时
      connection-test-query: SELECT 1
      leak-detection-threshold: 60000  # 60 秒未归还视为泄漏（开发环境启用）
```

**阿里云 RDS 推荐参数对照：**

| HikariCP 参数 | 推荐值 | 说明 |
|---------------|--------|------|
| `maximum-pool-size` | CPU 核数 * 2 + 1 | 4 核机器建议 10 |
| `max-lifetime` | 600000 (10min) | 必须小于 RDS `wait_timeout`（默认 28800s） |
| `connection-test-query` | `SELECT 1` | RDS 闲置连接会被自动断开，需要心跳检测 |

---

## 2. 命名规范

### 2.1 数据库对象命名

| 对象 | 规则 | 示例 |
|------|------|------|
| 数据库名 | 全小写，项目名 | `grace` |
| 表名 | 全小写，snake_case，单数形式 | `video`, `publish_record` |
| 列名 | 全小写，snake_case | `video_id`, `created_at` |
| 主键列 | 统一命名 `id` | `id VARCHAR(36)` |
| 外键引用列 | `被引用表_id` | `video_id`, `channel_id` |
| 索引名 | `idx_表名_列名` | `idx_video_status` |
| 唯一索引名 | `uk_表名_列名` | `uk_oauth_token_platform` |
| 时间戳列 | `created_at` / `updated_at` / `xxx_at` | `published_at`, `expires_at` |
| 布尔列 | 形容词或 `is_xxx`（DDL 中用 BOOLEAN） | `upload_complete`, `system_updates` |
| JSON 字段 | `xxx_json` 后缀 | `tags_json` |

### 2.2 Java 映射命名

MyBatis 已启用 `map-underscore-to-camel-case: true`，无需手动映射：

| 数据库列 | Java 字段 | 自动映射 |
|----------|-----------|---------|
| `video_id` | `videoId` | YES |
| `created_at` | `createdAt` | YES |
| `tags_json` | `tagsJson` | YES |
| `progress_percent` | `progressPercent` | YES |

---

## 3. 表设计规范

### 3.1 通用规则

| 规则 | 说明 |
|------|------|
| 主键策略 | UUID v4，`VARCHAR(36)`，应用层生成（`UUID.randomUUID().toString()`） |
| 时间戳 | 每张表必须有 `created_at`，可变实体必须有 `updated_at` |
| 时间戳默认值 | `TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP` |
| 自动更新 | `updated_at` 加 `ON UPDATE CURRENT_TIMESTAMP` |
| NOT NULL | 除明确可空的业务字段外，所有列默认 `NOT NULL` |
| 字符集 | 建表统一加 `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci` |
| 无物理外键 | DDD 架构下不使用 `FOREIGN KEY`，跨上下文引用通过应用层保证 |

### 3.2 主键设计

```
+-------------------------------------------+
|  为什么选 UUID 而不是自增 ID？              |
|  1. DDD 聚合根 ID 由领域层生成，不依赖 DB    |
|  2. 未来跨库/分库无冲突                     |
|  3. 避免暴露业务量信息                      |
+-------------------------------------------+
```

**Typed ID 在 MyBatis 中的映射：**

每个聚合根的 ID 在 Java 中是强类型的 record（如 `VideoId`），通过 TypeHandler 与数据库 `VARCHAR(36)` 互转：

```java
// Domain 层 — Typed ID
public record VideoId(String value) {
    public VideoId {
        Objects.requireNonNull(value, "VideoId value must not be null");
    }
}

// Infrastructure 层 — MyBatis TypeHandler
@MappedTypes(VideoId.class)
public class VideoIdTypeHandler extends BaseTypeHandler<VideoId> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, 
                                     VideoId parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, parameter.value());
    }

    @Override
    public VideoId getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return value == null ? null : new VideoId(value);
    }

    @Override
    public VideoId getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return value == null ? null : new VideoId(value);
    }

    @Override
    public VideoId getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return value == null ? null : new VideoId(value);
    }
}
```

### 3.3 枚举存储

枚举统一存为 `VARCHAR`，MyBatis 使用 `EnumTypeHandler`（默认已配置）：

| 枚举类 | 数据库列 | 列类型 | 存储值示例 |
|--------|---------|--------|-----------|
| `VideoStatus` | `status` | `VARCHAR(30)` | `UPLOADED`, `METADATA_CONFIRMED` |
| `PublishStatus` | `status` | `VARCHAR(30)` | `PENDING`, `UPLOADING`, `PUBLISHED` |
| `PlatformType` | `platform` | `VARCHAR(30)` | `YOUTUBE`, `TIKTOK` |
| `PromotionMethod` | `method` | `VARCHAR(20)` | `API_POST`, `CRAWL_SUBMIT` |

### 3.4 JSON 字段处理

当字段是列表或动态结构时，使用 `TEXT` + JSON 字符串存储，通过自定义 TypeHandler 序列化/反序列化：

```java
// 场景：video_metadata.tags_json 存储 ["tag1", "tag2"]
@MappedTypes(List.class)
public class JsonStringListTypeHandler extends BaseTypeHandler<List<String>> {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, 
                                     List<String> parameter, JdbcType jdbcType) throws SQLException {
        try {
            ps.setString(i, MAPPER.writeValueAsString(parameter));
        } catch (JsonProcessingException e) {
            throw new SQLException("Failed to serialize tags to JSON", e);
        }
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parseJson(rs.getString(columnName));
    }

    // ... 其他 getNullableResult 重载方法类似

    private List<String> parseJson(String json) throws SQLException {
        if (json == null || json.isBlank()) return List.of();
        try {
            return MAPPER.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            throw new SQLException("Failed to deserialize tags from JSON", e);
        }
    }
}
```

---

## 4. 索引设计规范

### 4.1 索引规则

| 规则 | 说明 |
|------|------|
| 必须索引 | 所有 `WHERE` 条件中频繁出现的列 |
| 禁止冗余索引 | 联合索引 `(a, b)` 已覆盖对 `a` 的单列查询 |
| 选择性原则 | 低基数列（如 `status` 只有 3-5 个值）单独建索引效果有限，考虑与其他列组合 |
| 覆盖索引优先 | 高频查询尽量通过索引覆盖，避免回表 |

### 4.2 当前表索引清单

| 表 | 索引名 | 索引列 | 用途 |
|----|--------|--------|------|
| `video` | `idx_video_status` | `status` | 按状态筛选视频列表 |
| `video` | `idx_video_created_at` | `created_at` | 时间排序分页 |
| `upload_session` | `idx_upload_session_status` | `status` | 查询活跃上传会话 |
| `oauth_token` | `uk_oauth_token_platform` | `platform` (UNIQUE) | 确保每平台一条令牌 |
| `promotion_channel` | `idx_promotion_channel_status` | `status` | 筛选启用的推广渠道 |
| `api_key` | `idx_api_key_prefix` | `prefix` | 通过前缀快速定位 API Key |
| `video_metadata` | `idx_video_metadata_video_id` | `video_id` | 通过视频 ID 查元数据 |
| `publish_record` | `idx_publish_record_video_id` | `video_id` | 查询视频的发布记录 |
| `publish_record` | `idx_publish_record_status` | `status` | 按状态筛选发布记录 |
| `promotion_record` | `idx_promotion_record_video_id` | `video_id` | 查询视频的推广记录 |
| `promotion_record` | `idx_promotion_record_channel_id` | `channel_id` | 按渠道查推广记录 |
| `promotion_record` | `idx_promotion_record_status` | `status` | 按状态筛选推广记录 |
| `promotion_record` | `idx_promotion_record_created_at` | `created_at` | 按时间排序推广记录 |

### 4.3 慢查询监控

在阿里云 RDS 控制台开启慢查询日志：

```sql
-- RDS 控制台设置（或联系 DBA）
SET GLOBAL slow_query_log = 'ON';
SET GLOBAL long_query_time = 1;        -- 超过 1 秒视为慢查询
SET GLOBAL log_queries_not_using_indexes = 'ON';
```

应用层通过 MyBatis 拦截器记录慢 SQL：

```java
@Intercepts({
    @Signature(type = Executor.class, method = "query",
               args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
    @Signature(type = Executor.class, method = "update",
               args = {MappedStatement.class, Object.class})
})
@Component
public class SlowSqlInterceptor implements Interceptor {
    
    private static final Logger log = LoggerFactory.getLogger(SlowSqlInterceptor.class);
    private static final long SLOW_THRESHOLD_MS = 1000;  // 1 秒

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            return invocation.proceed();
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            if (elapsed > SLOW_THRESHOLD_MS) {
                MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
                log.warn("[SLOW-SQL] {}ms | mapper={} | sql-id={}",
                         elapsed, ms.getResource(), ms.getId());
            }
        }
    }
}
```

---

## 5. 数据表 ER 关系

> 说明：Grace 平台采用 DDD 架构，**不使用物理外键**。以下关系图用 `..>` 表示逻辑引用。

```
┌─────────────────────────────────────────────────────────────┐
│                     Video Context                           │
│  ┌──────────┐    ┌─────────────────┐                        │
│  │  video    │    │  upload_session  │                       │
│  │  (id) PK  │    │  (upload_id) PK │                       │
│  └──────────┘    └─────────────────┘                        │
│       │                                                     │
└───────┼─────────────────────────────────────────────────────┘
        │ video_id (逻辑引用)
        ├──────────────────────────────┐
        │                              │
┌───────▼──────────────┐   ┌───────────▼────────────────────┐
│  Metadata Context     │   │  Distribution Context          │
│  ┌────────────────┐   │   │  ┌────────────────┐            │
│  │ video_metadata  │   │   │  │ publish_record  │            │
│  │ (id) PK        │   │   │  │ (id) PK         │            │
│  │  video_id ──────┼───┘   │  │  video_id ──────┤            │
│  └────────────────┘   │   │  │  metadata_id    │            │
└───────────────────────┘   │  └────────────────┘            │
                            │  ┌─────────────┐               │
                            │  │ oauth_token  │               │
                            │  │ (id) PK      │               │
                            │  └─────────────┘               │
                            └────────────────────────────────┘
                                       │ video_id (逻辑引用)
        ┌──────────────────────────────┘
        │
┌───────▼──────────────────────────────────────────┐
│  Promotion Context                                │
│  ┌──────────────────┐   ┌───────────────────┐    │
│  │ promotion_channel │   │ promotion_record   │    │
│  │ (id) PK           │   │ (id) PK            │    │
│  └──────────────────┘   │  video_id           │    │
│                          │  channel_id ────────┤    │
│                          └───────────────────┘    │
└───────────────────────────────────────────────────┘

┌───────────────────────────────────────────────────┐
│  User & Settings Context                          │
│  ┌──────────────┐ ┌─────────────────────────┐     │
│  │ user_profile  │ │ notification_preference  │     │
│  │ (id) PK       │ │ (id) PK                  │     │
│  └──────────────┘ └─────────────────────────┘     │
│  ┌──────────┐                                     │
│  │ api_key   │                                     │
│  │ (id) PK   │                                     │
│  └──────────┘                                     │
└───────────────────────────────────────────────────┘
```

---

## 6. 数据表汇总

| # | 表名 | 限界上下文 | 聚合根 | 行数量级预估 |
|---|------|-----------|--------|-------------|
| 1 | `video` | Video | Video | 千级 |
| 2 | `upload_session` | Video | UploadSession | 千级（有 TTL 清理） |
| 3 | `video_metadata` | Metadata | VideoMetadata | 同 video 1:1 |
| 4 | `publish_record` | Distribution | PublishRecord | 千级（视频 * 平台） |
| 5 | `oauth_token` | Distribution | OAuthToken | 个位数（平台数） |
| 6 | `promotion_channel` | Promotion | PromotionChannel | 十级 |
| 7 | `promotion_record` | Promotion | PromotionRecord | 千级（视频 * 渠道） |
| 8 | `user_profile` | User & Settings | UserProfile | 1（MVP 单用户） |
| 9 | `notification_preference` | User & Settings | NotificationPreference | 1 |
| 10 | `api_key` | User & Settings | ApiKey | 个位数 |

---

## 7. 数据库版本管理

使用 **Flyway** 管理数据库 schema 变更：

### 7.1 Maven 依赖

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-mysql</artifactId>
</dependency>
```

### 7.2 迁移文件结构

```
src/main/resources/
└── db/
    └── migration/
        ├── V1__create_video_tables.sql
        ├── V2__create_metadata_tables.sql
        ├── V3__create_distribution_tables.sql
        ├── V4__create_promotion_tables.sql
        ├── V5__create_user_settings_tables.sql
        └── V6__add_indexes.sql
```

### 7.3 Flyway 配置

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    baseline-version: 0
    validate-on-migrate: true
    encoding: UTF-8
```

### 7.4 迁移命名规范

| 类型 | 格式 | 示例 |
|------|------|------|
| 版本迁移 | `V{版本号}__{描述}.sql` | `V1__create_video_tables.sql` |
| 可重复迁移 | `R__{描述}.sql` | `R__create_views.sql` |

> **注意**：版本号用单调递增整数，描述用 snake_case，双下划线 `__` 分隔。

---

## 8. 安全规范

### 8.1 凭证管理

| 级别 | 场景 | 处理方式 |
|------|------|---------|
| 开发环境 | 本地连接 RDS | `application-dev.yml`（加入 `.gitignore`） |
| CI/CD | 测试流水线 | 通过 CI 环境变量（如 GitHub Secrets）注入 |
| 生产环境 | 线上服务 | 通过环境变量或 K8s Secret 注入 |

`.gitignore` 中必须包含：

```
application-dev.yml
application-local.yml
*.secret
```

### 8.2 敏感数据加密

| 数据 | 加密方式 | 存储形式 |
|------|---------|---------|
| 数据库密码 | 环境变量注入 | 不存储在代码仓库中 |
| OAuth Access/Refresh Token | AES-256-GCM | `TEXT` 列存密文 |
| Promotion Channel API Key | AES-256-GCM | `TEXT` 列存密文 |
| 用户 API Key | BCrypt 单向哈希 | `VARCHAR(255)` 列存哈希值 |

### 8.3 RDS 安全组配置建议

```
+----------------------------------------------+
| 阿里云 RDS 安全组规则                          |
| 1. 仅允许 ECS/容器服务内网 IP 段访问           |
| 2. 开发环境通过 VPN 或白名单访问               |
| 3. 禁止公网 0.0.0.0/0 直连                    |
| 4. 定期审计白名单中的 IP 条目                  |
+----------------------------------------------+
```

---

## 9. 全量 DDL 索引

完整的建表 DDL 和执行顺序参见 [09-infrastructure-config.md 第 A2 节](./09-infrastructure-config.md#a2-全量-ddl-汇总)。

建表执行顺序：

1. **Phase 1（无依赖）**：`video` → `upload_session` → `oauth_token` → `promotion_channel` → `user_profile` → `notification_preference` → `api_key`
2. **Phase 2（逻辑依赖）**：`video_metadata` → `publish_record` → `promotion_record`
