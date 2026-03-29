package com.grace.platform.video.application.dto;

import java.time.LocalDateTime;

public record ServerUploadInitDTO(
    String uploadId,
    int totalChunks,
    long chunkSize,
    String tempDirectory,
    LocalDateTime expiresAt
) {
}