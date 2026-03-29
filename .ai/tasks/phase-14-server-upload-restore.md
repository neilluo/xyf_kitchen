# Phase 14: 恢复服务端上传链路

> 目标：恢复服务端分片上传功能，支持外部服务调用
> 背景：Phase 13 将上传改为前端直传 OSS，但需要保留服务端上传链路用于暴露 API
> 预估工时：8 小时

## 进度统计

- [x] 共 6 个任务，已完成 4/6

---

## 背景说明

### Phase 13 改动回顾
- 前端改为直传 OSS（ali-oss SDK）
- 后端删除了分片上传、合并服务（ChunkMergeService）
- 后端改为接收 OSS 回调

### 新需求
- 保留前端直传 OSS 链路（性能更好）
- 恢复服务端上传链路（用于暴露 API 给外部服务）
- 服务端上传完成后转存到 OSS

---

## 架构设计

### 双链路上传支持

```
┌─────────────────────────────────────────────────────────────┐
│                      上传方式选择                            │
├─────────────────────────┬───────────────────────────────────┤
│    前端直传 OSS          │      服务端上传                    │
│    (Web 用户)            │      (外部 API)                    │
├─────────────────────────┼───────────────────────────────────┤
│ 1. 获取 STS 凭证         │ 1. 初始化上传（服务端）             │
│ 2. 直传 OSS              │ 2. 分片上传到服务端                 │
│ 3. OSS 回调后端           │ 3. 服务端合并                       │
│ 4. 完成                  │ 4. 上传到 OSS                      │
│                          │ 5. 完成                            │
└─────────────────────────┴───────────────────────────────────┘
```

### 存储统一

无论哪种上传方式，最终都存储到 OSS：
- 前端直传：直接到 OSS
- 服务端上传：合并后上传到 OSS

---

## 任务列表

### P14-01: 恢复 ChunkMergeService 及相关代码

- **参考文档**: Git 历史记录（从 Phase 13 之前的提交恢复）
- **产出文件**:
  - 恢复 `video.domain.ChunkMergeService` 接口
  - 恢复 `video.infrastructure.file.ChunkMergeServiceImpl`
  - 恢复 `video.application.dto.ChunkUploadDTO`
- **验证命令**: `mvn clean compile`
- **依赖**: 无
- **状态**: [x]
- **注意**: 从 Git 历史找回代码，保持原有实现

---

### P14-02: 创建服务端上传应用服务

- **参考文档**: `docs/backend/03-context-video.md` §E（应用服务）
- **产出文件**:
  - `video.application.ServerUploadApplicationService`
  - `video.application.dto.ServerUploadInitDTO`
  - `video.application.dto.ServerChunkUploadDTO`
  - `video.application.dto.ServerUploadCompleteDTO`
  - `video.application.command.ServerUploadInitCommand`
- **验证命令**: `mvn clean compile`
- **依赖**: P14-01
- **状态**: [x]
- **注意**: 
  - 与前端直传区分，使用独立的服务类
  - 合并后上传到 OSS，不保留本地文件

---

### P14-03: 创建服务端上传控制器

- **参考文档**: `api.md` §B（视频上传 API）
- **产出文件**:
  - `video.interfaces.ServerUploadController`
  - `video.interfaces.dto.request.ServerUploadInitRequest`
  - `video.interfaces.dto.response.ServerUploadInitResponse`
  - `video.interfaces.dto.request.ServerChunkUploadRequest`
  - `video.interfaces.dto.response.ServerChunkUploadResponse`
  - `video.interfaces.dto.response.ServerUploadCompleteResponse`
- **验证命令**: `mvn clean compile`
- **依赖**: P14-02
- **状态**: [x]
- **注意**:
  - API 路径：`/api/videos/upload/server/*`
  - 与原有前端上传 API 区分

