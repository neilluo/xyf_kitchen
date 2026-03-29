# Phase 13: 存储架构迁移 - 本地文件系统到阿里云 OSS

> 目标：将视频存储从本地文件系统迁移到阿里云 OSS，实现前端直传
> 影响范围：Video、Metadata、Distribution、Storage 四个限界上下文
> 预估工时：37 小时

## 进度统计

- [x] 共 8 个任务，已完成 7/8

---

## 背景与动机

### 当前问题
- 视频存储在服务器本地磁盘 (`./data/videos/`)，磁盘空间有限
- 多平台分发时需要从服务器读取视频再上传，带宽压力大
- 服务器需要处理分片合并，CPU/IO 开销大

### 目标架构
- 视频直传阿里云 OSS，服务器不存储视频文件
- 多平台分发时直接使用 OSS URL，无需重复上传
- 元数据生成从 OSS 提取视频帧

---

## 架构变更总览

### 流程对比

```
当前流程（本地存储）：
用户 → 分片上传后端 → 后端合并 → 本地存储 → 元数据生成 → 分发时读取本地文件上传

新流程（OSS 直传）：
用户 → 获取 STS 凭证 → 直传 OSS → OSS 回调后端 → 保存 OSS URL → 元数据生成 → 分发时使用 OSS URL
```

### 新增限界上下文

**Storage Context（存储上下文）**
- 职责：OSS 存储抽象、STS 临时凭证管理、上传回调处理
- 位置：`com.grace.platform.storage`

---

## 任务列表

### P13-01: 创建 Storage 限界上下文 - 领域层

- **参考文档**: `docs/backend/02-shared-kernel.md`（值对象模式）
- **产出文件**:
  - `storage.domain.StorageProvider`（枚举：OSS/LOCAL/S3）
  - `storage.domain.StsCredentials`（STS 临时凭证值对象）
  - `storage.domain.OssStorageService`（OSS 服务接口）
  - `storage.domain.UploadCallback`（OSS 回调数据对象）
  - `storage.domain.StorageUrl`（存储 URL 值对象）
- **验证命令**: `mvn clean compile`
- **依赖**: 无
- **状态**: [x]
- **注意**: 遵循 DDD 分层，domain 层不依赖 Spring

---

### P13-02: 创建 Storage 限界上下文 - 基础设施层

- **参考文档**: `docs/backend/09-infrastructure-config.md`（配置类模式）
- **产出文件**:
  - `storage.infrastructure.ali.AliOssStorageServiceImpl`（阿里云 OSS 实现）
  - `storage.infrastructure.ali.OssProperties`（OSS 配置类）
  - `storage.infrastructure.ali.StsTokenService`（STS 凭证获取服务）
- **验证命令**: `mvn clean compile`
- **依赖**: P13-01
- **状态**: [x]
- **注意**: 
  - 使用阿里云 Java SDK
  - STS 角色 ARN 从配置注入
  - 处理 OSS 回调签名验证

---

### P13-03: 创建 Storage 限界上下文 - 接口层

- **参考文档**: `api.md` §通用约定（ApiResponse 包装）
- **产出文件**:
  - `storage.interfaces.OssStorageController`（STS 凭证获取 API）
  - `storage.interfaces.OssCallbackController`（OSS 回调接收 API）
  - `storage.interfaces.dto.request.StsTokenRequest`
  - `storage.interfaces.dto.response.StsTokenResponse`
  - `storage.interfaces.dto.request.OssCallbackRequest`
- **验证命令**: `mvn clean compile`
- **依赖**: P13-01
- **状态**: [x]
- **注意**:
  - API: `POST /api/storage/oss/sts-token`
  - API: `POST /api/storage/oss/callback`
  - 回调需要验证签名防篡改

---

### P13-04: 重构 Video 上下文 - 领域层变更

- **参考文档**: `docs/backend/03-context-video.md`（Video 聚合根）
- **产出文件**:
  - 修改 `video.domain.Video`:
    - `filePath: String` → `storageUrl: String`
    - 新增 `storageProvider: StorageProvider`
  - 修改 `video.domain.UploadSession`:
    - 新增 `storageKey: String`（OSS Object Key）
    - 新增 `ossBucket: String`
    - 删除 `tempDirectory: String`
- **验证命令**: `mvn clean compile`
- **依赖**: P13-01
- **状态**: [x]
- **注意**: 保持向后兼容，支持存量数据迁移

---

### P13-05: 重构 Video 上下文 - 应用层变更

