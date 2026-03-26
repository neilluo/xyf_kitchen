# 测试策略（Testing Strategy）

> 依赖文档：[01-project-scaffolding.md](./01-project-scaffolding.md)、[02-shared-kernel.md](./02-shared-kernel.md)
> 属性定义：design.md §Correctness Properties
> 需求映射：所有需求的验收标准

---

## A. 测试框架与依赖

| 类型 | 框架 | 用途 |
|------|------|------|
| 单元测试 | JUnit 5 | 边界条件、错误处理、具体示例 |
| 属性测试 | jqwik 1.8.x | 12 个 Correctness Properties，每个至少 100 次迭代 |
| Mock | Mockito 5.x | 隔离外部依赖（LLM、YouTube API、OpenCrawl） |
| 集成测试 | Spring Boot Test + Testcontainers | MySQL 容器化集成测试 |
| 断言 | AssertJ | 流式断言 |

**Maven 依赖（已在 01-project-scaffolding.md 中声明）：**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>net.jqwik</groupId>
    <artifactId>jqwik</artifactId>
    <version>1.8.5</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>mysql</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

---

## B. 测试目录结构

```
src/test/java/com/grace/platform/
├── video/
│   ├── domain/
│   │   ├── VideoPropertyTest.java          # Property 1, 2, 3
│   │   └── VideoUnitTest.java              # 边界条件
│   └── integration/
│       └── VideoIntegrationTest.java
├── metadata/
│   ├── domain/
│   │   ├── MetadataPropertyTest.java       # Property 4, 5
│   │   └── MetadataUnitTest.java
│   └── integration/
│       └── MetadataIntegrationTest.java
├── distribution/
│   ├── domain/
│   │   ├── DistributionPropertyTest.java   # Property 6, 7, 8
│   │   └── DistributionUnitTest.java
│   └── integration/
│       └── DistributionIntegrationTest.java
├── promotion/
│   ├── domain/
│   │   ├── PromotionPropertyTest.java      # Property 9, 10, 11, 12
│   │   └── PromotionUnitTest.java
│   └── integration/
│       └── PromotionIntegrationTest.java
├── usersettings/
│   ├── domain/
│   │   └── UserSettingsUnitTest.java
│   └── integration/
│       └── UserSettingsIntegrationTest.java
├── dashboard/
│   └── integration/
│       └── DashboardIntegrationTest.java
├── shared/
│   └── EncryptionServiceTest.java
└── testutil/
    ├── TestFixtures.java                   # 共享测试数据工厂
    ├── AbstractIntegrationTest.java        # Testcontainers 基类
    └── Arbitraries.java                    # jqwik 自定义生成器
```

---

## C. 属性测试（Property-Based Testing）

### C1. 属性测试规范

每个属性测试遵循以下规范：

```java
@Property(tries = 100)
@Label("Feature: video-distribution-platform, Property {N}: {property_text}")
void property_{N}_description(@ForAll ... args) {
    // Given: 使用 jqwik Arbitrary 生成随机有效输入
    // When: 执行被测行为
    // Then: 断言属性成立
}
```

**标签格式：** `Feature: video-distribution-platform, Property {number}: {property_text}`

### C2. Property 1：视频文件信息提取正确性

**所在文件：** `VideoPropertyTest.java`
**验证需求：** Requirements 1.1

```java
@Property(tries = 100)
@Label("Feature: video-distribution-platform, Property 1: 视频文件信息提取正确性")
void property_1_videoFileInfoExtraction(
        @ForAll("validVideoFiles") VideoFileFixture fixture) {
    // Given: 一个有效的视频文件（格式 MP4/MOV/AVI/MKV，大小 ≤ 5GB）
    // When: VideoFileInspector 提取文件信息
    VideoFileInfo info = videoFileInspector.inspect(fixture.path());
    
    // Then: 提取的信息与实际文件属性一致
    assertThat(info.fileName()).isEqualTo(fixture.expectedFileName());
    assertThat(info.fileSize()).isEqualTo(fixture.expectedFileSize());
    assertThat(info.format()).isEqualTo(fixture.expectedFormat());
    assertThat(info.durationSeconds()).isEqualTo(fixture.expectedDuration());
}
```

