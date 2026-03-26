# Grace Platform API 接口文档

## 1. 概述

本文档定义 Grace 视频分发与推广平台的全部 REST API 接口，作为服务端暴露给前端的契约。

| 项目 | 说明 |
|------|------|
| 基础 URL | `http://{host}:{port}/api` |
| 后端框架 | Java 21 + Spring Boot 3.x |
| 架构风格 | DDD（领域驱动设计） |
| 数据库 | MySQL |
| 传输协议 | HTTPS（生产环境） |
| 数据格式 | `application/json; charset=UTF-8`（分片上传使用 `multipart/form-data`） |
| 认证方式 | MVP 阶段暂无认证；后续通过 `X-API-Key` Header 认证 |

---

## 2. 通用约定

### 2.1 统一响应信封

所有接口统一返回以下 JSON 结构：

**成功响应：**

```json
{
  "code": 0,
  "message": "success",
  "data": { },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| code | int | 业务状态码，`0` 表示成功 |
| message | string | 描述信息 |
| data | object / array / null | 业务数据 |
| timestamp | string | ISO 8601 格式响应时间 |

**分页响应（data 内嵌套）：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "items": [],
    "total": 100,
    "page": 1,
    "pageSize": 20,
    "totalPages": 5
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| items | array | 当前页数据 |
| total | long | 总记录数 |
| page | int | 当前页码（从 1 开始） |
| pageSize | int | 每页条数 |
| totalPages | int | 总页数 |

**错误响应：**

```json
{
  "code": 1001,
  "message": "不支持的视频格式：WMV。支持的格式：MP4, MOV, AVI, MKV",
  "errors": [
    { "field": "format", "message": "格式不在支持列表中" }
  ],
  "timestamp": "2024-01-15T10:30:00Z"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| code | int | 业务错误码（非 0） |
| message | string | 用户可读的错误描述 |
| errors | array / null | 字段级校验错误（可选） |
| errors[].field | string | 出错字段名 |
| errors[].message | string | 字段错误描述 |
| timestamp | string | ISO 8601 |

### 2.2 通用查询参数

适用于所有分页列表接口：

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| page | int | 1 | 页码，从 1 开始 |
| pageSize | int | 20 | 每页条数，最大 100 |
| sort | string | createdAt | 排序字段 |
| order | string | desc | 排序方向：`asc` / `desc` |

### 2.3 错误码体系

| 错误码 | HTTP Status | 名称 | 触发条件 |
|--------|-------------|------|---------|
| 0 | 200/201 | SUCCESS | 请求成功 |
| **1001-1099：视频上下文** | | | |
| 1001 | 400 | UNSUPPORTED_VIDEO_FORMAT | 文件格式不在 MP4/MOV/AVI/MKV 列表中 |
| 1002 | 400 | VIDEO_FILE_SIZE_EXCEEDED | 文件大小 > 5GB |
| 1003 | 404 | UPLOAD_SESSION_NOT_FOUND | uploadId 不存在或已过期 |
| 1004 | 400 | UPLOAD_SESSION_EXPIRED | 上传会话已超时 |
| 1005 | 400 | CHUNK_INDEX_OUT_OF_RANGE | chunkIndex 超出 totalChunks 范围 |
| 1006 | 400 | DUPLICATE_CHUNK | 重复上传相同 chunkIndex |
| 1007 | 400 | UPLOAD_NOT_COMPLETE | 尚有分片未上传就调用 complete |
| 1008 | 404 | VIDEO_NOT_FOUND | 视频 ID 不存在 |
| **2001-2099：元数据上下文** | | | |
| 2001 | 400 | INVALID_METADATA | 元数据字段约束校验失败 |
| 2002 | 404 | METADATA_NOT_FOUND | 元数据 ID 不存在 |
| 2003 | 409 | METADATA_ALREADY_CONFIRMED | 已确认的元数据不可再编辑 |
| 2004 | 400 | VIDEO_NOT_UPLOADED | 视频尚未上传完成 |
| **3001-3099：分发上下文** | | | |
| 3001 | 400 | UNSUPPORTED_PLATFORM | 请求的分发平台未注册 |
| 3002 | 401 | PLATFORM_AUTH_EXPIRED | 平台 OAuth token 过期 |
| 3003 | 400 | PLATFORM_NOT_AUTHORIZED | 平台未授权连接 |
| 3004 | 429 | PLATFORM_QUOTA_EXCEEDED | 平台 API 配额超限 |
| 3005 | 400 | VIDEO_NOT_READY | 视频状态非 READY_TO_PUBLISH |
| 3006 | 404 | PUBLISH_TASK_NOT_FOUND | 发布任务 ID 不存在 |
| 3007 | 502 | PLATFORM_API_ERROR | 平台 API 返回非预期错误 |
| **4001-4099：推广上下文** | | | |
| 4001 | 404 | CHANNEL_NOT_FOUND | 推广渠道 ID 不存在 |
| 4002 | 400 | INVALID_CHANNEL_CONFIG | 推广渠道配置校验失败 |
| 4003 | 400 | CHANNEL_DISABLED | 推广渠道已被禁用 |
| 4004 | 404 | PROMOTION_RECORD_NOT_FOUND | 推广记录不存在 |
| **5001-5099：设置上下文** | | | |
| 5001 | 404 | PROFILE_NOT_FOUND | 用户资料不存在 |
| 5002 | 404 | API_KEY_NOT_FOUND | API Key 不存在 |
| **9001-9099：基础设施错误** | | | |
| 9001 | 503 | LLM_SERVICE_UNAVAILABLE | 阿里云 LLM 服务调用失败 |
| 9002 | 502 | OPENCRAWL_EXECUTION_FAILED | OpenCrawl 执行失败 |
| 9003 | 500 | ENCRYPTION_ERROR | AES 加解密异常 |
| 9999 | 500 | INTERNAL_SERVER_ERROR | 未知内部错误 |

---

## 3. 接口详细定义

---

### A. 仪表盘 (Dashboard)

---

#### A1. GET /api/dashboard/overview

**描述：** 获取仪表盘全部概览数据。聚合视频统计、最近上传、发布分布、推广概览为一次请求。

**对应UI页面：** grace_dashboard

**查询参数：**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| dateRange | string | 否 | 30d | 推广概览和分析数据的时间范围：`7d` / `30d` / `90d` / `all` |

**响应 `data` 字段：**

| 字段 | 类型 | 说明 |
|------|------|------|
| stats | object | 统计卡片数据 |
| stats.totalVideos | int | 视频总数 |
| stats.pendingReview | int | 待审核数量（UPLOADED + METADATA_GENERATED） |
| stats.published | int | 已发布数量（PUBLISHED） |
| stats.promoting | int | 推广中数量（有 EXECUTING 状态推广记录的视频） |
| recentUploads | array | 最近上传列表（最多 5 条） |
| recentUploads[].videoId | string | 视频 ID |
| recentUploads[].fileName | string | 文件名 |
| recentUploads[].thumbnailUrl | string / null | 缩略图 URL |
| recentUploads[].status | string | 视频状态枚举 |
| recentUploads[].createdAt | string | ISO 8601 上传时间 |
| publishDistribution | object | 发布状态分布（饼图数据） |
| publishDistribution.published | int | 已发布数 |
| publishDistribution.pending | int | 处理中数 |
| publishDistribution.failed | int | 失败数 |
| promotionOverview | array | 各渠道推广成功率 |
| promotionOverview[].channelId | string | 渠道 ID |
| promotionOverview[].channelName | string | 渠道名称 |
| promotionOverview[].totalExecutions | int | 总执行次数 |
| promotionOverview[].successCount | int | 成功次数 |
| promotionOverview[].failedCount | int | 失败次数 |
| promotionOverview[].successRate | double | 成功率（0.0-1.0） |
| analytics | object | 分析数据 |
| analytics.avgEngagementRate | double | 平均互动率 |
| analytics.totalImpressions | long | 总曝光量 |

**响应示例：**

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "stats": {
      "totalVideos": 24,
      "pendingReview": 5,
      "published": 16,
      "promoting": 3
    },
    "recentUploads": [
      {
        "videoId": "vid_abc123",
        "fileName": "法式红酒烩牛肉.mp4",
        "thumbnailUrl": "https://storage.grace.app/thumbnails/vid_abc123.jpg",
        "status": "PUBLISHED",
        "createdAt": "2023-10-24T14:30:00Z"
      }
    ],
    "publishDistribution": {
      "published": 16,
      "pending": 5,
      "failed": 4
    },
    "promotionOverview": [
      {
        "channelId": "ch_001",
        "channelName": "Weibo / 微博",
        "totalExecutions": 20,
        "successCount": 17,
        "failedCount": 3,
        "successRate": 0.85
      }
    ],
    "analytics": {
      "avgEngagementRate": 0.042,
      "totalImpressions": 124800
    }
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

**错误码：** 无特殊错误码

---

### B. 视频管理 (Video)

---

#### B1. POST /api/videos/upload/init

**描述：** 初始化分片上传。校验文件格式（MP4/MOV/AVI/MKV）和大小（≤5GB），创建上传会话。

**对应需求：** 需求 1.1, 1.3, 1.4, 1.5

**请求 Body：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| fileName | string | 是 | 文件名，含扩展名 |
| fileSize | long | 是 | 文件字节数，最大 5,368,709,120 (5GB) |
| format | string | 是 | 文件格式枚举：`MP4` / `MOV` / `AVI` / `MKV` |

```json
{
  "fileName": "红烧肉教程.mp4",
  "fileSize": 1073741824,
  "format": "MP4"
}
```

**响应 `data` 字段：**

| 字段 | 类型 | 说明 |
|------|------|------|
| uploadId | string | 唯一上传会话标识 |
| totalChunks | int | 计算出的总分片数 |
| chunkSize | long | 建议分片大小（字节） |
| expiresAt | string | 上传会话过期时间（ISO 8601） |

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "uploadId": "upl_x7k9m2",
    "totalChunks": 64,
    "chunkSize": 16777216,
    "expiresAt": "2024-01-16T10:30:00Z"
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

**错误码：** `1001` UNSUPPORTED_VIDEO_FORMAT, `1002` VIDEO_FILE_SIZE_EXCEEDED

---

#### B2. POST /api/videos/upload/{uploadId}/chunk

**描述：** 上传单个分片。Content-Type 为 `multipart/form-data`。

**对应需求：** 需求 1.1, 1.6

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| uploadId | string | 上传会话 ID |

**请求参数（multipart/form-data）：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| chunkIndex | int | 是 | 分片索引，从 0 开始 |
| chunk | binary | 是 | 分片文件二进制数据 |

**响应 `data` 字段：**

| 字段 | 类型 | 说明 |
|------|------|------|
| uploadId | string | 上传会话 ID |
| chunkIndex | int | 已接收的分片索引 |
| uploadedChunks | int | 已上传分片总数 |
| totalChunks | int | 总分片数 |

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "uploadId": "upl_x7k9m2",
    "chunkIndex": 5,
    "uploadedChunks": 6,
    "totalChunks": 64
  },
  "timestamp": "2024-01-15T10:31:00Z"
}
```

**错误码：** `1003` UPLOAD_SESSION_NOT_FOUND, `1004` UPLOAD_SESSION_EXPIRED, `1005` CHUNK_INDEX_OUT_OF_RANGE, `1006` DUPLICATE_CHUNK

---

#### B3. POST /api/videos/upload/{uploadId}/complete

**描述：** 完成上传。合并所有分片，提取视频文件信息（时长等），持久化 Video 记录，发布 `VideoUploadedEvent`。

**对应需求：** 需求 1.7, 2.1

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| uploadId | string | 上传会话 ID |

**请求 Body：** 无

**响应 `data` 字段：**

| 字段 | 类型 | 说明 |
|------|------|------|
| videoId | string | 生成的视频唯一标识 |
| fileName | string | 文件名 |
| fileSize | long | 文件字节数 |
| format | string | 文件格式 |
| duration | string | 时长，ISO 8601 Duration 格式（如 `PT12M34S`） |
| status | string | 固定为 `UPLOADED` |
| createdAt | string | ISO 8601 |

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "videoId": "vid_abc123",
    "fileName": "红烧肉教程.mp4",
    "fileSize": 1073741824,
    "format": "MP4",
    "duration": "PT12M34S",
    "status": "UPLOADED",
    "createdAt": "2024-01-15T10:35:00Z"
  },
  "timestamp": "2024-01-15T10:35:00Z"
}
```