- **参考文档**: `docs/backend/03-context-video.md` §E（应用服务）
- **产出文件**:
  - 修改 `video.application.VideoApplicationService`:
    - `initUpload`: 创建 UploadSession，返回 STS 凭证
    - `uploadChunk`: **删除方法**
    - `completeUpload`: 接收 OSS 回调，不再合并分片
    - `getUploadProgress`: 改为查询 OSS 上传进度或回调状态
  - 删除 `video.application.command.UploadChunkCommand`
  - 删除 `video.application.dto.ChunkUploadDTO`
- **验证命令**: `mvn clean compile`
- **依赖**: P13-04
- **状态**: [x]
- **注意**: 删除 ChunkMergeService 依赖

---

### P13-06: 重构 Video 上下文 - 基础设施层清理

- **参考文档**: 无（删除代码）
- **产出文件**:
  - 删除 `video.infrastructure.file.ChunkMergeService` 接口
  - 删除 `video.infrastructure.file.ChunkMergeServiceImpl`
  - 修改 `video.infrastructure.file.VideoFileInspectorImpl`:
    - 支持从 OSS URL 提取视频信息（下载到临时文件）
- **验证命令**: `mvn clean compile`
- **依赖**: P13-05
- **状态**: [x]
- **注意**: 
  - VideoFileInspector 改为从 OSS 下载到临时文件后提取
  - 提取完成后删除临时文件

---

### P13-07: 重构 Metadata 上下文 - 多模态输入适配

- **参考文档**: `docs/backend/04-context-metadata.md`（多模态元数据生成）
- **产出文件**:
  - 修改 `metadata.domain.VideoFrameExtractor`:
    - 支持从 OSS URL 提取视频帧
  - 修改 `metadata.infrastructure.ali.QwenLlmServiceAdapter`:
    - 多模态输入改为 OSS 图片 URL（使用 OSS 截图功能）
- **验证命令**: `mvn clean compile`
- **依赖**: P13-06
- **状态**: [x]
- **注意**: 
  - 方案 A：使用 OSS 视频截帧功能生成图片 URL
  - 方案 B：下载到临时文件提取帧后删除

---

### P13-08: 重构 Distribution 上下文 - YouTube 分发适配

- **参考文档**: `docs/backend/05-context-distribution.md`（YouTube 分发）
- **产出文件**:
  - 修改 `distribution.domain.VideoDistributor` 接口:
    - `distribute(videoFile, metadata)` → `distribute(storageUrl, metadata)`
  - 修改 `distribution.infrastructure.youtube.YouTubeDistributor`:
    - 从 OSS URL 流式读取视频上传到 YouTube
- **验证命令**: `mvn clean compile`
- **依赖**: P13-07
- **状态**: [ ]
- **注意**: YouTube API 支持从 URL 上传视频

---

### P13-09: 数据库迁移脚本

- **参考文档**: `docs/backend/db-design.md`
- **产出文件**:
  - `src/main/resources/db/migration/V7__migrate_to_oss_storage.sql`
- **验证命令**: `mvn flyway:migrate`
- **依赖**: P13-04
- **状态**: [ ]
- **注意**: 
  - 存量数据：file_path → storage_url（手动迁移或保持兼容）
  - 新增字段：storage_provider, storage_key, oss_bucket

---

### P13-10: 前端重构 - OSS 直传上传

- **参考文档**: `docs/frontend/06-page-video-upload.md`
- **产出文件**:
  - `src/api/storage.ts`（STS 凭证获取 API）
  - `src/hooks/useOssUpload.ts`（OSS 上传 hook，使用 ali-oss SDK）
  - 重写 `src/pages/VideoUploadPage.tsx`
  - 删除 `src/hooks/useUpload.ts`（旧分片上传 hook）
- **验证命令**: `cd grace-frontend && npm run lint && npx tsc --noEmit`
- **依赖**: P13-03
- **状态**: [ ]
- **注意**: 
  - 安装依赖：`npm install ali-oss`
  - 支持断点续传
  - 上传完成后轮询后端或等待 WebSocket 通知

---

## 数据库 Schema 变更

### V7__migrate_to_oss_storage.sql

```sql
-- video 表变更
ALTER TABLE video 
  ADD COLUMN storage_url VARCHAR(1000) AFTER file_path,
  ADD COLUMN storage_provider VARCHAR(20) DEFAULT 'LOCAL' AFTER storage_url,
  ADD COLUMN storage_key VARCHAR(500) AFTER storage_provider;

-- 存量数据迁移（可选，手动执行）
-- UPDATE video SET storage_url = CONCAT('file://', file_path) WHERE storage_url IS NULL;

-- upload_session 表变更
ALTER TABLE upload_session
  ADD COLUMN storage_key VARCHAR(500) AFTER temp_directory,
  ADD COLUMN oss_bucket VARCHAR(100) AFTER storage_key;

-- 可选：删除本地存储字段（确认迁移完成后执行）
-- ALTER TABLE video DROP COLUMN file_path;
-- ALTER TABLE upload_session DROP COLUMN temp_directory;
```

