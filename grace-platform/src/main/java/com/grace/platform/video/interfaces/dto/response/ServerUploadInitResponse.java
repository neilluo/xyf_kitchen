package com.grace.platform.video.interfaces.dto.response;

import java.time.LocalDateTime;

public record ServerUploadInitResponse(
    String uploadId,
    int totalChunks,
    long chunkSize,
    String tempDirectory,
    LocalDateTime expiresAt
) {
}