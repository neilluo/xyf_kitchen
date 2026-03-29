package com.grace.platform.video.application.dto;

/**
 * 分片上传响应 DTO。
 * <p>
 * 对应 API B2 响应的 data 字段。
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
public record ChunkUploadDTO(
    String uploadId,
    int chunkIndex,
    int uploadedChunks,
    int totalChunks
) {
}