package com.grace.platform.video.interfaces.dto.response;

import java.time.LocalDateTime;

public record ServerUploadCompleteResponse(
    String videoId,
    String fileName,
    long fileSize,
    String format,
    String duration,
    String status,
    String storageUrl,
    LocalDateTime createdAt
) {
}