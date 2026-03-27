package com.grace.platform.video.interfaces.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 视频详情响应 DTO。
 * <p>
 * 对应 API B6 响应的 data 字段，包含视频基本信息、元数据和发布记录。
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
public record VideoDetailResponse(
    String videoId,
    String fileName,
    String format,
    long fileSize,
    String duration,
    String filePath,
    String status,
    String thumbnailUrl,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    MetadataResponse metadata,
    List<PublishRecordResponse> publishRecords
) {
    /**
     * 元数据响应 DTO（内嵌）。
     */
    public record MetadataResponse(
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
     * 发布记录响应 DTO（内嵌）。
     */
    public record PublishRecordResponse(
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
