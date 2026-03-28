## 🟠 错误码体系未实现 - 无法统一错误处理

**严重程度:** MAJOR  
**类别:** bug  
**发现时间:** 2026-03-28  
**发现者:** Kelly (QA Lead)  
**影响范围:** 错误处理、前端错误提示、日志记录  

### 问题描述

api.md §2.3 定义了完整的错误码体系（1001-1099 视频上下文、2001-2099 元数据上下文等），但实际代码中可能未实现这些错误码，导致错误处理不统一，前端无法根据错误码进行精确的错误提示。

### 预期错误码体系

根据 api.md §2.3，应实现以下错误码：

#### 视频上下文（1001-1099）

| 错误码 | HTTP Status | 名称 | 触发条件 |
|--------|-------------|------|---------|
| 1001 | 400 | UNSUPPORTED_VIDEO_FORMAT | 文件格式不在 MP4/MOV/AVI/MKV 列表中 |
| 1002 | 400 | VIDEO_FILE_SIZE_EXCEEDED | 文件大小 > 5GB |
| 1003 | 404 | UPLOAD_SESSION_NOT_FOUND | uploadId 不存在或已过期 |
| 1004 | 400 | UPLOAD_SESSION_EXPIRED | 上传会话已超时 |
| 1005 | 400 | CHUNK_INDEX_OUT_OF_RANGE | chunkIndex 超出 totalChunks 范围 |
| 1006 | 400 | DUPLICATE_CHUNK | 重复上传相同 chunkIndex |
| 1007 | 400 | UPLOAD_NOT_COMPLETE | 尚有分片未上传就调用 complete |
| 1008 | 404 | VIDEO_NOT_FOUND | 视频 ID 不存在 |

#### 元数据上下文（2001-2099）

| 错误码 | HTTP Status | 名称 | 触发条件 |
|--------|-------------|------|---------|
| 2001 | 400 | INVALID_METADATA | 元数据字段约束校验失败 |
| 2002 | 404 | METADATA_NOT_FOUND | 元数据 ID 不存在 |
| 2003 | 409 | METADATA_ALREADY_CONFIRMED | 已确认的元数据不可再编辑 |
| 2004 | 400 | VIDEO_NOT_UPLOADED | 视频尚未上传完成 |

#### 分发上下文（3001-3099）

| 错误码 | HTTP Status | 名称 | 触发条件 |
|--------|-------------|------|---------|
| 3001 | 400 | UNSUPPORTED_PLATFORM | 请求的分发平台未注册 |
| 3002 | 401 | PLATFORM_AUTH_EXPIRED | 平台 OAuth token 过期 |
| 3003 | 400 | PLATFORM_NOT_AUTHORIZED | 平台未授权连接 |
| 3004 | 429 | PLATFORM_QUOTA_EXCEEDED | 平台 API 配额超限 |
| 3005 | 400 | VIDEO_NOT_READY | 视频状态非 READY_TO_PUBLISH |
| 3006 | 404 | PUBLISH_TASK_NOT_FOUND | 发布任务 ID 不存在 |
| 3007 | 502 | PLATFORM_API_ERROR | 平台 API 返回非预期错误 |

#### 推广上下文（4001-4099）

| 错误码 | HTTP Status | 名称 | 触发条件 |
|--------|-------------|------|---------|
| 4001 | 404 | CHANNEL_NOT_FOUND | 推广渠道 ID 不存在 |
| 4002 | 400 | INVALID_CHANNEL_CONFIG | 推广渠道配置校验失败 |
| 4003 | 400 | CHANNEL_DISABLED | 推广渠道已被禁用 |
| 4004 | 404 | PROMOTION_RECORD_NOT_FOUND | 推广记录不存在 |

#### 设置上下文（5001-5099）

| 错误码 | HTTP Status | 名称 | 触发条件 |
|--------|-------------|------|---------|
| 5001 | 404 | PROFILE_NOT_FOUND | 用户资料不存在 |
| 5002 | 404 | API_KEY_NOT_FOUND | API Key 不存在 |

#### 基础设施错误（9001-9099）

| 错误码 | HTTP Status | 名称 | 触发条件 |
|--------|-------------|------|---------|
| 9001 | 503 | LLM_SERVICE_UNAVAILABLE | 阿里云 LLM 服务调用失败 |
| 9002 | 502 | OPENCRAWL_EXECUTION_FAILED | OpenCrawl 执行失败 |
| 9003 | 500 | ENCRYPTION_ERROR | AES 加解密异常 |
| 9999 | 500 | INTERNAL_SERVER_ERROR | 未知内部错误 |