**自定义 Arbitrary：**

```java
@Provide
Arbitrary<VideoFileFixture> validVideoFiles() {
    Arbitrary<VideoFormat> formats = Arbitraries.of(VideoFormat.values());
    Arbitrary<Long> sizes = Arbitraries.longs().between(1, 5L * 1024 * 1024 * 1024);
    Arbitrary<Long> durations = Arbitraries.longs().between(1, 86400);
    Arbitrary<String> names = Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(100);
    
    return Combinators.combine(names, sizes, formats, durations)
        .as(VideoFileFixture::new);
}
```

### C3. Property 2：视频格式验证边界

**所在文件：** `VideoPropertyTest.java`
**验证需求：** Requirements 1.3, 1.5

```java
@Property(tries = 100)
@Label("Feature: video-distribution-platform, Property 2: 视频格式验证边界")
void property_2_videoFormatValidation(
        @ForAll("anyFileExtension") String extension) {
    boolean isSupported = Set.of("MP4", "MOV", "AVI", "MKV")
        .contains(extension.toUpperCase());
    
    if (isSupported) {
        // Then: 应接受该文件
        assertThatCode(() -> Video.validateFormat(extension))
            .doesNotThrowAnyException();
    } else {
        // Then: 应拒绝并返回包含支持格式列表的错误信息
        assertThatThrownBy(() -> Video.validateFormat(extension))
            .isInstanceOf(UnsupportedVideoFormatException.class)
            .hasMessageContaining("MP4")
            .hasMessageContaining("MOV")
            .hasMessageContaining("AVI")
            .hasMessageContaining("MKV");
    }
}
```

### C4. Property 3：视频信息持久化往返

**所在文件：** `VideoPropertyTest.java`（集成测试变体在 `VideoIntegrationTest.java`）
**验证需求：** Requirements 1.7

```java
@Property(tries = 100)
@Label("Feature: video-distribution-platform, Property 3: 视频信息持久化往返")
void property_3_videoRoundTrip(@ForAll("validVideos") Video video) {
    // When: 保存后通过 ID 查询
    videoRepository.save(video);
    Optional<Video> found = videoRepository.findById(video.getId());
    
    // Then: 返回的 Video 应与原始实体在所有字段上相等
    assertThat(found).isPresent();
    assertThat(found.get()).usingRecursiveComparison().isEqualTo(video);
}
```

### C5. Property 4：元数据字段约束不变量

**所在文件：** `MetadataPropertyTest.java`
**验证需求：** Requirements 2.2, 2.3, 2.4

```java
@Property(tries = 100)
@Label("Feature: video-distribution-platform, Property 4: 元数据字段约束不变量")
void property_4_metadataConstraints(
        @ForAll("generatedMetadata") VideoMetadata metadata) {
    // Then: 以下约束必须同时成立
    assertThat(metadata.getTitle()).isNotBlank();
    assertThat(metadata.getTitle().length()).isLessThanOrEqualTo(100);
    assertThat(metadata.getDescription().length()).isLessThanOrEqualTo(5000);
    assertThat(metadata.getTags()).hasSizeGreaterThanOrEqualTo(5);
    assertThat(metadata.getTags()).hasSizeLessThanOrEqualTo(15);
}
```

### C6. Property 5：元数据编辑往返

**所在文件：** `MetadataPropertyTest.java`
**验证需求：** Requirements 3.2, 3.3

```java
@Property(tries = 100)
@Label("Feature: video-distribution-platform, Property 5: 元数据编辑往返")
void property_5_metadataEditRoundTrip(
        @ForAll("existingMetadata") VideoMetadata original,
        @ForAll("validUpdates") MetadataUpdate update) {
    // When: 执行更新
    original.update(update.title(), update.description(), update.tags());
    
    // Then: 更新后查询应反映所有更新内容
    if (update.title() != null) {
        assertThat(original.getTitle()).isEqualTo(update.title());
    }
    if (update.description() != null) {
        assertThat(original.getDescription()).isEqualTo(update.description());
    }
    if (update.tags() != null) {
        assertThat(original.getTags()).isEqualTo(update.tags());
    }
}
```