**错误码：** `1003` UPLOAD_SESSION_NOT_FOUND, `1007` UPLOAD_NOT_COMPLETE

---

#### B4. GET /api/videos/upload/{uploadId}/progress

**描述：** 查询上传进度。前端轮询此接口展示上传进度条。

**对应需求：** 需求 1.2

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| uploadId | string | 上传会话 ID |

**响应 `data` 字段：**

| 字段 | 类型 | 说明 |
|------|------|------|
| uploadId | string | 上传会话 ID |
| uploadedChunks | int | 已上传分片数 |
| totalChunks | int | 总分片数 |
| progressPercent | int | 进度百分比 0-100 |
| status | string | 会话状态：`ACTIVE` / `COMPLETED` / `EXPIRED` |

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "uploadId": "upl_x7k9m2",
    "uploadedChunks": 29,
    "totalChunks": 64,
    "progressPercent": 45,
    "status": "ACTIVE"
  },
  "timestamp": "2024-01-15T10:32:00Z"
}
```

**错误码：** `1003` UPLOAD_SESSION_NOT_FOUND

---

#### B5. GET /api/videos

**描述：** 分页查询视频列表。支持文件名搜索、状态筛选、日期范围筛选。

**对应UI页面：** grace_video_management

**查询参数：**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | int | 否 | 1 | 页码 |
| pageSize | int | 否 | 20 | 每页条数，最大 100 |
| keyword | string | 否 | - | 按 fileName 模糊搜索 |
| status | string | 否 | - | 状态筛选，多值逗号分隔，如 `UPLOADED,PUBLISHED` |
| startDate | string | 否 | - | 上传起始日期（ISO 8601 date） |
| endDate | string | 否 | - | 上传截止日期（ISO 8601 date） |
| sort | string | 否 | createdAt | 排序字段：`createdAt` / `fileName` / `fileSize` / `duration` |
| order | string | 否 | desc | `asc` / `desc` |

**响应 `data` 字段（分页结构）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| items | array | 视频列表 |
| items[].videoId | string | 视频 ID |
| items[].fileName | string | 文件名 |
| items[].format | string | 文件格式 |
| items[].fileSize | long | 文件字节数 |
| items[].duration | string | 时长 |
| items[].status | string | 视频状态枚举 |
| items[].thumbnailUrl | string / null | 缩略图 URL |
| items[].hasMetadata | boolean | 是否已有元数据 |
| items[].createdAt | string | 上传时间 |
| items[].updatedAt | string | 最后更新时间 |
| total | long | 总记录数 |
| page | int | 当前页 |
| pageSize | int | 每页条数 |
| totalPages | int | 总页数 |

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "items": [
      {
        "videoId": "vid_abc123",
        "fileName": "法式红酒烩牛肉.mp4",
        "format": "MP4",
        "fileSize": 1073741824,
        "duration": "PT12M34S",
        "status": "PUBLISHED",
        "thumbnailUrl": "https://storage.grace.app/thumbnails/vid_abc123.jpg",
        "hasMetadata": true,
        "createdAt": "2023-10-24T14:30:00Z",
        "updatedAt": "2023-10-25T09:00:00Z"
      }
    ],
    "total": 24,
    "page": 1,
    "pageSize": 20,
    "totalPages": 2
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

**错误码：** 无特殊错误码

---

#### B6. GET /api/videos/{videoId}

**描述：** 获取单个视频详情，包含关联的元数据和发布记录。

**对应UI页面：** grace_metadata_review, grace_distribution_promotion

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| videoId | string | 视频 ID |

**响应 `data` 字段：**

| 字段 | 类型 | 说明 |
|------|------|------|
| videoId | string | 视频 ID |
| fileName | string | 文件名 |
| format | string | 文件格式 |
| fileSize | long | 文件字节数 |
| duration | string | 时长 |
| filePath | string | 服务器文件路径 |
| status | string | 视频状态枚举 |
| thumbnailUrl | string / null | 缩略图 URL |
| createdAt | string | 上传时间 |
| updatedAt | string | 最后更新时间 |
| metadata | object / null | 关联的元数据（如存在） |
| metadata.metadataId | string | 元数据 ID |
| metadata.title | string | 标题 |
| metadata.description | string | 描述 |
| metadata.tags | array of string | 标签列表 |
| metadata.source | string | 来源：`AI_GENERATED` / `MANUAL` / `AI_EDITED` |
| metadata.createdAt | string | 创建时间 |
| metadata.updatedAt | string | 更新时间 |
| publishRecords | array | 发布记录列表 |
| publishRecords[].publishRecordId | string | 发布记录 ID |
| publishRecords[].platform | string | 平台标识 |
| publishRecords[].status | string | 发布状态枚举 |
| publishRecords[].videoUrl | string / null | 发布后视频链接 |
| publishRecords[].progressPercent | int | 上传进度 0-100 |
| publishRecords[].errorMessage | string / null | 错误信息 |
| publishRecords[].publishedAt | string / null | 发布时间 |
| publishRecords[].createdAt | string | 记录创建时间 |

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "videoId": "vid_abc123",
    "fileName": "红烧肉教程.mp4",
    "format": "MP4",
    "fileSize": 1073741824,
    "duration": "PT12M34S",
    "filePath": "/data/videos/vid_abc123.mp4",
    "status": "PUBLISHED",
    "thumbnailUrl": "https://storage.grace.app/thumbnails/vid_abc123.jpg",
    "createdAt": "2023-10-24T14:30:00Z",
    "updatedAt": "2023-10-25T09:00:00Z",
    "metadata": {
      "metadataId": "meta_def456",
      "title": "秘制红烧肉：入口即化的家常美味",
      "description": "本期视频为大家带来一道经典的红烧肉教程...",
      "tags": ["美食", "红烧肉", "中华料理", "烹饪教程", "家常菜"],
      "source": "AI_EDITED",
      "createdAt": "2023-10-24T15:00:00Z",
      "updatedAt": "2023-10-24T16:30:00Z"
    },
    "publishRecords": [
      {
        "publishRecordId": "pub_ghi789",
        "platform": "youtube",
        "status": "COMPLETED",
        "videoUrl": "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
        "progressPercent": 100,
        "errorMessage": null,
        "publishedAt": "2023-10-25T09:00:00Z",
        "createdAt": "2023-10-25T08:50:00Z"
      }
    ]
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

**错误码：** `1008` VIDEO_NOT_FOUND

---

### C. 元数据管理 (Metadata)

---

#### C1. POST /api/metadata/generate

**描述：** 根据视频信息调用阿里云 LLM 生成元数据（标题、描述、标签）。通常由 `VideoUploadedEvent` 自动触发，也可由前端手动调用。

**对应需求：** 需求 2.1, 2.2, 2.3, 2.4, 2.5, 2.7

**请求 Body：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| videoId | string | 是 | 目标视频 ID |

```json
{
  "videoId": "vid_abc123"
}
```

**响应 `data` 字段：**

| 字段 | 类型 | 说明 |
|------|------|------|
| metadataId | string | 元数据 ID |
| videoId | string | 关联视频 ID |
| title | string | 生成的标题（≤100 字符） |
| description | string | 生成的描述（≤5000 字符） |
| tags | array of string | 生成的标签（5-15 个） |
| source | string | 固定为 `AI_GENERATED` |
| createdAt | string | ISO 8601 |
| updatedAt | string | ISO 8601 |

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "metadataId": "meta_def456",
    "videoId": "vid_abc123",
    "title": "秘制红烧肉：入口即化的家常美味",
    "description": "本期视频为大家带来一道经典的红烧肉教程，选用五花肉慢炖两小时，搭配冰糖、老抽、料酒...",
    "tags": ["美食", "红烧肉", "中华料理", "烹饪教程", "家常菜", "五花肉", "慢炖"],
    "source": "AI_GENERATED",
    "createdAt": "2024-01-15T10:36:00Z",
    "updatedAt": "2024-01-15T10:36:00Z"
  },
  "timestamp": "2024-01-15T10:36:00Z"
}
```

