# Phase 6: User & Settings 限界上下文

> 参考实施路线图：`docs/backend/00-index.md` §6 Phase 4
> 产出：User/Settings 上下文全部领域模型、API Key 生成、跨上下文 ACL、持久层、测试

## 进度统计

- [x] 共 10 个任务，已完成 10/10

---

## 任务列表

### P6-01: 创建 User/Settings 域实体

- **参考文档**: `docs/backend/07-context-user-settings.md` §B1（UserProfile 代码 + createDefault/updateProfile/updateAvatar 方法）、§B2（NotificationPreference 代码 + createDefault/update 方法）、§B3（ApiKey 代码 + create/recordUsage/isExpired 方法）
- **产出文件**:
  - `grace-platform/src/main/java/com/grace/platform/usersettings/domain/model/UserProfile.java`
  - `grace-platform/src/main/java/com/grace/platform/usersettings/domain/model/NotificationPreference.java`
  - `grace-platform/src/main/java/com/grace/platform/usersettings/domain/model/ApiKey.java`
- **验证命令**: `mvn clean compile`
- **依赖**: P1-04, P1-12
- **状态**: [x]
- **注意**: MVP 单用户模式，UserProfile 和 NotificationPreference 使用固定 ID（"default-user"、"default-notification"）；ApiKey.hashedKey 为 BCrypt 哈希不可逆

---

### P6-02: 创建 ApiKeyGenerationService

- **参考文档**: `docs/backend/07-context-user-settings.md` §C1（ApiKeyGenerationService 接口 + GeneratedApiKey record）、§C2（ApiKeyGenerationServiceImpl 完整代码含 SecureRandom + Base62 + BCrypt）
- **产出文件**:
  - `grace-platform/src/main/java/com/grace/platform/usersettings/domain/service/ApiKeyGenerationService.java`
  - `grace-platform/src/main/java/com/grace/platform/usersettings/domain/service/GeneratedApiKey.java`
  - `grace-platform/src/main/java/com/grace/platform/usersettings/domain/service/ApiKeyGenerationServiceImpl.java`
- **验证命令**: `mvn clean compile`
- **依赖**: P6-01, P1-12
- **状态**: [x]
- **注意**: rawKey = "grc_" + Base62(32字节)；prefix = rawKey前8位 + "..." + 后4位；hashedKey = BCrypt(rawKey)

---

### P6-03: 创建 User/Settings Repository 接口

- **参考文档**: `docs/backend/07-context-user-settings.md` §E（UserProfileRepository/NotificationPreferenceRepository/ApiKeyRepository 接口代码）
- **产出文件**:
  - `grace-platform/src/main/java/com/grace/platform/usersettings/domain/repository/UserProfileRepository.java`
  - `grace-platform/src/main/java/com/grace/platform/usersettings/domain/repository/NotificationPreferenceRepository.java`
  - `grace-platform/src/main/java/com/grace/platform/usersettings/domain/repository/ApiKeyRepository.java`
- **验证命令**: `mvn clean compile`
- **依赖**: P6-01
- **状态**: [x]

---

### P6-04: 创建 UserSettingsApplicationService

- **参考文档**: `docs/backend/07-context-user-settings.md` §F（UserSettingsApplicationService 完整代码含 10 个方法 G1-G10 的实现）
- **产出文件**:
  - `grace-platform/src/main/java/com/grace/platform/usersettings/application/UserSettingsApplicationService.java`
  - `grace-platform/src/main/java/com/grace/platform/usersettings/application/dto/ProfileResponse.java`
  - `grace-platform/src/main/java/com/grace/platform/usersettings/application/dto/UpdateProfileRequest.java`
  - `grace-platform/src/main/java/com/grace/platform/usersettings/application/dto/NotificationPreferenceResponse.java`
  - `grace-platform/src/main/java/com/grace/platform/usersettings/application/dto/UpdateNotificationRequest.java`
  - `grace-platform/src/main/java/com/grace/platform/usersettings/application/dto/CreateApiKeyRequest.java`
  - `grace-platform/src/main/java/com/grace/platform/usersettings/application/dto/ApiKeyCreatedResponse.java`
  - `grace-platform/src/main/java/com/grace/platform/usersettings/application/dto/ApiKeyResponse.java`
  - `grace-platform/src/main/java/com/grace/platform/usersettings/application/dto/ConnectedAccountResponse.java`
- **验证命令**: `mvn clean compile`
- **依赖**: P6-02, P6-03, P1-07, P1-08
- **状态**: [x]

