## 🔴 Flyway 数据库迁移被禁用 - 违反架构规范

**严重程度:** CRITICAL  
**类别:** infrastructure  
**发现时间:** 2026-03-28  
**发现者:** Kelly (QA Lead)  
**影响范围:** 数据库 schema 管理、环境一致性、团队协作  

### 问题描述

Flyway 数据库迁移功能被禁用（flyway.enabled=false），需要手动管理数据库 schema。这违反了项目设计文档中的约定，带来严重的环境一致性和版本控制风险。

### 复现步骤

1. 打开 grace-platform/src/main/resources/application.yml
2. 搜索 flyway.enabled 配置项
3. 发现配置为：flyway.enabled=false

### 预期行为

根据 docs/backend/09-infrastructure-config.md 和 AGENTS.md 的约定：

- Flyway 应该启用并自动执行数据库迁移脚本
- 所有数据库变更必须通过 Flyway migration 脚本（V{N}__{description}.sql）
- 禁止直接使用 DDL 或手动修改表结构

### 实际行为

Flyway 被禁用，需要手动维护表结构。

### 风险评估

**高风险：**

1. **环境不一致**
   - 开发、测试、生产环境的 schema 可能出现差异
   - 难以追踪 schema 变更历史

2. **团队协作困难**
   - 多个开发者修改 schema 时容易冲突
   - 新成员搭建环境时需要手动执行 SQL

3. **部署风险**
   - 无法保证部署时 schema 与代码匹配
   - 回滚困难

4. **违反架构规范**
   - 违反 AGENTS.md 中"Never Do"第 5 条：忘记使用 Flyway
   - 违反 docs/backend/09-infrastructure-config.md 的约定

### 可能原因

1. **迁移脚本缺失**
   - 尚未创建初始 migration 脚本
   - migration 脚本有语法错误

2. **配置问题**
   - Flyway 配置不完整
   - 数据库连接信息错误

3. **历史遗留问题**
   - 开发初期为快速原型临时禁用
   - 忘记重新启用

### 建议修复步骤

#### 1. 创建 Flyway 迁移目录

```bash
mkdir -p grace-platform/src/main/resources/db/migration
```

#### 2. 创建初始迁移脚本

根据 docs/backend/db-design.md 创建 V1__initial_schema.sql：

