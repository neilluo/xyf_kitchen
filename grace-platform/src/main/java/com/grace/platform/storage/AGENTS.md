# Storage 模块 - AGENTS.md

## 模块定位

**Storage 模块**是 Grace 平台的**存储基础设施层**，负责提供统一的对象存储抽象，支持多种存储后端（阿里云 OSS、本地文件系统、AWS S3 等）。

## 核心能力

### 1. 对象存储抽象
- 提供统一的 `OssStorageService` 接口，屏蔽底层存储差异
- 支持存储 provider 切换（OSS / LOCAL / S3）
- 统一的存储 URL 格式：`storage://{provider}/{bucket}/{key}`

### 2. STS 临时凭证管理
- 为前端直传 OSS 生成临时 STS 凭证
- 凭证有效期可控（默认 1 小时）
- 最小权限原则（仅允许 PutObject/GetObject）

### 3. 上传回调处理
- 接收 OSS 上传完成回调
- 验证回调签名防篡改
- 触发后续业务处理（元数据生成等）

### 4. 存储操作
- 文件上传/下载
- 分片上传支持
- 预签名 URL 生成
- 文件元信息获取

## 模块边界

### 对外暴露的接口

| 接口 | 说明 | 使用方 |
|------|------|--------|
| `OssStorageService.generateStsToken()` | 生成 STS 临时凭证 | Video 模块（前端直传） |
| `OssStorageService.verifyCallbackSignature()` | 验证 OSS 回调签名 | Video 模块（回调处理） |
| `OssStorageService.uploadFile()` | 上传文件到 OSS | Video 模块（服务端上传） |
| `OssStorageService.getVideoUrl()` | 获取视频访问 URL | Video、Metadata、Distribution 模块 |
| `OssStorageService.deleteFile()` | 删除文件 | Video 模块（清理） |

### 不对外暴露的实现细节

- 阿里云 SDK 的具体使用
- STS 角色的 ARN 和权限策略
- OSS Bucket 的物理结构
- 回调签名的加密算法

## 与其他模块的关系

```
Storage 模块（基础设施层）
    ↑ 被依赖
Video 模块 - 使用 STS 凭证、处理回调、上传文件
    ↑ 触发
Metadata 模块 - 从 OSS 提取视频帧
    ↑ 使用
Distribution 模块 - 使用 OSS URL 分发视频
```

## 核心领域对象

### StsCredentials（STS 临时凭证）
```java
public record StsCredentials(
    String accessKeyId,      // 临时 AccessKey
    String accessKeySecret,  // 临时 Secret
    String securityToken,    // STS Token
    String bucket,           // OSS Bucket
    String region,           // OSS 地域
    LocalDateTime expiresAt  // 过期时间
) {}
```

### StorageUrl（存储 URL）
```java
public record StorageUrl(
    StorageProvider provider,  // OSS / LOCAL / S3
    String bucket,             // Bucket 名称
    String key                 // Object Key
) {
    public String toUrlString() {
        return String.format("storage://%s/%s/%s", provider, bucket, key);
    }
}
```

### UploadCallback（上传回调）
```java
public record UploadCallback(
    String uploadId,      // 上传会话 ID
    String storageKey,    // OSS Object Key
    long fileSize,        // 文件大小
    String etag,          // OSS ETag
    String callbackSignature  // 回调签名
) {}
```

## 配置项

```yaml
grace:
  storage:
    # OSS 配置
    oss:
      endpoint: oss-cn-hangzhou.aliyuncs.com
      bucket: grace-video-storage
      region: cn-hangzhou
      callback-url: http://backend/api/storage/oss/callback
      callback-auth: enabled
    
    # STS 配置
    sts:
      role-arn: acs:ram::xxx:role/grace-oss-sts-role
      duration-seconds: 3600
    
    # 本地存储配置（降级方案）
    local:
      video-dir: ./data/videos
      temp-dir: ./data/temp
```

## 关键设计决策

### 1. 为什么需要 Storage 模块？
- **解耦**：Video 模块不需要关心具体存储实现
- **可替换**：未来可以切换存储提供商（AWS S3、MinIO 等）
- **安全性**：STS 凭证管理集中化，避免密钥泄露

### 2. 为什么前端直传 OSS？
- **降低服务器带宽**：视频流不经过应用服务器
- **提高上传速度**：用户直连 OSS，减少网络跳数
- **水平扩展**：上传压力在 OSS，应用服务器无状态

### 3. 为什么保留服务端上传？
- **API 暴露**：外部服务可以通过 API 上传
- **兼容性**：存量系统迁移时可能需要服务端上传
- **灵活性**：某些场景下服务端上传更合适（如后台导入）

## 使用示例

### 前端直传 OSS
```java
// Video 模块调用 Storage 模块获取 STS 凭证
StsCredentials credentials = ossStorageService.generateStsToken(uploadId, storageKey);
// 返回给前端，前端使用 ali-oss SDK 直传
```

### 服务端上传
```java
// Video 模块调用 Storage 模块上传文件
Path localFile = ...;
String storageKey = "videos/" + videoId + ".mp4";
ossStorageService.uploadFile(localFile, storageKey);
String url = ossStorageService.getVideoUrl(storageKey);
```

## 注意事项

1. **STS 凭证有效期**：默认 1 小时，上传大文件时需要评估是否足够
2. **回调可靠性**：OSS 回调可能丢失，需要配合前端轮询或补偿机制
3. **存储成本**：OSS 存储和流量费用需要监控
4. **权限最小化**：STS 角色只授予必要的权限（PutObject/GetObject）

## 扩展方向

- 支持 AWS S3 存储
- 支持 MinIO 私有部署
- 支持多云存储（主备切换）
- 支持存储分层（热数据/冷数据）