### 验证方法

```bash
# 测试错误码返回
curl -X POST http://localhost:8080/api/videos/upload/init \
  -H "Content-Type: application/json" \
  -d '{"fileName":"test.wmv","fileSize":1024,"format":"WMV"}' \
  | jq .
```

**预期响应：**
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

### 当前问题

如果未实现错误码体系，可能出现：

1. **使用 HTTP Status 代替业务错误码**
   ```json
   {
     "status": 400,
     "error": "Bad Request"
   }
   ```

2. **错误信息不统一**
   - 有些接口返回中文错误
   - 有些接口返回英文错误
   - 错误信息格式不一致

3. **前端无法精确判断错误类型**
   - 只能根据 HTTP Status 粗略判断
   - 无法区分具体的业务错误场景

### 建议修复步骤

#### 1. 创建 ErrorCode 枚举

参考 Issue #6 中的 ErrorCode 实现，确保包含所有 api.md 定义的错误码。

#### 2. 在各领域服务中使用 ErrorCode

```java
// VideoApplicationService.java
public UploadInitDTO initUpload(UploadInitCommand command) {
    // 校验文件格式
    if (!VideoFormat.isValid(command.format())) {
        throw new BusinessException(ErrorCode.UNSUPPORTED_VIDEO_FORMAT, 
            "不支持的视频格式：" + command.format() + "。支持的格式：MP4, MOV, AVI, MKV");
    }
    
    // 校验文件大小
    if (command.fileSize() > MAX_FILE_SIZE) {
        throw new BusinessException(ErrorCode.VIDEO_FILE_SIZE_EXCEEDED,
            "文件大小超过限制（最大 5GB）");
    }
    
    // ... 其他逻辑
}
```

#### 3. 在全局异常处理器中映射 ErrorCode

```java
// GlobalExceptionHandler.java
@ExceptionHandler(BusinessException.class)
@ResponseStatus(HttpStatus.BAD_REQUEST)
public ApiResponse<Void> handleBusinessException(BusinessException ex) {
    return ApiResponse.error(ex.getErrorCode().getCode(), ex.getMessage());
}
```

#### 4. 为不同 HTTP Status 配置映射

```java
// GlobalExceptionHandler.java
@ExceptionHandler(UnsupportedPlatformException.class)
@ResponseStatus(HttpStatus.BAD_REQUEST)
public ApiResponse<Void> handleUnsupportedPlatform(UnsupportedPlatformException ex) {
    return ApiResponse.error(ErrorCode.UNSUPPORTED_PLATFORM.getCode(), ex.getMessage());
}

@ExceptionHandler(AuthExpiredException.class)
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public ApiResponse<Void> handleAuthExpired(AuthExpiredException ex) {
    return ApiResponse.error(ErrorCode.PLATFORM_AUTH_EXPIRED.getCode(), ex.getMessage());
}

@ExceptionHandler(QuotaExceededException.class)
@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public ApiResponse<Void> handleQuotaExceeded(QuotaExceededException ex) {
    return ApiResponse.error(ErrorCode.PLATFORM_QUOTA_EXCEEDED.getCode(), ex.getMessage());
}
```

### 验收标准

- [ ] ErrorCode 枚举包含 api.md §2.3 所有错误码
- [ ] 各领域服务在抛出异常时使用 ErrorCode
- [ ] 全局异常处理器正确映射 ErrorCode 到 HTTP 响应
- [ ] 错误响应包含正确的 code 和 message
- [ ] 前端可以根据 code 精确判断错误类型
- [ ] 更新 api.md 确认实现完成

### 关联文档

- [api.md §2.3](../api.md) - 错误码体系
- [docs/backend/02-shared-kernel.md](../docs/backend/02-shared-kernel.md) - 共享内核
- [design.md §Error Handling](../design.md) - 错误处理策略

### 前端适配

前端 API 层可以根据 code 进行精确的错误处理：

```typescript
// src/api/error-handler.ts
export function handleApiError(code: number, message: string) {
  switch (code) {
    case 1001:
      toast.error('不支持的视频格式，请上传 MP4/MOV/AVI/MKV 格式');
      break;
    case 1002:
      toast.error('文件大小超过 5GB 限制');
      break;
    case 3004:
      toast.error('YouTube API 配额超限，请稍后重试');
      break;
    default:
      toast.error(message || '操作失败');
  }
}
```

---

*由 Kelly (QA Lead) 在回归测试中发现*
