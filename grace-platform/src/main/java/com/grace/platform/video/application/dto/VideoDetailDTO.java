package com.grace.platform.video.application.dto;

import com.grace.platform.video.domain.VideoStatus;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 视频详情 DTO。
 * <p>
 * 对应 API B6 响应的 data 字段，包含视频基本信息、元数据和发布记录。
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
public record VideoDetailDTO(
    String videoId,
    String fileName,
    String format,
    long fileSize,
    Duration duration,
    String filePath,
    VideoStatus status,
    String thumbnailUrl,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    MetadataDTO metadata,
    List<PublishRecordDTO> publishRecords
) {
    /**
     * 元数据 DTO（内嵌）。
     */
    public record MetadataDTO(
        String metadataId,
        String title,
        String description,
        List<String> tags,
        String source,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
    ) {
    }

    /**
     * 发布记录 DTO（内嵌）。
     */
    public record PublishRecordDTO(
        String publishRecordId,
        String platform,
        String status,
        String videoUrl,
        int progressPercent,
        String errorMessage,
        LocalDateTime publishedAt,
        LocalDateTime createdAt
    ) {
    }
}
