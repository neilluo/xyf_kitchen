package com.grace.platform.video.application.dto;

import com.grace.platform.video.domain.VideoStatus;

import java.time.Duration;
import java.time.LocalDateTime;

public record ServerUploadCompleteDTO(
    String videoId,
    String fileName,
    long fileSize,
    String format,
    Duration duration,
    VideoStatus status,
    String storageUrl,
    LocalDateTime createdAt
) {
}