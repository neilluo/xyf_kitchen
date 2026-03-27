package com.grace.platform.video.application.dto;

import java.time.LocalDateTime;

/**
 * 初始化上传响应 DTO。
 * <p>
 * 对应 API B1 响应的 data 字段。
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
public record UploadInitDTO(
    String uploadId,
    int totalChunks,
    long chunkSize,
    LocalDateTime expiresAt
) {
}
