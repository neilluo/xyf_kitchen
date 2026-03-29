# Video 模块 - AGENTS.md

## 模块定位

**Video 模块**是 Grace 平台的**核心业务能力模块**，负责视频的完整生命周期管理：从上传、存储、元数据生成到多平台分发。它是整个平台的**数据入口**和**流程编排中心**。

## 核心能力

### 1. 双模式上传支持
- **前端直传 OSS**：用户通过浏览器直连 OSS 上传，性能最优
- **服务端上传**：外部服务通过 API 分片上传到后端，后端转存 OSS
- 统一的上传会话管理（UploadSession）

### 2. 视频生命周期管理
- 视频上传（Upload）
- 元数据生成（Metadata Generation）
- 元数据审核（Metadata Review）
- 多平台分发（Distribution）
- 社区推广（Promotion）

### 3. 视频信息提取
- 视频格式识别（MP4/MOV/AVI/MKV）
- 视频时长提取
- 视频帧提取（用于 AI 元数据生成）
- 文件大小校验（最大 5GB）

### 4. 状态机管理
```
UPLOADED → METADATA_GENERATED → READY_TO_PUBLISH → PUBLISHING → PUBLISHED → PROMOTION_DONE
```

### 5. 领域事件发布
- `VideoUploadedEvent`：上传完成，触发元数据生成
- `MetadataConfirmedEvent`：元数据确认，触发分发
- `VideoPublishedEvent`：发布完成，触发推广

## 模块边界

### 对外暴露的接口

| 接口 | 说明 | 使用方 |
|------|------|--------|
| `VideoUploadController.initUpload()` | 初始化上传（返回 STS 或服务端配置） | 前端 |
| `VideoUploadController.handleOssCallback()` | 接收 OSS 上传回调 | OSS |
| `ServerUploadController.initUpload()` | 服务端初始化上传 | 外部服务 |
| `ServerUploadController.uploadChunk()` | 服务端上传分片 | 外部服务 |
| `ServerUploadController.completeUpload()` | 服务端完成上传 | 外部服务 |
| `VideoApplicationService.getVideo()` | 获取视频信息 | Metadata、Distribution |
| `VideoApplicationService.listVideos()` | 查询视频列表 | Dashboard、前端 |

### 依赖的其他模块

| 模块 | 依赖说明 |
|------|---------|
| **Storage** | 获取 STS 凭证、上传文件到 OSS、获取视频 URL |
| **Metadata** | 发布 VideoUploadedEvent，订阅 MetadataConfirmedEvent |
| **Distribution** | 发布 MetadataConfirmedEvent，订阅 VideoPublishedEvent |

### 不对外暴露的实现细节

- UploadSession 的存储实现（MyBatis）
- 视频文件合并的具体算法（FileChannel）
- 视频信息提取的实现（FFprobe）
- 状态机的内部转换逻辑

## 核心领域对象

### Video（视频聚合根）
```java
public class Video {
    private VideoId id;                    // 视频 ID
    private String fileName;               // 原始文件名
    private long fileSize;                 // 文件大小
    private VideoFormat format;            // 视频格式
    private Duration duration;             // 视频时长
    private String storageUrl;             // 存储 URL（OSS）
    private StorageProvider storageProvider; // 存储提供商
    private VideoStatus status;            // 视频状态
    private LocalDateTime createdAt;       // 创建时间
    private LocalDateTime updatedAt;       // 更新时间
}
```

### UploadSession（上传会话）
```java
public class UploadSession {
    private String uploadId;               // 上传会话 ID
    private String fileName;               // 文件名
    private long fileSize;                 // 文件大小
    private VideoFormat format;            // 文件格式
    private int totalChunks;               // 总分片数
    private int uploadedChunks;            // 已上传分片数
    private String storageKey;             // OSS Object Key
    private String ossBucket;              // OSS Bucket
    private UploadMode uploadMode;         // 上传模式（DIRECT_OSS / SERVER_UPLOAD）
    private String tempDirectory;          // 临时目录（服务端上传用）
    private UploadSessionStatus status;    // 会话状态
    private LocalDateTime expiresAt;       // 过期时间
}
```

### UploadMode（上传模式枚举）
```java
public enum UploadMode {
    DIRECT_OSS,      // 前端直传 OSS
    SERVER_UPLOAD    // 服务端上传
}
```

## 上传流程对比

### 前端直传 OSS（DIRECT_OSS）
```
用户 → 初始化上传 → 获取 STS 凭证 → 直传 OSS → OSS 回调后端 → 完成
```

**特点**：
- 上传速度快（用户直连 OSS）
- 服务器无带宽压力
- 适合 Web 用户上传

### 服务端上传（SERVER_UPLOAD）
```
外部服务 → 初始化上传 → 分片上传后端 → 后端合并 → 上传到 OSS → 完成
```

**特点**：
- 支持外部 API 调用
- 后端控制上传流程
- 适合后台导入、存量迁移

## 与 Storage 模块的关系

```
Video 模块（业务层）
    ↓ 调用
Storage 模块（基础设施层）
    ↓ 使用
阿里云 OSS
```

**职责分离**：
- **Video 模块**：业务逻辑、流程编排、状态管理
- **Storage 模块**：存储抽象、STS 凭证、OSS 操作

## 关键设计决策

### 1. 为什么需要双模式上传？
- **前端直传**：用户体验好，服务器压力小
- **服务端上传**：API 暴露，外部系统集成
- **统一存储**：最终都存储到 OSS，便于分发

### 2. 为什么 UploadSession 要区分 uploadMode？
- 不同模式需要不同的后续处理
- 服务端上传需要本地临时目录
- 便于统计和监控两种上传方式的使用情况

### 3. 为什么视频状态机不可逆？
- `PUBLISHED` 后不可回退（已发布到外部平台）
- `METADATA_CONFIRMED` 后不可编辑（已确认元数据）
- 保证业务流程的严谨性

## 使用示例

### 前端直传 OSS
```java
// 1. 初始化上传
UploadInitDTO init = videoApplicationService.initUpload(command);
// 返回：uploadId, stsCredentials, storageKey, ossBucket

// 2. 前端使用 STS 凭证直传 OSS（ali-oss SDK）

// 3. OSS 回调后端
videoApplicationService.handleOssCallback(uploadId, callback);
```

### 服务端上传
```java
// 1. 初始化上传
ServerUploadInitDTO init = serverUploadApplicationService.initUpload(command);

// 2. 分片上传（循环调用）
for (int i = 0; i < totalChunks; i++) {
    serverUploadApplicationService.uploadChunk(uploadId, i, chunkInput);
}

// 3. 完成上传（合并 + 转存 OSS）
ServerUploadCompleteDTO complete = serverUploadApplicationService.completeUpload(uploadId);
```

## 注意事项

1. **上传会话过期**：默认 24 小时，需要及时完成上传
2. **分片大小**：服务端上传建议 5MB/片，前端直传由 OSS SDK 控制
3. **临时文件清理**：服务端上传完成后需要清理本地临时文件
4. **并发控制**：同一 uploadId 的分片上传需要串行处理

## 扩展方向

- 支持更多视频格式（WebM、FLV 等）
- 支持视频转码（生成多清晰度）
- 支持视频截图封面
- 支持视频水印
- 支持批量上传
