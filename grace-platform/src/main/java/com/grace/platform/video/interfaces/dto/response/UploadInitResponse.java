package com.grace.platform.video.interfaces.dto.response;

import java.time.LocalDateTime;

/**
 * 初始化上传响应 DTO。
 * <p>
 * 对应 API B1 响应的 data 字段。
 * 包含 OSS 直传所需的 STS 临时凭证和上传会话信息。
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
public record UploadInitResponse(
    String uploadId,
    int totalChunks,
    long chunkSize,
    LocalDateTime expiresAt,
    String storageKey,
    String ossBucket,
    StsCredentialsResponse stsCredentials
) {
    /**
     * STS 临时凭证响应
     */
    public record StsCredentialsResponse(
        String accessKeyId,
        String accessKeySecret,
        String securityToken,
        LocalDateTime expiration,
        String region,
        String bucket
    ) {
    }
}