---

## API 变更

### 新增 API

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/storage/oss/sts-token` | POST | 获取 STS 临时凭证 |
| `/api/storage/oss/callback` | POST | OSS 上传完成回调 |

### 修改 API

| 端点 | 变更 |
|------|------|
| `/api/videos/upload/init` | 响应新增 `stsCredentials`, `ossBucket`, `storageKey` |
| `/api/videos/upload/{uploadId}/chunk` | **删除** |
| `/api/videos/upload/{uploadId}/complete` | 改为 OSS 回调触发，前端不再调用 |

### 删除 API

| 端点 | 说明 |
|------|------|
| `/api/videos/upload/{uploadId}/chunk` | 前端直传 OSS，后端不接收分片 |

---

## 阿里云配置

### OSS Bucket 配置

```yaml
# OSS 配置
oss:
  endpoint: oss-cn-hangzhou.aliyuncs.com
  bucket: grace-video-storage
  region: cn-hangzhou
  callback:
    url: http://your-backend/api/storage/oss/callback
    auth: enabled
  sts:
    role-arn: acs:ram::your_account:role/grace-oss-sts-role
    duration-seconds: 3600  # 1小时
```

### RAM 角色权限

```json
{
  "Version": "1",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "oss:PutObject",
        "oss:GetObject",
        "oss:AbortMultipartUpload",
        "oss:ListParts"
      ],
      "Resource": [
        "acs:oss:*:*:grace-video-storage/*"
      ]
    }
  ]
}
```

---

## 前端上传流程

```typescript
// 新上传流程
async function uploadVideo(file: File) {
  // 1. 初始化上传，获取 STS 凭证
  const { uploadId, stsCredentials, storageKey, ossBucket } = await initUpload({
    fileName: file.name,
    fileSize: file.size,
    format: 'MP4'
  })
  
  // 2. 创建 OSS 客户端
  const ossClient = new OSS({
    region: stsCredentials.region,
    bucket: ossBucket,
    accessKeyId: stsCredentials.accessKeyId,
    accessKeySecret: stsCredentials.accessKeySecret,
    stsToken: stsCredentials.securityToken,
  })
  
  // 3. 直传 OSS（支持分片/断点续传）
  await ossClient.multipartUpload(storageKey, file, {
    progress: (p) => setProgress(p * 100)
  })
  
  // 4. 上传完成，OSS 会自动回调后端
  // 前端轮询或等待后端通知元数据生成完成
  const video = await pollVideoStatus(uploadId)
  return video
}
```

---

## 风险与缓解

| 风险 | 影响 | 缓解方案 |
|------|------|---------|
| OSS 上传失败 | 高 | 前端实现断点续传 + 重试机制 |
| OSS 回调丢失 | 高 | 前端轮询 + 后端补偿任务 |
| STS 凭证泄露 | 中 | 限制权限 + 短有效期（1小时）+ 最小权限原则 |
| 成本激增 | 中 | 设置预算告警 + CDN 加速 + 生命周期管理 |
| 存量数据迁移 | 低 | 双写期 + 灰度迁移 + 回滚方案 |

---

## 回滚方案

如需回滚到本地存储：
1. 切换 `StorageProvider` 为 `LOCAL`
2. 恢复 `VideoApplicationService.uploadChunk` 方法
3. 恢复 `ChunkMergeService`
4. 数据库字段保持兼容（storage_url 可存储本地路径）

---

## 验收标准

- [ ] 前端可以直传视频到 OSS
- [ ] OSS 上传完成后自动触发元数据生成
- [ ] 元数据生成可以从 OSS 提取视频帧
- [ ] YouTube 分发可以使用 OSS URL 上传
- [ ] 存量视频可以正常播放和分发
- [ ] 上传失败有重试机制
- [ ] 回调丢失有补偿机制

---

## 参考文档

- [阿里云 OSS 文档](https://help.aliyun.com/document_detail/31817.html)
- [阿里云 STS 文档](https://help.aliyun.com/document_detail/100624.html)
- [阿里云 OSS 视频截帧](https://help.aliyun.com/document_detail/64555.html)
- [ali-oss SDK](https://github.com/ali-sdk/ali-oss)