### C7. Property 6：平台路由正确性

**所在文件：** `DistributionPropertyTest.java`
**验证需求：** Requirements 4.1

```java
@Property(tries = 100)
@Label("Feature: video-distribution-platform, Property 6: 平台路由正确性")
void property_6_platformRouting(
        @ForAll("platformIdentifiers") String platformId) {
    if (registeredPlatforms.contains(platformId)) {
        // Then: 应返回对应的 VideoDistributor 实现
        VideoDistributor distributor = registry.getDistributor(platformId);
        assertThat(distributor.platform()).isEqualTo(platformId);
    } else {
        // Then: 应抛出 UnsupportedPlatformException
        assertThatThrownBy(() -> registry.getDistributor(platformId))
            .isInstanceOf(UnsupportedPlatformException.class);
    }
}
```

### C8. Property 7：发布结果包含有效视频 URL

**所在文件：** `DistributionPropertyTest.java`
**验证需求：** Requirements 4.3

```java
@Property(tries = 100)
@Label("Feature: video-distribution-platform, Property 7: 发布结果包含有效视频 URL")
void property_7_publishResultContainsValidUrl(
        @ForAll("successfulPublishRecords") PublishRecord record) {
    // Then: videoUrl 应为非空的有效 URL 格式
    assertThat(record.getVideoUrl()).isNotBlank();
    assertThatCode(() -> new URL(record.getVideoUrl())).doesNotThrowAnyException();
    
    // 当平台为 YouTube 时，URL 应匹配特定前缀
    if ("youtube".equals(record.getPlatform())) {
        assertThat(record.getVideoUrl()).satisfiesAnyOf(
            url -> assertThat(url).startsWith("https://www.youtube.com/watch?v="),
            url -> assertThat(url).startsWith("https://youtu.be/")
        );
    }
}
```

### C9. Property 8：发布记录持久化往返

**所在文件：** `DistributionPropertyTest.java`
**验证需求：** Requirements 4.7

```java
@Property(tries = 100)
@Label("Feature: video-distribution-platform, Property 8: 发布记录持久化往返")
void property_8_publishRecordRoundTrip(
        @ForAll("validPublishRecords") PublishRecord record) {
    // When: 保存后通过 videoId 查询
    publishRecordRepository.save(record);
    List<PublishRecord> records = publishRecordRepository.findByVideoId(record.getVideoId());
    
    // Then: 返回的记录列表应包含该记录
    assertThat(records).anySatisfy(found ->
        assertThat(found).usingRecursiveComparison().isEqualTo(record)
    );
}
```

### C10. Property 9：推广渠道 CRUD 往返

**所在文件：** `PromotionPropertyTest.java`
**验证需求：** Requirements 5.1

```java
@Property(tries = 100)
@Label("Feature: video-distribution-platform, Property 9: 推广渠道 CRUD 往返")
void property_9_channelCrudRoundTrip(
        @ForAll("validChannels") PromotionChannel channel) {
    // Create: 保存后查询应返回相同数据
    promotionChannelRepository.save(channel);
    Optional<PromotionChannel> found = promotionChannelRepository.findById(channel.getId());
    assertThat(found).isPresent();
    assertThat(found.get()).usingRecursiveComparison().isEqualTo(channel);
    
    // Update: 更新后查询应反映更新
    channel.updateName("Updated-" + channel.getName());
    promotionChannelRepository.save(channel);
    found = promotionChannelRepository.findById(channel.getId());
    assertThat(found.get().getName()).startsWith("Updated-");
    
    // Delete: 删除后查询应返回空
    promotionChannelRepository.deleteById(channel.getId());
    found = promotionChannelRepository.findById(channel.getId());
    assertThat(found).isEmpty();
}
```