```sql
-- V1__initial_schema.sql
-- Grace Platform 初始数据库 schema

-- 视频表
CREATE TABLE IF NOT EXISTS video (
    id VARCHAR(36) PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    format VARCHAR(10) NOT NULL,
    duration_seconds BIGINT,
    file_path VARCHAR(512) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 上传会话表
CREATE TABLE IF NOT EXISTS upload_session (
    upload_id VARCHAR(36) PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    format VARCHAR(10) NOT NULL,
    total_chunks INT NOT NULL,
    uploaded_chunks INT NOT NULL DEFAULT 0,
    temp_directory VARCHAR(512),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 视频元数据表
CREATE TABLE IF NOT EXISTS video_metadata (
    id VARCHAR(36) PRIMARY KEY,
    video_id VARCHAR(36) NOT NULL,
    title VARCHAR(100) NOT NULL,
    description TEXT,
    tags_json JSON,
    source VARCHAR(50) NOT NULL DEFAULT 'AI_GENERATED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (video_id) REFERENCES video(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 发布记录表
CREATE TABLE IF NOT EXISTS publish_record (
    id VARCHAR(36) PRIMARY KEY,
    video_id VARCHAR(36) NOT NULL,
    metadata_id VARCHAR(36),
    platform VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    video_url VARCHAR(512),
    upload_task_id VARCHAR(100),
    progress_percent INT DEFAULT 0,
    error_message TEXT,
    published_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (video_id) REFERENCES video(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- OAuth 令牌表
CREATE TABLE IF NOT EXISTS oauth_token (
    id VARCHAR(36) PRIMARY KEY,
    platform VARCHAR(50) NOT NULL UNIQUE,
    encrypted_access_token TEXT NOT NULL,
    encrypted_refresh_token TEXT,
    expires_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 推广渠道表
CREATE TABLE IF NOT EXISTS promotion_channel (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(50) NOT NULL,
    channel_url VARCHAR(512) NOT NULL,
    encrypted_api_key TEXT,
    priority INT DEFAULT 1,
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 推广记录表
CREATE TABLE IF NOT EXISTS promotion_record (
    id VARCHAR(36) PRIMARY KEY,
    video_id VARCHAR(36) NOT NULL,
    channel_id VARCHAR(36) NOT NULL,
    promotion_copy TEXT,
    method VARCHAR(20) NOT NULL,
    status VARCHAR(50) NOT NULL,
    result_url VARCHAR(512),
    error_message TEXT,
    executed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (video_id) REFERENCES video(id) ON DELETE CASCADE,
    FOREIGN KEY (channel_id) REFERENCES promotion_channel(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 用户资料表
CREATE TABLE IF NOT EXISTS user_profile (
    id VARCHAR(36) PRIMARY KEY,
    display_name VARCHAR(100),
    email VARCHAR(255),
    avatar_url VARCHAR(512),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 通知偏好表
CREATE TABLE IF NOT EXISTS notification_preference (
    id VARCHAR(36) PRIMARY KEY,
    upload_complete BOOLEAN DEFAULT TRUE,
    promotion_success BOOLEAN DEFAULT TRUE,
    system_updates BOOLEAN DEFAULT FALSE,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- API Key 表
CREATE TABLE IF NOT EXISTS api_key (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    hashed_key VARCHAR(255) NOT NULL,
    prefix VARCHAR(50) NOT NULL,
    expires_at TIMESTAMP,
    last_used_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_prefix (prefix)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 创建索引
CREATE INDEX idx_video_status ON video(status);
CREATE INDEX idx_video_created_at ON video(created_at);
CREATE INDEX idx_metadata_video_id ON video_metadata(video_id);
CREATE INDEX idx_publish_video_id ON publish_record(video_id);
CREATE INDEX idx_publish_platform ON publish_record(platform);
CREATE INDEX idx_promotion_video_id ON promotion_record(video_id);
CREATE INDEX idx_promotion_channel_id ON promotion_record(channel_id);
```

#### 3. 启用 Flyway

在 application.yml 中修改配置：

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: false
    validate-on-migrate: true
```

#### 4. 验证配置

```bash
cd grace-platform
mvn flyway:migrate
```

#### 5. 提交迁移脚本

```bash
git add src/main/resources/db/migration/V1__initial_schema.sql
git add src/main/resources/application.yml
git commit -m "feat(shared): 启用 Flyway 数据库迁移并创建初始 schema"
```

### 验收标准

- [ ] Flyway 配置启用（flyway.enabled=true）
- [ ] 创建初始迁移脚本 V1__initial_schema.sql
- [ ] 迁移脚本包含所有必要的表（video, upload_session, video_metadata, publish_record, oauth_token, promotion_channel, promotion_record, user_profile, notification_preference, api_key）
- [ ] 迁移脚本成功执行（mvn flyway:migrate）
- [ ] 数据库表结构与 docs/backend/db-design.md 一致
- [ ] 更新 docs/backend/09-infrastructure-config.md 确认 Flyway 配置

### 关联文档

- [docs/backend/09-infrastructure-config.md](../docs/backend/09-infrastructure-config.md) - 基础设施配置
- [docs/backend/db-design.md](../docs/backend/db-design.md) - 数据库设计
- [AGENTS.md](../AGENTS.md) - Git Workflow（数据库变更必须通过 Flyway）
- [api.md](../api.md) - API 契约（依赖正确的数据库结构）

### 参考资料

- [Flyway 官方文档](https://flywaydb.org/documentation/)
- [Spring Boot + Flyway 集成](https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.data-initialization.migration-tool.flyway)

---

*由 Kelly (QA Lead) 在回归测试中发现*