**错误码：** `1008` VIDEO_NOT_FOUND, `2004` VIDEO_NOT_UPLOADED, `9001` LLM_SERVICE_UNAVAILABLE

---

#### C2. PUT /api/metadata/{id}

**描述：** 用户编辑更新元数据。更新后 source 变为 `AI_EDITED`（原为 AI_GENERATED 时）或 `MANUAL`。

**对应需求：** 需求 3.2, 3.3, 3.5

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| id | string | 元数据 ID |

**请求 Body：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | string | 否 | 新标题，≤100 字符 |
| description | string | 否 | 新描述，≤5000 字符 |
| tags | array of string | 否 | 新标签列表，5-15 个 |

```json
{
  "title": "经典红烧肉：三步搞定的家常硬菜",
  "tags": ["美食", "红烧肉", "中华料理", "烹饪教程", "家常菜", "下饭菜"]
}
```

**响应 `data` 字段：** 同 C1 响应中的元数据结构

**错误码：** `2001` INVALID_METADATA, `2002` METADATA_NOT_FOUND, `2003` METADATA_ALREADY_CONFIRMED

---

#### C3. POST /api/metadata/{id}/regenerate

**描述：** 重新调用 LLM 生成元数据，覆盖当前版本。

**对应需求：** 需求 3.6

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| id | string | 元数据 ID |