### C11. Property 10：API Key 加密存储

**所在文件：** `PromotionPropertyTest.java`
**验证需求：** Requirements 5.3

```java
@Property(tries = 100)
@Label("Feature: video-distribution-platform, Property 10: API Key 加密存储")
void property_10_apiKeyEncryptedStorage(
        @ForAll("rawApiKeys") String rawApiKey) {
    // When: 加密后持久化
    String encrypted = encryptionService.encrypt(rawApiKey);
    
    // Then: 加密值不等于明文
    assertThat(encrypted).isNotEqualTo(rawApiKey);
    
    // And: 解密后应能还原
    String decrypted = encryptionService.decrypt(encrypted);
    assertThat(decrypted).isEqualTo(rawApiKey);
}
```

### C12. Property 11：推广文案结构不变量

**所在文件：** `PromotionPropertyTest.java`
**验证需求：** Requirements 6.1, 6.3, 6.4

```java
@Property(tries = 100)
@Label("Feature: video-distribution-platform, Property 11: 推广文案结构不变量")
void property_11_promotionCopyStructure(
        @ForAll("generatedPromotionCopies") PromotionCopy copy) {
    // Then: 文案内容必须包含视频发布链接
    assertThat(copy.content()).containsPattern("https?://");
    
    // And: 必须包含推荐的推广方式
    assertThat(copy.recommendedMethod()).isIn(
        PromotionMethod.POST, PromotionMethod.COMMENT, PromotionMethod.SHARE);
    
    // And: 必须包含推荐理由
    assertThat(copy.methodReason()).isNotBlank();
}
```

### C13. Property 12：推广记录持久化往返

**所在文件：** `PromotionPropertyTest.java`
**验证需求：** Requirements 7.3, 7.5

```java
@Property(tries = 100)
@Label("Feature: video-distribution-platform, Property 12: 推广记录持久化往返")
void property_12_promotionRecordRoundTrip(
        @ForAll("validPromotionRecords") PromotionRecord record) {
    // When: 保存后通过 videoId 查询
    promotionRecordRepository.save(record);
    List<PromotionRecord> records = promotionRecordRepository.findByVideoId(record.getVideoId());
    
    // Then: 返回的记录列表应包含该记录
    assertThat(records).anySatisfy(found ->
        assertThat(found).usingRecursiveComparison().isEqualTo(record)
    );
}
```

---

## D. 单元测试

### D1. 单元测试覆盖矩阵

| 上下文 | 测试文件 | 测试场景 | 关联需求 |
|-------|---------|---------|---------|
| Video | `VideoUnitTest` | 不支持的视频格式返回正确错误信息 | 1.3 |
| Video | `VideoUnitTest` | 文件大小恰好等于 5GB 的边界情况 | 1.4 |
| Video | `VideoUnitTest` | 分片索引越界检查 | 1.6 |
| Video | `VideoUnitTest` | 重复分片上传检测 | 1.6 |
| Metadata | `MetadataUnitTest` | LLM 服务调用失败时的降级处理 | 2.6 |
| Metadata | `MetadataUnitTest` | 已确认元数据不可再编辑 | 3.5 |
| Metadata | `MetadataUnitTest` | 元数据标题超长截断/拒绝 | 2.4 |
| Distribution | `DistributionUnitTest` | 不支持的平台标识返回 UnsupportedPlatformException | 4.1 |
| Distribution | `DistributionUnitTest` | 平台 API 各类错误码的处理 | 4.5 |
| Distribution | `DistributionUnitTest` | OAuth token 过期时自动刷新 | 4.2 |
| Distribution | `DistributionUnitTest` | 配额超限后标记 QUOTA_EXCEEDED | 4.5 |
| Promotion | `PromotionUnitTest` | 推广渠道必填字段缺失校验 | 5.2 |
| Promotion | `PromotionUnitTest` | API Key 验证失败阻止保存 | 5.5 |
| Promotion | `PromotionUnitTest` | OpenCrawl 执行失败的错误记录 | 7.4 |
| Promotion | `PromotionUnitTest` | 单渠道失败不中断批量推广 | 7.5 |
| Promotion | `PromotionUnitTest` | 禁用渠道被跳过 | 5.4 |
| User | `UserSettingsUnitTest` | 头像文件类型校验（仅 JPG/PNG） | 10.2 |
| User | `UserSettingsUnitTest` | 头像文件大小校验（≤ 2MB） | 10.2 |
| User | `UserSettingsUnitTest` | API Key 创建后明文仅返回一次 | 10.6 |
| Shared | `EncryptionServiceTest` | AES-256-GCM 加解密往返 | 5.6 |
| Shared | `EncryptionServiceTest` | 不同明文产生不同密文（IV 随机性） | 5.6 |

