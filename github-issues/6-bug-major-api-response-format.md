## 🟠 后端 API 未实现统一响应格式 ApiResponse

**严重程度:** MAJOR  
**类别:** bug  
**发现时间:** 2026-03-28  
**发现者:** Kelly (QA Lead)  
**影响范围:** 所有 API 接口、前端错误处理  

### 问题描述

根据 api.md §2.1 约定，所有接口应返回统一的响应信封结构（ApiResponse），包含 code、message、data、timestamp 字段。但实际实现可能未遵循此约定，导致前端无法统一处理响应和错误。

### 预期响应格式

**成功响应：**

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

**分页响应：**

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

### 验证方法

```bash
# 测试任意 API 接口
curl -X POST http://localhost:8080/api/videos/upload/init \
  -H "Content-Type: application/json" \
  -d '{"fileName":"test.mp4","fileSize":1024,"format":"MP4"}' \
  | jq .
```

**检查点：**
- [ ] 响应是否包含 `code` 字段（int 类型）
- [ ] 响应是否包含 `message` 字段（string 类型）
- [ ] 响应是否包含 `data` 字段（object/array/null）
- [ ] 响应是否包含 `timestamp` 字段（ISO 8601 格式）
- [ ] 错误响应是否包含 `errors` 字段（可选，字段级校验错误）

### 当前问题

如果 API 未实现统一响应格式，可能出现：

1. **直接返回数据对象**
   ```json
   {
     "uploadId": "upl_x7k9m2",
     "totalChunks": 64
   }
   ```

2. **Spring 默认错误格式**
   ```json
   {
     "timestamp": "2024-01-15T10:30:00.000+00:00",
     "status": 500,
     "error": "Internal Server Error",
     "path": "/api/videos/upload/init"
   }
   ```

3. **格式不一致**
   - 有些接口有 code，有些没有
   - 有些接口有 timestamp，有些没有
   - 错误响应格式不统一

### 建议修复步骤

#### 1. 创建 ApiResponse 类

在 shared/kernel 包中创建：

```java
package com.grace.platform.shared.kernel;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    
    private int code;
    private String message;
    private T data;
    private LocalDateTime timestamp;
    
    // 成功响应
    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(0);
        response.setMessage("success");
        response.setData(data);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }
    
    // 成功响应（自定义消息）
    public static <T> ApiResponse<T> success(T data, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(0);
        response.setMessage(message);
        response.setData(data);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }
    
    // 错误响应
    public static <T> ApiResponse<T> error(int code, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(code);
        response.setMessage(message);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }
    
    // 错误响应（带字段级错误）
    public static <T> ApiResponse<T> error(int code, String message, java.util.List<FieldError> errors) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(code);
        response.setMessage(message);
        response.setTimestamp(LocalDateTime.now());
        // 需要将 errors 放入 data 或单独字段
        return response;
    }
}
```

#### 2. 创建 FieldError 类

```java
package com.grace.platform.shared.kernel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FieldError {
    private String field;
    private String message;
}
```

#### 3. 创建全局异常处理器

```java
package com.grace.platform.shared.kernel;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleBusinessException(BusinessException ex) {
        return ApiResponse.error(ex.getErrorCode().getCode(), ex.getMessage());
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidationException(MethodArgumentNotValidException ex) {
        List<FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> new FieldError(error.getField(), error.getDefaultMessage()))
            .collect(Collectors.toList());
        return ApiResponse.error(400, "参数校验失败", errors);
    }
    
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Void> handleGenericException(Exception ex) {
        return ApiResponse.error(500, "Internal server error: " + ex.getMessage());
    }
}
```

#### 4. 创建 BusinessException 类

```java
package com.grace.platform.shared.kernel;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    
    private final ErrorCode errorCode;
    
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
    
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
```

#### 5. 创建 ErrorCode 枚举

