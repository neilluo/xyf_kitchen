package com.grace.platform.video.application.dto;

import com.grace.platform.video.domain.VideoStatus;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 视频列表项 DTO。
 * <p>
 * 对应 API B5 响应的 items 数组元素。
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
public record VideoListItemDTO(
    String videoId,
    String fileName,
    String format,
    long fileSize,
    Duration duration,
    VideoStatus status,
    String thumbnailUrl,
    boolean hasMetadata,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