### D2. 单元测试示例

```java
class VideoUnitTest {
    
    @Test
    @DisplayName("不支持的视频格式应返回包含支持格式列表的错误信息")
    void shouldRejectUnsupportedFormat() {
        assertThatThrownBy(() -> Video.validateFormat("WMV"))
            .isInstanceOf(UnsupportedVideoFormatException.class)
            .hasMessageContaining("WMV")
            .hasMessageContaining("MP4, MOV, AVI, MKV");
    }
    
    @Test
    @DisplayName("文件大小恰好等于 5GB 应被接受")
    void shouldAcceptExactly5GB() {
        long fiveGB = 5L * 1024 * 1024 * 1024;
        assertThatCode(() -> Video.validateFileSize(fiveGB))
            .doesNotThrowAnyException();
    }
    
    @Test
    @DisplayName("文件大小超过 5GB 应被拒绝")
    void shouldRejectOver5GB() {
        long overFiveGB = 5L * 1024 * 1024 * 1024 + 1;
        assertThatThrownBy(() -> Video.validateFileSize(overFiveGB))
            .isInstanceOf(VideoFileSizeExceededException.class);
    }
}
```

```java
class PromotionUnitTest {
    
    @Test
    @DisplayName("单渠道推广失败不应中断批量推广流程")
    void singleChannelFailureShouldNotInterruptBatch() {
        // Given: 3 个渠道，第 2 个会失败
        PromotionExecutor failingExecutor = mock(PromotionExecutor.class);
        when(failingExecutor.execute(any())).thenThrow(new OpenCrawlExecutionException("error"));
        
        // When: 批量执行
        List<PromotionRecord> results = promotionApplicationService.executeBatch(channels, copy);
        
        // Then: 3 个记录均有结果（1 个 FAILED，2 个 COMPLETED）
        assertThat(results).hasSize(3);
        assertThat(results).filteredOn(r -> r.getStatus() == PromotionStatus.FAILED).hasSize(1);
        assertThat(results).filteredOn(r -> r.getStatus() == PromotionStatus.COMPLETED).hasSize(2);
    }
}
```

---

## E. 集成测试

### E1. Testcontainers 基类

```java
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {
    
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("grace_test")
        .withUsername("test")
        .withPassword("test")
        .withInitScript("schema.sql"); // 使用 09-infrastructure-config.md 中的全量 DDL
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }
}
```

**test/resources/application-test.yml：**

```yaml
mybatis:
  mapper-locations: classpath:mapper/**/*.xml
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl  # 测试时打印 SQL

grace:
  encryption:
    key: "dGVzdC1lbmNyeXB0aW9uLWtleS0xMjM0NTY3OA=="  # 测试用 AES 密钥
  storage:
    video-dir: ${java.io.tmpdir}/grace-test/videos
    temp-dir: ${java.io.tmpdir}/grace-test/temp
    avatar-dir: ${java.io.tmpdir}/grace-test/avatars
```

### E2. 集成测试覆盖矩阵

