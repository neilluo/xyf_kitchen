package com.grace.platform.distribution.interfaces.dto.response;

import com.grace.platform.distribution.domain.PublishStatus;

import java.time.LocalDateTime;

/**
 * 发布结果响应 DTO
 * <p>
 * 对应 API D1 响应的 data 字段。
 * 包含发布任务的初始状态和上传任务 ID。
 * </p>
 *
 * @param publishRecordId 发布记录 ID
 * @param videoId         视频 ID
 * @param platform        平台标识
 * @param uploadTaskId    上传任务 ID（用于查询发布状态）
 * @param status          初始状态：PENDING 或 UPLOADING
 * @param createdAt       ISO 8601 创建时间
 */
public record PublishResultResponse(
    String publishRecordId,
    String videoId,
    String platform,
    String uploadTaskId,
    PublishStatus status,
    LocalDateTime createdAt
) {
}