**API 列表**:

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/videos/upload/server/init` | POST | 服务端初始化上传 |
| `/api/videos/upload/server/{uploadId}/chunk` | POST | 服务端上传分片 |
| `/api/videos/upload/server/{uploadId}/complete` | POST | 服务端完成上传并转存 OSS |

---

### P14-04: 修改 UploadSession 支持双模式

- **参考文档**: `docs/backend/03-context-video.md` §B.2
- **产出文件**:
  - 修改 `video.domain.UploadSession`:
    - 新增 `uploadMode: UploadMode`（DIRECT_OSS / SERVER_UPLOAD）
    - 恢复 `tempDirectory` 字段（服务端上传需要）
- **验证命令**: `mvn clean compile`
- **依赖**: P14-01
- **状态**: [x]
- **注意**: 保持与 Phase 13 的兼容性

---

### P14-05: 实现服务端上传到 OSS 转存

- **参考文档**: `storage.infrastructure.ali.AliOssStorageServiceImpl`
- **产出文件**:
  - 修改 `ServerUploadApplicationService.completeUpload`:
    - 合并分片
    - 上传到 OSS
    - 删除本地临时文件
    - 保存 Video 记录
- **验证命令**: `mvn clean compile`
- **依赖**: P14-02, P14-04
- **状态**: [ ]
- **注意**: 
  - 使用 AliOssStorageServiceImpl 上传到 OSS
  - 确保临时文件清理

---

### P14-06: 前端适配 - 添加上传方式选择

- **参考文档**: `docs/frontend/06-page-video-upload.md`
- **产出文件**:
  - 修改 `src/pages/VideoUploadPage.tsx`:
    - 添加上传方式选择（直传 OSS / 服务端上传）
    - 默认使用直传 OSS
    - 保留服务端上传选项（高级/外部服务使用）
- **验证命令**: `cd grace-frontend && npm run lint && npx tsc --noEmit`
- **依赖**: P14-03
- **状态**: [ ]
- **注意**: 默认用户仍使用直传 OSS，服务端上传作为备用选项

---

## 数据库 Schema 变更

### V8__add_server_upload_support.sql

```sql
-- upload_session 表添加上传模式字段
ALTER TABLE upload_session
  ADD COLUMN upload_mode VARCHAR(20) DEFAULT 'DIRECT_OSS' AFTER status,
  ADD COLUMN temp_directory VARCHAR(1000) AFTER oss_bucket;

-- 注释：upload_mode 取值：DIRECT_OSS（前端直传）/ SERVER_UPLOAD（服务端上传）
```

---

## API 变更汇总

### 新增 API（服务端上传）

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/videos/upload/server/init` | POST | 服务端初始化上传 |
| `/api/videos/upload/server/{uploadId}/chunk` | POST | 服务端上传分片 |
| `/api/videos/upload/server/{uploadId}/complete` | POST | 服务端完成上传并转存 OSS |

### 现有 API（保持不变）

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/videos/upload/init` | POST | 前端直传初始化（返回 STS） |
| `/api/storage/oss/callback` | POST | OSS 上传完成回调 |

---

## 代码结构

```
com.grace.platform.video
├── application
│   ├── VideoApplicationService          # 前端直传（Phase 13）
│   ├── ServerUploadApplicationService   # 服务端上传（新增）
│   └── dto
│       ├── ServerUploadInitDTO
│       ├── ServerChunkUploadDTO
│       └── ServerUploadCompleteDTO
├── domain
│   ├── ChunkMergeService                # 恢复
│   ├── UploadMode                       # 新增枚举
│   └── UploadSession                    # 修改
├── infrastructure
│   └── file
│       └── ChunkMergeServiceImpl        # 恢复
└── interfaces
    ├── VideoUploadController            # 前端直传（Phase 13）
    └── ServerUploadController           # 服务端上传（新增）
```

---

## 关键实现细节

### 服务端上传到 OSS 流程

```java
@Service
public class ServerUploadApplicationService {
    
    public VideoInfoDTO completeUpload(String uploadId) {
        // 1. 获取 UploadSession
        UploadSession session = uploadSessionRepository.findById(uploadId);
        
        // 2. 合并分片（本地）
        Path tempDir = Paths.get(session.getTempDirectory());
        Path mergedFile = chunkMergeService.mergeChunks(tempDir, ...);
        
        // 3. 上传到 OSS
        String storageKey = generateStorageKey(session);
        ossStorageService.uploadFile(mergedFile, storageKey);
        
        // 4. 获取 OSS URL
        String storageUrl = ossStorageService.getVideoUrl(storageKey);
        
        // 5. 创建 Video 记录
        Video video = Video.create(
            session.getFileName(),
            session.getFileSize(),
            session.getFormat(),
            duration,
            storageUrl,  // OSS URL
            StorageProvider.OSS
        );
        videoRepository.save(video);
        
        // 6. 清理本地文件
        Files.delete(mergedFile);
        deleteChunks(tempDir);
        
        // 7. 发布事件
        eventPublisher.publish(new VideoUploadedEvent(...));
        
        return new VideoInfoDTO(...);
    }
}
```

---

## 验收标准

- [ ] 服务端上传 API 可用（`/api/videos/upload/server/*`）
- [ ] 服务端上传后视频存储到 OSS
- [ ] 前端直传 OSS 链路仍然可用
- [ ] 两种方式上传的视频都可以正常播放和分发
- [ ] 服务端上传后本地临时文件被清理
- [ ] 数据库 upload_mode 字段正确记录上传方式

---

## 回滚方案

如需回滚：
1. 删除 ServerUploadController
2. 删除 ServerUploadApplicationService
3. 保留 ChunkMergeService（不影响前端直传）
4. 回滚数据库迁移 V8

---

## 参考文档

- Phase 13 代码（前端直传 OSS 实现）
- Git 历史（找回 ChunkMergeService 代码）
- `docs/backend/03-context-video.md`