**请求 Body：** 无

**响应 `data` 字段：** 同 C1 响应中的元数据结构，source 重置为 `AI_GENERATED`

**错误码：** `2002` METADATA_NOT_FOUND, `2003` METADATA_ALREADY_CONFIRMED, `9001` LLM_SERVICE_UNAVAILABLE

---

#### C4. POST /api/metadata/{id}/confirm

**描述：** 确认元数据。将视频状态更新为 `READY_TO_PUBLISH`，发布 `MetadataConfirmedEvent`。此操作不可逆。

**对应需求：** 需求 3.4

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| id | string | 元数据 ID |

**请求 Body：** 无

**响应 `data` 字段：** 同 C1 响应中的元数据结构

**错误码：** `2001` INVALID_METADATA（元数据不完整）, `2002` METADATA_NOT_FOUND, `2003` METADATA_ALREADY_CONFIRMED

---

#### C5. GET /api/metadata/video/{videoId}

**描述：** 根据视频 ID 获取关联的最新元数据。适用于仅需刷新元数据的场景（如重新生成后）。

**对应UI页面：** grace_metadata_review

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| videoId | string | 视频 ID |

**响应 `data` 字段：** 同 C1 响应中的元数据结构；如无元数据则 `data` 为 `null`

**错误码：** `1008` VIDEO_NOT_FOUND

---

### D. 视频分发 (Distribution)

---

#### D1. POST /api/distribution/publish

**描述：** 发布视频到指定平台。通过 `platform` 字段路由到对应的 VideoDistributor 实现。

**对应需求：** 需求 4.1, 4.2, 4.6, 4.7

**请求 Body：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| videoId | string | 是 | 视频 ID |
| metadataId | string | 是 | 元数据 ID |
| platform | string | 是 | 目标平台标识，如 `youtube` |
| privacyStatus | string | 否 | 视频隐私状态：`public`(默认) / `unlisted` / `private` |

```json
{
  "videoId": "vid_abc123",
  "metadataId": "meta_def456",
  "platform": "youtube",
  "privacyStatus": "public"
}
```

**响应 `data` 字段：**

| 字段 | 类型 | 说明 |
|------|------|------|
| publishRecordId | string | 发布记录 ID |
| videoId | string | 视频 ID |
| platform | string | 平台标识 |
| uploadTaskId | string | 上传任务 ID（用于查询发布状态） |
| status | string | 初始状态：`PENDING` 或 `UPLOADING` |
| createdAt | string | ISO 8601 |

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "publishRecordId": "pub_ghi789",
    "videoId": "vid_abc123",
    "platform": "youtube",
    "uploadTaskId": "task_jkl012",
    "status": "UPLOADING",
    "createdAt": "2024-01-15T11:00:00Z"
  },
  "timestamp": "2024-01-15T11:00:00Z"
}
```

**错误码：** `1008` VIDEO_NOT_FOUND, `2002` METADATA_NOT_FOUND, `3001` UNSUPPORTED_PLATFORM, `3003` PLATFORM_NOT_AUTHORIZED, `3005` VIDEO_NOT_READY

---

#### D2. GET /api/distribution/status/{taskId}

**描述：** 查询发布上传状态。前端轮询此接口展示发布进度。

**对应需求：** 需求 4.3, 4.4, 4.5

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| taskId | string | 上传任务 ID |

**响应 `data` 字段：**

| 字段 | 类型 | 说明 |
|------|------|------|
| publishRecordId | string | 发布记录 ID |
| taskId | string | 上传任务 ID |
| platform | string | 平台标识 |
| status | string | `PENDING` / `UPLOADING` / `COMPLETED` / `FAILED` / `QUOTA_EXCEEDED` |
| progressPercent | int | 进度 0-100 |
| videoUrl | string / null | 发布成功后的视频链接 |
| errorMessage | string / null | 错误信息 |
| publishedAt | string / null | 发布完成时间 |

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "publishRecordId": "pub_ghi789",
    "taskId": "task_jkl012",
    "platform": "youtube",
    "status": "UPLOADING",
    "progressPercent": 65,
    "videoUrl": null,
    "errorMessage": null,
    "publishedAt": null
  },
  "timestamp": "2024-01-15T11:05:00Z"
}
```

**错误码：** `3006` PUBLISH_TASK_NOT_FOUND

---

#### D3. POST /api/distribution/auth/{platform}

**描述：** 发起平台 OAuth 授权。返回 OAuth 授权 URL，前端引导用户跳转。

**对应需求：** 需求 4.2（YouTube OAuth 2.0）

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| platform | string | 平台标识，如 `youtube` |

**请求 Body：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| redirectUri | string | 是 | OAuth 授权完成后回调的前端 URL |

```json
{
  "redirectUri": "https://grace.app/settings/oauth/callback"
}
```

**响应 `data` 字段：**

| 字段 | 类型 | 说明 |
|------|------|------|
| authUrl | string | 完整的 OAuth 授权 URL |
| state | string | CSRF 防护用的 state 参数 |

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "authUrl": "https://accounts.google.com/o/oauth2/v2/auth?client_id=...&redirect_uri=...&scope=...&state=abc123",
    "state": "abc123"
  },
  "timestamp": "2024-01-15T11:10:00Z"
}
```

**错误码：** `3001` UNSUPPORTED_PLATFORM

---

#### D4. GET /api/distribution/auth/{platform}/callback

**描述：** OAuth 授权回调端点。平台 OAuth 服务器将用户重定向到此 URL 并携带 authorization code。后端用 code 交换 access_token / refresh_token 并加密存储。

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| platform | string | 平台标识 |

**查询参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| code | string | 是 | 授权码 |
| state | string | 是 | CSRF state 校验 |

**响应：** HTTP 302 重定向回前端页面，URL 携带 `?auth=success` 或 `?auth=failed&reason=...`

**错误码：** `3001` UNSUPPORTED_PLATFORM, `3007` PLATFORM_API_ERROR（授权码交换失败）

---

#### D5. GET /api/distribution/platforms

**描述：** 获取所有可用的分发平台列表及其 OAuth 授权状态。

**对应UI页面：** grace_distribution_promotion（Step 2 选择平台）

**请求参数：** 无

**响应 `data` 字段（array）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| platform | string | 平台标识 |
| displayName | string | 显示名称 |
| authorized | boolean | 是否已完成 OAuth 授权 |
| authExpired | boolean | 授权是否过期 |
| enabled | boolean | 平台是否可用（false 表示"即将支持"） |

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "platform": "youtube",
      "displayName": "YouTube",
      "authorized": true,
      "authExpired": false,
      "enabled": true
    },
    {
      "platform": "douyin",
      "displayName": "抖音",
      "authorized": false,
      "authExpired": false,
      "enabled": false
    },
    {
      "platform": "bilibili",
      "displayName": "B站",
      "authorized": false,
      "authExpired": false,
      "enabled": false
    },
    {
      "platform": "xiaohongshu",
      "displayName": "小红书",
      "authorized": false,
      "authExpired": false,
      "enabled": false
    }
  ],
  "timestamp": "2024-01-15T11:15:00Z"
}
```

**错误码：** 无特殊错误码

---

#### D6. GET /api/distribution/records/{videoId}