```java
package com.grace.platform.shared.kernel;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    
    // 成功
    SUCCESS(0, "success"),
    
    // 视频上下文 (1001-1099)
    UNSUPPORTED_VIDEO_FORMAT(1001, "不支持的视频格式"),
    VIDEO_FILE_SIZE_EXCEEDED(1002, "文件大小超过限制"),
    UPLOAD_SESSION_NOT_FOUND(1003, "上传会话不存在"),
    UPLOAD_SESSION_EXPIRED(1004, "上传会话已过期"),
    CHUNK_INDEX_OUT_OF_RANGE(1005, "分片索引超出范围"),
    DUPLICATE_CHUNK(1006, "重复的分片"),
    UPLOAD_NOT_COMPLETE(1007, "上传未完成"),
    VIDEO_NOT_FOUND(1008, "视频不存在"),
    
    // 元数据上下文 (2001-2099)
    INVALID_METADATA(2001, "元数据无效"),
    METADATA_NOT_FOUND(2002, "元数据不存在"),
    METADATA_ALREADY_CONFIRMED(2003, "元数据已确认"),
    VIDEO_NOT_UPLOADED(2004, "视频未上传"),
    
    // 分发上下文 (3001-3099)
    UNSUPPORTED_PLATFORM(3001, "不支持的平台"),
    PLATFORM_AUTH_EXPIRED(3002, "平台授权已过期"),
    PLATFORM_NOT_AUTHORIZED(3003, "平台未授权"),
    PLATFORM_QUOTA_EXCEEDED(3004, "平台配额超限"),
    VIDEO_NOT_READY(3005, "视频未准备好"),
    PUBLISH_TASK_NOT_FOUND(3006, "发布任务不存在"),
    PLATFORM_API_ERROR(3007, "平台 API 错误"),
    
    // 推广上下文 (4001-4099)
    CHANNEL_NOT_FOUND(4001, "渠道不存在"),
    INVALID_CHANNEL_CONFIG(4002, "渠道配置无效"),
    CHANNEL_DISABLED(4003, "渠道已禁用"),
    PROMOTION_RECORD_NOT_FOUND(4004, "推广记录不存在"),
    
    // 设置上下文 (5001-5099)
    PROFILE_NOT_FOUND(5001, "用户资料不存在"),
    API_KEY_NOT_FOUND(5002, "API Key 不存在"),
    
    // 基础设施错误 (9001-9099)
    LLM_SERVICE_UNAVAILABLE(9001, "LLM 服务不可用"),
    OPENCRAWL_EXECUTION_FAILED(9002, "OpenCrawl 执行失败"),
    ENCRYPTION_ERROR(9003, "加解密错误"),
    
    // 通用错误
    INTERNAL_SERVER_ERROR(9999, "内部服务器错误");
    
    private final int code;
    private final String message;
}
```

#### 6. 更新所有 Controller

确保所有 Controller 方法返回 `ApiResponse<T>`：

```java
@RestController
@RequestMapping("/api/videos")
public class VideoUploadController {
    
    private final VideoApplicationService videoService;
    
    @PostMapping("/upload/init")
    public ApiResponse<UploadInitResponse> initUpload(@RequestBody UploadInitRequest request) {
        UploadInitDTO result = videoService.initUpload(request);
        return ApiResponse.success(convertToResponse(result));
    }
}
```

### 验收标准

- [ ] 创建 ApiResponse 泛型类
- [ ] 创建 FieldError 类
- [ ] 创建 BusinessException 异常类
- [ ] 创建 ErrorCode 枚举（包含 api.md §2.3 所有错误码）
- [ ] 实现 GlobalExceptionHandler
- [ ] 所有 Controller 返回 ApiResponse<T>
- [ ] 成功响应包含 code=0, message="success", data, timestamp
- [ ] 错误响应包含 code, message, timestamp, errors（可选）
- [ ] 更新 api.md 确认实现完成

### 关联文档

- [api.md §2.1](../api.md) - 统一响应信封
- [api.md §2.3](../api.md) - 错误码体系
- [docs/backend/02-shared-kernel.md](../docs/backend/02-shared-kernel.md) - 共享内核

### 影响范围

**需要修改的文件：**
- shared/kernel/ApiResponse.java（新建）
- shared/kernel/FieldError.java（新建）
- shared/kernel/ErrorCode.java（新建）
- shared/kernel/BusinessException.java（新建）
- shared/kernel/GlobalExceptionHandler.java（新建）
- 所有 Controller 类

**前端影响：**
- 前端 API 调用层需要适配新的响应格式
- 错误处理逻辑需要基于 code 字段判断

---

*由 Kelly (QA Lead) 在回归测试中发现*