| 测试文件 | 测试场景 | 覆盖流程 |
|---------|---------|---------|
| `VideoIntegrationTest` | 完整分片上传流程 | init → chunk × N → complete → 查询 |
| `MetadataIntegrationTest` | 元数据生成与确认 | generate（Mock LLM）→ update → confirm |
| `DistributionIntegrationTest` | 发布流程 | publish（Mock YouTube API）→ 查询状态 → 查询记录 |
| `PromotionIntegrationTest` | 推广全流程 | 创建渠道 → 生成文案（Mock LLM）→ 执行推广（Mock OpenCrawl）→ 查询报告 |
| `UserSettingsIntegrationTest` | 设置 CRUD | 获取/更新 Profile → 上传头像 → 通知偏好 → API Key 生命周期 |
| `DashboardIntegrationTest` | 仪表盘聚合查询 | 预置数据 → 调用 A1 → 验证各统计字段 |

### E3. 端到端集成测试示例

```java
class VideoIntegrationTest extends AbstractIntegrationTest {
    
    @Autowired
    private VideoApplicationService videoService;
    
    @Autowired
    private VideoRepository videoRepository;
    
    @Test
    @DisplayName("完整分片上传流程：初始化 → 上传分片 → 完成 → 验证")
    void fullChunkedUploadFlow() {
        // 1. Init
        InitUploadResponse init = videoService.initUpload(
            new InitUploadRequest("test.mp4", 10_000_000L, "MP4", 3));
        assertThat(init.uploadId()).isNotBlank();
        
        // 2. Upload chunks
        for (int i = 0; i < 3; i++) {
            videoService.uploadChunk(init.uploadId(), i, createMockChunk(i));
        }
        
        // 3. Complete
        CompleteUploadResponse complete = videoService.completeUpload(init.uploadId());
        assertThat(complete.videoId()).isNotBlank();
        
        // 4. Verify
        Optional<Video> video = videoRepository.findById(new VideoId(complete.videoId()));
        assertThat(video).isPresent();
        assertThat(video.get().getStatus()).isEqualTo(VideoStatus.UPLOADED);
        assertThat(video.get().getFileName()).isEqualTo("test.mp4");
    }
}
```

### E4. Mock 外部服务策略

| 外部服务 | Mock 方式 | 说明 |
|---------|----------|------|
| 阿里云 LLM（通义千问） | `@MockBean LlmService` | 返回预定义的 LlmResponse |
| YouTube Data API v3 | `@MockBean YouTubeApiAdapter` | 返回预定义的上传结果 |
| OpenCrawl API | `@MockBean OpenCrawlAdapter` | 返回预定义的执行结果 |

```java
@MockBean
private LlmService llmService;

@BeforeEach
void setupMocks() {
    when(llmService.chat(any(LlmRequest.class)))
        .thenReturn(new LlmResponse(
            "{\"title\":\"Test Title\",\"description\":\"Test Desc\",\"tags\":[\"t1\",\"t2\",\"t3\",\"t4\",\"t5\"]}",
            100, 200));
}
```

---

## F. 测试数据工厂（Test Fixtures）