**描述：** 获取指定视频的所有发布记录。

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| videoId | string | 视频 ID |

**响应 `data` 字段（array）：** 每条记录结构同 B6 中 `publishRecords[]` 字段

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "publishRecordId": "pub_ghi789",
      "platform": "youtube",
      "status": "COMPLETED",
      "videoUrl": "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
      "progressPercent": 100,
      "errorMessage": null,
      "publishedAt": "2023-10-25T09:00:00Z",
      "createdAt": "2023-10-25T08:50:00Z"
    }
  ],
  "timestamp": "2024-01-15T11:20:00Z"
}
```

**错误码：** `1008` VIDEO_NOT_FOUND

---

### E. 推广渠道 (Channel)

---

#### E1. POST /api/channels

**描述：** 创建推广渠道。API Key 以 AES-256-GCM 加密后存储。

**对应需求：** 需求 5.1, 5.6

**请求 Body：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | string | 是 | 渠道名称 |
| type | string | 是 | 渠道类型：`SOCIAL_MEDIA` / `FORUM` / `BLOG` / `OTHER` |
| channelUrl | string | 是 | 渠道 URL |
| apiKey | string | 否 | API Key 明文（HTTPS 传输加密，存储 AES-256-GCM 加密） |
| priority | int | 否 | 优先级（默认 1），数值越小优先级越高，范围 1-99 |

```json
{
  "name": "Weibo / 微博",
  "type": "SOCIAL_MEDIA",
  "channelUrl": "https://weibo.com",
  "apiKey": "sk-weibo-xxxxxxxxxxxx",
  "priority": 1
}
```

**响应 `data` 字段：**

| 字段 | 类型 | 说明 |
|------|------|------|
| channelId | string | 渠道 ID |
| name | string | 渠道名称 |
| type | string | 渠道类型 |
| channelUrl | string | 渠道 URL |
| hasApiKey | boolean | 是否已配置 API Key（**不返回明文**） |
| priority | int | 优先级 |
| status | string | 默认 `ENABLED` |
| createdAt | string | ISO 8601 |
| updatedAt | string | ISO 8601 |

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "channelId": "ch_001",
    "name": "Weibo / 微博",
    "type": "SOCIAL_MEDIA",
    "channelUrl": "https://weibo.com",
    "hasApiKey": true,
    "priority": 1,
    "status": "ENABLED",
    "createdAt": "2024-01-15T12:00:00Z",
    "updatedAt": "2024-01-15T12:00:00Z"
  },
  "timestamp": "2024-01-15T12:00:00Z"
}
```

**错误码：** `4002` INVALID_CHANNEL_CONFIG

---

#### E2. PUT /api/channels/{id}

**描述：** 更新推广渠道配置。支持部分更新，包括启用/禁用切换。

**对应需求：** 需求 5.2, 5.3

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| id | string | 渠道 ID |

**请求 Body：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | string | 否 | 渠道名称 |
| type | string | 否 | 渠道类型 |
| channelUrl | string | 否 | 渠道 URL |
| apiKey | string | 否 | 新的 API Key |
| priority | int | 否 | 优先级 |
| status | string | 否 | `ENABLED` / `DISABLED` |

**响应 `data` 字段：** 同 E1 响应中的 Channel 结构

**错误码：** `4001` CHANNEL_NOT_FOUND, `4002` INVALID_CHANNEL_CONFIG

---

#### E3. DELETE /api/channels/{id}

**描述：** 删除推广渠道。如有关联推广记录则软删除（标记为 DISABLED）。

**对应需求：** 需求 5.2

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| id | string | 渠道 ID |

**请求 Body：** 无

**响应：** HTTP 204 No Content，`data` 为 `null`

**错误码：** `4001` CHANNEL_NOT_FOUND

---

#### E4. GET /api/channels

**描述：** 列出所有推广渠道。

**对应需求：** 需求 5.1

**查询参数：**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | string | 否 | 筛选：`ENABLED` / `DISABLED` |
| type | string | 否 | 筛选渠道类型 |

**响应 `data` 字段（array）：** 每条为 Channel 结构，同 E1 响应

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "channelId": "ch_001",
      "name": "Weibo / 微博",
      "type": "SOCIAL_MEDIA",
      "channelUrl": "https://weibo.com",
      "hasApiKey": true,
      "priority": 1,
      "status": "ENABLED",
      "createdAt": "2024-01-15T12:00:00Z",
      "updatedAt": "2024-01-15T12:00:00Z"
    },
    {
      "channelId": "ch_002",
      "name": "Reddit",
      "type": "FORUM",
      "channelUrl": "https://reddit.com",
      "hasApiKey": true,
      "priority": 2,
      "status": "ENABLED",
      "createdAt": "2024-01-15T12:05:00Z",
      "updatedAt": "2024-01-15T12:05:00Z"
    }
  ],
  "timestamp": "2024-01-15T12:10:00Z"
}
```

**错误码：** 无特殊错误码

---

#### E5. GET /api/channels/{id}

**描述：** 获取单个渠道详情。

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| id | string | 渠道 ID |

**响应 `data` 字段：** 同 E1 响应中的 Channel 结构

**错误码：** `4001` CHANNEL_NOT_FOUND

---

### F. 推广任务 (Promotion)

---

#### F1. POST /api/promotions/generate-copy

**描述：** 为指定视频的推广渠道生成 AI 推广文案。调用阿里云 LLM，根据视频元数据和渠道特性生成定制化内容。

**对应需求：** 需求 6.1, 6.2, 6.3, 6.4

**请求 Body：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| videoId | string | 是 | 已发布的视频 ID |
| channelIds | array of string | 否 | 指定渠道列表；为空则为所有 ENABLED 渠道生成 |

```json
{
  "videoId": "vid_abc123",
  "channelIds": ["ch_001", "ch_002", "ch_003"]
}
```

**响应 `data` 字段（array）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| channelId | string | 渠道 ID |
| channelName | string | 渠道名称 |
| channelType | string | 渠道类型 |
| promotionTitle | string | AI 生成的推广标题 |
| promotionBody | string | AI 生成的推广正文（含视频 URL） |
| recommendedMethod | string | 推荐方式：`POST` / `COMMENT` / `SHARE` |
| methodReason | string | 推荐理由 |

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "channelId": "ch_001",
      "channelName": "Weibo / 微博",
      "channelType": "SOCIAL_MEDIA",
      "promotionTitle": "这道秘制红烧肉，好吃到舔盘！",
      "promotionBody": "在这里分享我的私藏菜谱！今天教大家做超级入味的红烧肉，入口即化，肥而不腻。点击链接看完整教程：https://youtube.com/watch?v=xxx #美食分享 #红烧肉教程",
      "recommendedMethod": "POST",
      "methodReason": "微博用户偏好图文+短视频原创帖，POST 方式覆盖面最广"
    },
    {
      "channelId": "ch_002",
      "channelName": "Reddit",
      "channelType": "FORUM",
      "promotionTitle": "Secrets to the perfect Braised Pork Belly (Dongpo Rou)",
      "promotionBody": "Hey r/Cooking! Just wanted to share my technique for traditional braised pork...",
      "recommendedMethod": "POST",
      "methodReason": "Reddit 论坛适合发布详细讨论帖，THREAD 形式利于社区互动"
    }
  ],
  "timestamp": "2024-01-15T13:00:00Z"
}
```

