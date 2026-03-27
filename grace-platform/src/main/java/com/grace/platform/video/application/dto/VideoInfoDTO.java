package com.grace.platform.video.application.dto;

import com.grace.platform.video.domain.VideoStatus;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 视频信息响应 DTO。
 * <p>
 * 对应 API B3 响应的 data 字段（完成上传后返回）。
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
public record VideoInfoDTO(
    String videoId,
    String fileName,
    long fileSize,
    String format,
    Duration duration,
    VideoStatus status,
    LocalDateTime createdAt
) {
}
