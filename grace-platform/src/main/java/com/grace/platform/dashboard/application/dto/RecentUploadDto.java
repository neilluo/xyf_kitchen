package com.grace.platform.dashboard.application.dto;

/**
 * 最近上传视频 DTO
 *
 * @param videoId      视频 ID
 * @param fileName     文件名
 * @param thumbnailUrl 缩略图 URL（nullable）
 * @param status       视频状态枚举值
 * @param createdAt    ISO 8601 上传时间
 */
public record RecentUploadDto(
    String videoId,
    String fileName,
    String thumbnailUrl,
    String status,
    String createdAt
) {}