**错误码：** `1008` VIDEO_NOT_FOUND, `9001` LLM_SERVICE_UNAVAILABLE

---

#### F2. POST /api/promotions/execute

**描述：** 批量执行推广任务。按渠道优先级依次通过 OpenCrawl 发布推广内容。单个渠道失败不中断整体流程。

**对应需求：** 需求 7.1, 7.2, 7.3, 7.4, 7.5, 7.6

**请求 Body：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| videoId | string | 是 | 视频 ID |
| promotionItems | array | 是 | 推广任务列表 |
| promotionItems[].channelId | string | 是 | 渠道 ID |
| promotionItems[].promotionTitle | string | 是 | 推广标题（可经用户编辑） |
| promotionItems[].promotionBody | string | 是 | 推广正文（可经用户编辑） |
| promotionItems[].method | string | 是 | 推广方式：`POST` / `COMMENT` / `SHARE` |

```json
{
  "videoId": "vid_abc123",
  "promotionItems": [
    {
      "channelId": "ch_001",
      "promotionTitle": "这道秘制红烧肉，好吃到舔盘！",
      "promotionBody": "在这里分享我的私藏菜谱...",
      "method": "POST"
    },
    {
      "channelId": "ch_002",
      "promotionTitle": "Secrets to the perfect Braised Pork Belly",
      "promotionBody": "Hey r/Cooking! Just wanted to share...",
      "method": "POST"
    }
  ]
}
```

**响应 `data` 字段（array）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| promotionRecordId | string | 推广记录 ID |
| channelId | string | 渠道 ID |
| channelName | string | 渠道名称 |
| method | string | 推广方式 |
| status | string | `COMPLETED` / `FAILED` |
| resultUrl | string / null | 推广发布链接 |
| errorMessage | string / null | 错误信息 |
| executedAt | string | 执行时间 |

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "promotionRecordId": "promo_001",
      "channelId": "ch_001",
      "channelName": "Weibo / 微博",
      "method": "POST",
      "status": "COMPLETED",
      "resultUrl": "https://weibo.com/1234567890/xxxxxx",
      "errorMessage": null,
      "executedAt": "2024-01-15T13:10:00Z"
    },
    {
      "promotionRecordId": "promo_002",
      "channelId": "ch_002",
      "channelName": "Reddit",
      "method": "POST",
      "status": "FAILED",
      "resultUrl": null,
      "errorMessage": "OpenCrawl execution timeout after 30s",
      "executedAt": "2024-01-15T13:10:05Z"
    }
  ],
  "timestamp": "2024-01-15T13:10:10Z"
}
```

**错误码：** `1008` VIDEO_NOT_FOUND, `4001` CHANNEL_NOT_FOUND, `4003` CHANNEL_DISABLED

---

#### F3. GET /api/promotions/history/{videoId}

**描述：** 分页查询指定视频的推广历史记录。

**对应UI页面：** grace_promotion_history

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| videoId | string | 视频 ID |

**查询参数：**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| page | int | 否 | 1 | 页码 |
| pageSize | int | 否 | 20 | 每页条数 |
| status | string | 否 | - | 状态筛选 |
| channelId | string | 否 | - | 渠道筛选 |
| startDate | string | 否 | - | 执行起始日期 |
| endDate | string | 否 | - | 执行截止日期 |

**响应 `data` 字段（分页结构）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| items | array | 推广记录列表 |
| items[].promotionRecordId | string | 推广记录 ID |
| items[].videoId | string | 视频 ID |
| items[].channelId | string | 渠道 ID |
| items[].channelName | string | 渠道名称 |
| items[].channelType | string | 渠道类型 |
| items[].promotionTitle | string | 推广标题 |
| items[].promotionBody | string | 推广正文 |
| items[].method | string | 推广方式 |
| items[].status | string | `PENDING` / `EXECUTING` / `COMPLETED` / `FAILED` |
| items[].resultUrl | string / null | 推广发布链接 |
| items[].errorMessage | string / null | 错误信息 |
| items[].executedAt | string / null | 执行时间 |
| items[].createdAt | string | 记录创建时间 |
| total | long | 总记录数 |
| page | int | 当前页 |
| pageSize | int | 每页条数 |
| totalPages | int | 总页数 |

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "items": [
      {
        "promotionRecordId": "promo_001",
        "videoId": "vid_abc123",
        "channelId": "ch_001",
        "channelName": "Weibo / 微博",
        "channelType": "SOCIAL_MEDIA",
        "promotionTitle": "这道秘制红烧肉，好吃到舔盘！",
        "promotionBody": "在这里分享我的私藏菜谱...",
        "method": "POST",
        "status": "COMPLETED",
        "resultUrl": "https://weibo.com/1234567890/xxxxxx",
        "errorMessage": null,
        "executedAt": "2024-01-15T13:10:00Z",
        "createdAt": "2024-01-15T13:09:50Z"
      }
    ],
    "total": 15,
    "page": 1,
    "pageSize": 20,
    "totalPages": 1
  },
  "timestamp": "2024-01-15T14:00:00Z"
}
```

**错误码：** `1008` VIDEO_NOT_FOUND

---

#### F4. GET /api/promotions/report/{videoId}

**描述：** 获取指定视频的推广执行报告汇总。

**对应UI页面：** grace_promotion_history（展开行 + Insights 卡片）

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| videoId | string | 视频 ID |

**响应 `data` 字段：**

| 字段 | 类型 | 说明 |
|------|------|------|
| videoId | string | 视频 ID |
| videoTitle | string | 视频标题 |
| totalChannels | int | 总渠道数 |
| successCount | int | 成功数 |
| failedCount | int | 失败数 |
| pendingCount | int | 待执行数 |
| overallSuccessRate | double | 整体成功率（0.0-1.0） |
| channelSummaries | array | 各渠道执行摘要 |
| channelSummaries[].channelId | string | 渠道 ID |
| channelSummaries[].channelName | string | 渠道名称 |
| channelSummaries[].channelType | string | 渠道类型 |
| channelSummaries[].method | string | 推广方式 |
| channelSummaries[].status | string | 执行状态 |
| channelSummaries[].resultUrl | string / null | 发布链接 |
| channelSummaries[].errorMessage | string / null | 错误信息 |
| channelSummaries[].executedAt | string / null | 执行时间 |

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "videoId": "vid_abc123",
    "videoTitle": "秘制红烧肉：入口即化的家常美味",
    "totalChannels": 3,
    "successCount": 2,
    "failedCount": 1,
    "pendingCount": 0,
    "overallSuccessRate": 0.67,
    "channelSummaries": [
      {
        "channelId": "ch_001",
        "channelName": "Weibo / 微博",
        "channelType": "SOCIAL_MEDIA",
        "method": "POST",
        "status": "COMPLETED",
        "resultUrl": "https://weibo.com/1234567890/xxxxxx",
        "errorMessage": null,
        "executedAt": "2024-01-15T13:10:00Z"
      }
    ]
  },
  "timestamp": "2024-01-15T14:30:00Z"
}
```

**错误码：** `1008` VIDEO_NOT_FOUND

---

#### F5. POST /api/promotions/{promotionRecordId}/retry

**描述：** 重试单条失败的推广记录。可选修改文案后重试。

**对应UI页面：** grace_promotion_history（失败行的"重试"按钮）

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| promotionRecordId | string | 推广记录 ID |

**请求 Body：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| promotionTitle | string | 否 | 修改后的推广标题；为空使用原标题 |
| promotionBody | string | 否 | 修改后的推广正文；为空使用原正文 |

```json
{
  "promotionBody": "修改后的推广正文内容..."
}
```

**响应 `data` 字段：** 同 F2 响应中的单条 PromotionResult 结构

**错误码：** `4004` PROMOTION_RECORD_NOT_FOUND, `4003` CHANNEL_DISABLED, `9002` OPENCRAWL_EXECUTION_FAILED

---

### G. 用户设置 (Settings)

---

#### G1. GET /api/settings/profile

**描述：** 获取当前用户资料。

**对应UI页面：** grace_settings_page_updated_sidebar（用户 Profile 区域）

**请求参数：** 无

**响应 `data` 字段：**

| 字段 | 类型 | 说明 |
|------|------|------|
| userId | string | 用户 ID |
| displayName | string | 显示名称 |
| email | string / null | 邮箱 |
| avatarUrl | string / null | 头像 URL |
| createdAt | string | ISO 8601 |

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "userId": "user_001",
    "displayName": "Grace Chef",
    "email": "grace.chef@theculinarycurator.com",
    "avatarUrl": "https://storage.grace.app/avatars/user_001.jpg",
    "createdAt": "2023-09-01T00:00:00Z"
  },
  "timestamp": "2024-01-15T15:00:00Z"
}
```

