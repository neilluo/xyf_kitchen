package com.grace.platform.video.interfaces.dto.response;

import java.time.LocalDateTime;

/**
 * 视频信息响应 DTO。
 * <p>
 * 对应 API B3 响应的 data 字段（完成上传后返回）。
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
public record VideoInfoResponse(
    String videoId,
    String fileName,
    long fileSize,
    String format,
    String duration,
    String status,
    LocalDateTime createdAt
) {
}
