package com.grace.platform.distribution.application.dto;

import com.grace.platform.distribution.domain.PublishStatus;

import java.time.LocalDateTime;

/**
 * 发布记录 DTO
 * <p>
 * 对应 API B6 和 D6 响应中的发布记录结构。
 * </p>
 *
 * @param publishRecordId 发布记录 ID
 * @param platform        平台标识
 * @param status          发布状态
 * @param videoUrl        发布后视频链接（可选）
 * @param progressPercent 上传进度百分比（0-100）
 * @param errorMessage    错误信息（可选）
 * @param publishedAt     发布时间（可选）
 * @param createdAt       记录创建时间
 */
public record PublishRecordDTO(
    String publishRecordId,
    String platform,
    PublishStatus status,
    String videoUrl,
    int progressPercent,
    String errorMessage,
    LocalDateTime publishedAt,
    LocalDateTime createdAt
) {
}