**错误码：** `5001` PROFILE_NOT_FOUND

---

#### G2. PUT /api/settings/profile

**描述：** 更新用户资料。支持部分更新。

**请求 Body：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| displayName | string | 否 | 显示名称 |
| email | string | 否 | 邮箱 |

```json
{
  "displayName": "Chef Grace",
  "email": "chef@grace.app"
}
```

**响应 `data` 字段：** 同 G1 响应中的 Profile 结构

**错误码：** `5001` PROFILE_NOT_FOUND

---

#### G3. POST /api/settings/profile/avatar

**描述：** 上传用户头像。Content-Type 为 `multipart/form-data`。

**对应UI页面：** grace_settings_page_updated_sidebar（Change Avatar 按钮）

**请求参数（multipart/form-data）：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| avatar | binary | 是 | 头像图片文件（JPG/PNG，最大 2MB） |

**响应 `data` 字段：**

| 字段 | 类型 | 说明 |
|------|------|------|
| avatarUrl | string | 新头像 URL |

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "avatarUrl": "https://storage.grace.app/avatars/user_001_v2.jpg"
  },
  "timestamp": "2024-01-15T15:05:00Z"
}
```

**错误码：** `5001` PROFILE_NOT_FOUND

---

#### G4. GET /api/settings/connected-accounts

**描述：** 获取已连接的第三方平台账户列表。数据来源为 OAuthToken 表。

**对应UI页面：** grace_settings_page_updated_sidebar（Connected Accounts 区域）

**请求参数：** 无

**响应 `data` 字段（array）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| platform | string | 平台标识 |
| displayName | string | 平台显示名 |
| authorized | boolean | 是否已授权 |
| accountName | string / null | 连接的账户名 |
| connectedAt | string / null | 连接时间 |

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "platform": "youtube",
      "displayName": "YouTube",
      "authorized": true,
      "accountName": "Grace Cooking Channel",
      "connectedAt": "2023-10-01T08:00:00Z"
    },
    {
      "platform": "weibo",
      "displayName": "Weibo",
      "authorized": false,
      "accountName": null,
      "connectedAt": null
    },
    {
      "platform": "bilibili",
      "displayName": "Bilibili",
      "authorized": false,
      "accountName": null,
      "connectedAt": null
    }
  ],
  "timestamp": "2024-01-15T15:10:00Z"
}
```

**错误码：** 无特殊错误码

---

#### G5. DELETE /api/settings/connected-accounts/{platform}

**描述：** 断开与指定平台的 OAuth 连接。删除存储的 OAuth Token。

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| platform | string | 平台标识 |

**请求 Body：** 无

**响应：** HTTP 204 No Content，`data` 为 `null`

**错误码：** `3001` UNSUPPORTED_PLATFORM, `3003` PLATFORM_NOT_AUTHORIZED

---

#### G6. GET /api/settings/notifications

**描述：** 获取通知偏好设置。

**对应UI页面：** grace_settings_page_updated_sidebar（Notification Preferences 区域）

**请求参数：** 无

**响应 `data` 字段：**

| 字段 | 类型 | 说明 |
|------|------|------|
| uploadComplete | boolean | 上传完成时通知 |
| promotionSuccess | boolean | 推广成功时通知 |
| systemUpdates | boolean | 系统更新通知 |

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "uploadComplete": true,
    "promotionSuccess": true,
    "systemUpdates": false
  },
  "timestamp": "2024-01-15T15:15:00Z"
}
```

**错误码：** 无特殊错误码

---

#### G7. PUT /api/settings/notifications

**描述：** 更新通知偏好。支持部分更新。

**请求 Body：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| uploadComplete | boolean | 否 | 上传完成时通知 |
| promotionSuccess | boolean | 否 | 推广成功时通知 |
| systemUpdates | boolean | 否 | 系统更新通知 |

```json
{
  "systemUpdates": true
}
```

**响应 `data` 字段：** 同 G6 响应结构

**错误码：** 无特殊错误码

---

#### G8. POST /api/settings/api-keys

**描述：** 生成平台 API Key。密钥仅在创建时返回一次明文。

**对应UI页面：** grace_settings_page_updated_sidebar（API Management 区域）

**请求 Body：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | string | 是 | Key 用途描述 |
| expiresInDays | int | 否 | 有效期天数（默认 90） |

```json
{
  "name": "Primary API Key",
  "expiresInDays": 90
}
```

**响应 `data` 字段：**

| 字段 | 类型 | 说明 |
|------|------|------|
| apiKeyId | string | API Key ID |
| name | string | 用途描述 |
| key | string | 明文密钥（**仅此一次返回**） |
| prefix | string | 密钥前缀（如 `grc_a1b2...`） |
| expiresAt | string | 过期时间 |
| createdAt | string | 创建时间 |

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "apiKeyId": "key_001",
    "name": "Primary API Key",
    "key": "grc_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6",
    "prefix": "grc_a1b2...o5p6",
    "expiresAt": "2024-04-15T15:20:00Z",
    "createdAt": "2024-01-15T15:20:00Z"
  },
  "timestamp": "2024-01-15T15:20:00Z"
}
```

**错误码：** 无特殊错误码

---

#### G9. GET /api/settings/api-keys

**描述：** 列出所有已创建的 API Key（不含明文）。

**请求参数：** 无

**响应 `data` 字段（array）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| apiKeyId | string | API Key ID |
| name | string | 用途描述 |
| prefix | string | 密钥前缀 |
| expiresAt | string | 过期时间 |
| lastUsedAt | string / null | 最后使用时间 |
| createdAt | string | 创建时间 |

