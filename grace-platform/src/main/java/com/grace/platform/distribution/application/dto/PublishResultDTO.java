package com.grace.platform.distribution.application.dto;

import com.grace.platform.distribution.domain.PublishStatus;

import java.time.LocalDateTime;

/**
 * 发布结果 DTO
 * <p>
 * 对应 API D1 响应的 data 字段。
 * </p>
 *
 * @param publishRecordId 发布记录 ID
 * @param videoId         视频 ID
 * @param platform        平台标识
 * @param uploadTaskId    上传任务 ID（用于查询发布状态）
 * @param status          当前发布状态
 * @param createdAt       创建时间
 */
public record PublishResultDTO(
    String publishRecordId,
    String videoId,
    String platform,
    String uploadTaskId,
    PublishStatus status,
    LocalDateTime createdAt
) {
}