```java
public final class TestFixtures {
    
    private TestFixtures() {}
    
    // --- Video Context ---
    
    public static Video createVideo() {
        return Video.create(
            new VideoId(UUID.randomUUID().toString()),
            "测试视频.mp4",
            10_000_000L,
            VideoFormat.MP4,
            120L,
            "/storage/videos/test.mp4"
        );
    }
    
    public static UploadSession createUploadSession() {
        return UploadSession.create(
            "upload-" + UUID.randomUUID(),
            "测试视频.mp4",
            10_000_000L,
            VideoFormat.MP4,
            3,
            "/storage/temp/upload-xxx"
        );
    }
    
    // --- Metadata Context ---
    
    public static VideoMetadata createMetadata(VideoId videoId) {
        return VideoMetadata.create(
            new VideoMetadataId(UUID.randomUUID().toString()),
            videoId,
            "AI 生成标题",
            "AI 生成的描述内容",
            List.of("美食", "烹饪", "教程", "厨房", "食谱"),
            MetadataSource.AI_GENERATED
        );
    }
    
    // --- Distribution Context ---
    
    public static PublishRecord createPublishRecord(VideoId videoId) {
        return PublishRecord.create(
            new PublishRecordId(UUID.randomUUID().toString()),
            videoId,
            new VideoMetadataId("meta-001"),
            "youtube"
        );
    }
    
    // --- Promotion Context ---
    
    public static PromotionChannel createChannel() {
        return PromotionChannel.create(
            new ChannelId(UUID.randomUUID().toString()),
            "测试渠道",
            ChannelType.SOCIAL_MEDIA,
            "https://example.com",
            1
        );
    }
    
    public static PromotionRecord createPromotionRecord(VideoId videoId, ChannelId channelId) {
        return PromotionRecord.create(
            new PromotionRecordId(UUID.randomUUID().toString()),
            videoId,
            channelId,
            "推广文案内容",
            PromotionMethod.POST
        );
    }
    
    // --- User & Settings Context ---
    
    public static ApiKey createApiKey() {
        return ApiKey.create(
            new ApiKeyId(UUID.randomUUID().toString()),
            "Test API Key",
            "$2a$12$hashedKeyValue",
            "grc_test...key1",
            LocalDateTime.now().plusDays(90)
        );
    }
}
```

---

## G. jqwik 自定义 Arbitrary 生成器

```java
public final class GraceArbitraries {
    
    private GraceArbitraries() {}
    
    public static Arbitrary<VideoFormat> videoFormats() {
        return Arbitraries.of(VideoFormat.values());
    }
    
    public static Arbitrary<String> videoFileNames() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(50)
            .map(name -> name + "." + Arbitraries.of("mp4", "mov", "avi", "mkv").sample());
    }
    
    public static Arbitrary<Long> validFileSizes() {
        return Arbitraries.longs().between(1, 5L * 1024 * 1024 * 1024);
    }
    
    public static Arbitrary<String> metadataTitles() {
        return Arbitraries.strings().ofMinLength(1).ofMaxLength(100);
    }
    
    public static Arbitrary<String> metadataDescriptions() {
        return Arbitraries.strings().ofMinLength(0).ofMaxLength(5000);
    }
    
    public static Arbitrary<List<String>> metadataTags() {
        return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20)
            .list().ofMinSize(5).ofMaxSize(15);
    }
    
    public static Arbitrary<ChannelType> channelTypes() {
        return Arbitraries.of(ChannelType.values());
    }
    
    public static Arbitrary<PromotionMethod> promotionMethods() {
        return Arbitraries.of(PromotionMethod.values());
    }
    
    public static Arbitrary<String> platformIdentifiers() {
        // 混合已注册和未注册的平台标识
        return Arbitraries.oneOf(
            Arbitraries.of("youtube", "bilibili", "douyin"),  // 已知平台
            Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20) // 随机平台
        );
    }
}
```

---

## H. 属性-测试-需求追溯矩阵

| Property | 测试文件 | 需求 | 上下文 |
|----------|---------|------|-------|
| 1 | `VideoPropertyTest` | 1.1 | Video |
| 2 | `VideoPropertyTest` | 1.3, 1.5 | Video |
| 3 | `VideoPropertyTest` | 1.7 | Video |
| 4 | `MetadataPropertyTest` | 2.2, 2.3, 2.4 | Metadata |
| 5 | `MetadataPropertyTest` | 3.2, 3.3 | Metadata |
| 6 | `DistributionPropertyTest` | 4.1 | Distribution |
| 7 | `DistributionPropertyTest` | 4.3 | Distribution |
| 8 | `DistributionPropertyTest` | 4.7 | Distribution |
| 9 | `PromotionPropertyTest` | 5.1 | Promotion |
| 10 | `PromotionPropertyTest` | 5.3 | Promotion |
| 11 | `PromotionPropertyTest` | 6.1, 6.3, 6.4 | Promotion |
| 12 | `PromotionPropertyTest` | 7.3, 7.5 | Promotion |