```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "apiKeyId": "key_001",
      "name": "Primary API Key",
      "prefix": "grc_a1b2...o5p6",
      "expiresAt": "2024-04-15T15:20:00Z",
      "lastUsedAt": "2024-01-14T20:00:00Z",
      "createdAt": "2024-01-15T15:20:00Z"
    }
  ],
  "timestamp": "2024-01-15T15:25:00Z"
}
```

**错误码：** 无特殊错误码

---

#### G10. DELETE /api/settings/api-keys/{apiKeyId}

**描述：** 撤销（删除）指定 API Key。

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| apiKeyId | string | API Key ID |

**请求 Body：** 无

**响应：** HTTP 204 No Content，`data` 为 `null`

**错误码：** `5002` API_KEY_NOT_FOUND

---

## 4. 端点汇总表

| 编号 | 方法 | 路径 | 描述 | 来源 |
|------|------|------|------|------|
| A1 | GET | /api/dashboard/overview | 仪表盘概览数据 | 新增 |
| B1 | POST | /api/videos/upload/init | 初始化分片上传 | design.md |
| B2 | POST | /api/videos/upload/{uploadId}/chunk | 上传分片 | design.md |
| B3 | POST | /api/videos/upload/{uploadId}/complete | 完成上传 | design.md |
| B4 | GET | /api/videos/upload/{uploadId}/progress | 查询上传进度 | design.md |
| B5 | GET | /api/videos | 视频列表（分页/搜索/筛选） | 新增 |
| B6 | GET | /api/videos/{videoId} | 视频详情（含元数据+发布记录） | 新增 |
| C1 | POST | /api/metadata/generate | AI 生成元数据 | design.md |
| C2 | PUT | /api/metadata/{id} | 更新元数据 | design.md |
| C3 | POST | /api/metadata/{id}/regenerate | 重新生成元数据 | design.md |
| C4 | POST | /api/metadata/{id}/confirm | 确认元数据 | design.md |
| C5 | GET | /api/metadata/video/{videoId} | 按视频 ID 获取元数据 | 新增 |
| D1 | POST | /api/distribution/publish | 发布视频到平台 | design.md |
| D2 | GET | /api/distribution/status/{taskId} | 查询发布状态 | design.md |
| D3 | POST | /api/distribution/auth/{platform} | 发起 OAuth 授权 | design.md |
| D4 | GET | /api/distribution/auth/{platform}/callback | OAuth 授权回调 | 新增 |
| D5 | GET | /api/distribution/platforms | 可用平台列表及授权状态 | 新增 |
| D6 | GET | /api/distribution/records/{videoId} | 视频发布记录列表 | 新增 |
| E1 | POST | /api/channels | 创建推广渠道 | design.md |
| E2 | PUT | /api/channels/{id} | 更新推广渠道 | design.md |
| E3 | DELETE | /api/channels/{id} | 删除推广渠道 | design.md |
| E4 | GET | /api/channels | 列出所有推广渠道 | design.md |
| E5 | GET | /api/channels/{id} | 获取单个渠道详情 | 新增 |
| F1 | POST | /api/promotions/generate-copy | AI 生成推广文案 | design.md |
| F2 | POST | /api/promotions/execute | 批量执行推广 | design.md |
| F3 | GET | /api/promotions/history/{videoId} | 推广历史（分页） | design.md |
| F4 | GET | /api/promotions/report/{videoId} | 推广执行报告 | design.md |
| F5 | POST | /api/promotions/{promotionRecordId}/retry | 重试失败推广 | 新增 |
| G1 | GET | /api/settings/profile | 获取用户资料 | 新增 |
| G2 | PUT | /api/settings/profile | 更新用户资料 | 新增 |
| G3 | POST | /api/settings/profile/avatar | 上传用户头像 | 新增 |
| G4 | GET | /api/settings/connected-accounts | 已连接账户列表 | 新增 |
| G5 | DELETE | /api/settings/connected-accounts/{platform} | 断开平台连接 | 新增 |
| G6 | GET | /api/settings/notifications | 获取通知偏好 | 新增 |
| G7 | PUT | /api/settings/notifications | 更新通知偏好 | 新增 |
| G8 | POST | /api/settings/api-keys | 创建 API Key | 新增 |
| G9 | GET | /api/settings/api-keys | 列出 API Keys | 新增 |
| G10 | DELETE | /api/settings/api-keys/{apiKeyId} | 撤销 API Key | 新增 |

---

## 5. 枚举值参考

### VideoFormat

| 值 | 说明 |
|------|------|
| MP4 | MPEG-4 |
| MOV | QuickTime |
| AVI | Audio Video Interleave |
| MKV | Matroska |

### VideoStatus

| 值 | 说明 |
|------|------|
| UPLOADED | 已上传，待生成元数据 |
| METADATA_GENERATED | 元数据已生成，待审核 |
| READY_TO_PUBLISH | 元数据已确认，待发布 |
| PUBLISHING | 发布中 |
| PUBLISHED | 已发布成功 |
| PUBLISH_FAILED | 发布失败 |
| PROMOTION_DONE | 推广完成 |

### UploadSessionStatus

| 值 | 说明 |
|------|------|
| ACTIVE | 上传进行中 |
| COMPLETED | 上传已完成 |
| EXPIRED | 会话已过期 |

### MetadataSource

| 值 | 说明 |
|------|------|
| AI_GENERATED | AI 自动生成 |
| MANUAL | 用户手动填写 |
| AI_EDITED | AI 生成后经用户编辑 |

### PublishStatus

| 值 | 说明 |
|------|------|
| PENDING | 待处理 |
| UPLOADING | 上传中 |
| COMPLETED | 发布成功 |
| FAILED | 发布失败 |
| QUOTA_EXCEEDED | 平台配额超限 |

### ChannelType

| 值 | 说明 |
|------|------|
| SOCIAL_MEDIA | 社交媒体（如微博） |
| FORUM | 论坛（如 Reddit） |
| BLOG | 博客（如豆瓣） |
| OTHER | 其他 |

### ChannelStatus

| 值 | 说明 |
|------|------|
| ENABLED | 已启用 |
| DISABLED | 已禁用 |

### PromotionMethod

| 值 | 说明 |
|------|------|
| POST | 发帖 |
| COMMENT | 评论 |
| SHARE | 分享/转发 |

### PromotionStatus

| 值 | 说明 |
|------|------|
| PENDING | 待执行 |
| EXECUTING | 执行中 |
| COMPLETED | 执行成功 |
| FAILED | 执行失败 |

---

## 6. 同步状态

本文档所有接口定义已与 `design.md` 和 `requirements.md` 完成同步。

| 目标文档 | 同步内容 | 状态 |
|---------|---------|------|
| design.md | Architecture：新增 User/Settings Context 限界上下文 | 已同步 |
| design.md | Components：新增 DashboardController、SettingsController、VideoQuery/Metadata/Distribution/Promotion 补充端点 | 已同步 |
| design.md | Data Models：新增 UserProfile、NotificationPreference、ApiKey 实体及 ER 图 | 已同步 |
| design.md | Error Handling：补充 ProfileNotFoundException、ApiKeyNotFoundException | 已同步 |
| requirements.md | 需求 9：仪表盘数据概览（统计卡片、最近上传、发布分布图、推广概览、时间范围筛选） | 已同步 |
| requirements.md | 需求 10：用户设置与账户管理（资料 CRUD、头像上传、已连接账户、通知偏好、API Key 生命周期） | 已同步 |
