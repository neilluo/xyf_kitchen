package com.grace.platform.distribution.application.dto;

import com.grace.platform.distribution.domain.PublishStatus;

import java.time.LocalDateTime;

/**
 * 上传状态 DTO
 * <p>
 * 对应 API D2 响应的 data 字段，用于查询发布上传进度。
 * </p>
 *
 * @param publishRecordId 发布记录 ID
 * @param taskId          上传任务 ID
 * @param platform        平台标识
 * @param status          当前发布状态
 * @param progressPercent 上传进度百分比（0-100）
 * @param videoUrl        发布成功后的视频链接（可选）
 * @param errorMessage    错误信息（可选，仅在失败状态时提供）
 * @param publishedAt     发布完成时间（可选）
 */
public record UploadStatusDTO(
    String publishRecordId,
    String taskId,
    String platform,
    PublishStatus status,
    int progressPercent,
    String videoUrl,
    String errorMessage,
    LocalDateTime publishedAt
) {
}
