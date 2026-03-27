package com.grace.platform.video.application.dto;

import com.grace.platform.video.domain.UploadSessionStatus;

/**
 * 上传进度响应 DTO。
 * <p>
 * 对应 API B4 响应的 data 字段。
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
public record UploadProgressDTO(
    String uploadId,
    int uploadedChunks,
    int totalChunks,
    int progressPercent,
    UploadSessionStatus status
) {
}