---

### P6-05: 创建 ConnectedAccountQueryService（跨上下文 ACL）

- **参考文档**: `docs/backend/07-context-user-settings.md` §H1（ConnectedAccountQueryService 完整代码 + KNOWN_PLATFORMS 列表 + queryConnectedAccounts/disconnectPlatform 逻辑）
- **产出文件**:
  - `grace-platform/src/main/java/com/grace/platform/usersettings/infrastructure/acl/ConnectedAccountQueryService.java`
- **验证命令**: `mvn clean compile`
- **依赖**: P4-04, P6-04
- **状态**: [x]
- **注意**: 只读查询 Distribution 上下文的 OAuthTokenRepository；KNOWN_PLATFORMS 含 youtube/weibo/bilibili

---

### P6-06: 创建 DefaultUserInitializer

- **参考文档**: `docs/backend/07-context-user-settings.md` §H4（DefaultUserInitializer 代码：ApplicationRunner 实现，启动时创建默认用户和通知偏好）
- **产出文件**:
  - `grace-platform/src/main/java/com/grace/platform/usersettings/infrastructure/DefaultUserInitializer.java`
- **验证命令**: `mvn clean compile`
- **依赖**: P6-03
- **状态**: [x]

---

### P6-07: 创建 SettingsController

- **参考文档**: `docs/backend/07-context-user-settings.md` §G（G1-G10 端点映射表 + 请求/响应 DTO 字段表）；`api.md` §G
- **产出文件**:
  - `grace-platform/src/main/java/com/grace/platform/usersettings/interfaces/rest/SettingsController.java`
- **验证命令**: `mvn clean compile`
- **依赖**: P6-04, P1-07, P1-09
- **状态**: [x]

---

### P6-08: 创建 User/Settings MyBatis Mapper + Repository 实现

- **参考文档**: `docs/backend/07-context-user-settings.md` §H3（UserProfileMapper/NotificationPreferenceMapper/ApiKeyMapper 接口代码 + 数据库列映射表）
- **产出文件**:
  - `grace-platform/src/main/java/com/grace/platform/usersettings/infrastructure/persistence/UserProfileMapper.java`
  - `grace-platform/src/main/java/com/grace/platform/usersettings/infrastructure/persistence/NotificationPreferenceMapper.java`
  - `grace-platform/src/main/java/com/grace/platform/usersettings/infrastructure/persistence/ApiKeyMapper.java`
  - `grace-platform/src/main/java/com/grace/platform/usersettings/infrastructure/persistence/UserProfileRepositoryImpl.java`
  - `grace-platform/src/main/java/com/grace/platform/usersettings/infrastructure/persistence/NotificationPreferenceRepositoryImpl.java`
  - `grace-platform/src/main/java/com/grace/platform/usersettings/infrastructure/persistence/ApiKeyRepositoryImpl.java`
  - `grace-platform/src/main/resources/mapper/usersettings/UserProfileMapper.xml`
  - `grace-platform/src/main/resources/mapper/usersettings/NotificationPreferenceMapper.xml`
  - `grace-platform/src/main/resources/mapper/usersettings/ApiKeyMapper.xml`
- **验证命令**: `mvn clean compile`
- **依赖**: P6-01, P1-05
- **状态**: [x]

---

### P6-09: 创建 User/Settings 配置类

- **参考文档**: `docs/backend/09-infrastructure-config.md`（Storage 配置项：avatar-dir/avatar-max-size/avatar-allowed-types）
- **产出文件**:
  - `grace-platform/src/main/java/com/grace/platform/usersettings/infrastructure/config/StorageProperties.java`
- **验证命令**: `mvn clean compile`
- **依赖**: P1-02
- **状态**: [x]

---

### P6-10: 创建 User/Settings 单元测试

- **参考文档**: `docs/backend/10-testing-strategy.md` §C（单元测试：头像文件校验、API Key 明文仅返回一次）
- **产出文件**:
  - `grace-platform/src/test/java/com/grace/platform/usersettings/UserSettingsUnitTest.java`
- **验证命令**: `mvn test -Dtest="UserSettingsUnitTest"`
- **依赖**: P6-01, P6-02, P1-16
- **状态**: [x]
- **测试要点**: 非 JPG/PNG 文件拒绝上传；超 2MB 文件拒绝；ApiKey rawKey 仅在 GeneratedApiKey 中出现，ApiKey 实体不含明文；BCrypt 哈希后无法还原
