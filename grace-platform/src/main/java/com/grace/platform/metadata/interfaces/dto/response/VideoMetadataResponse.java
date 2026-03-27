package com.grace.platform.metadata.interfaces.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 视频元数据响应
 * <p>
 * 用于返回元数据信息的响应体。
 * 对应 API C1-C5 的通用响应结构。
 * </p>
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
public record VideoMetadataResponse(
        String metadataId,
        String videoId,
        String title,
        String description,
        List<String> tags,
        String source,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    /**
     * 创建视频元数据响应
     *
     * @param metadataId  元数据 ID
     * @param videoId     视频 ID
     * @param title       标题
     * @param description 描述
     * @param tags        标签列表
     * @param source      来源：AI_GENERATED / MANUAL / AI_EDITED
     * @param createdAt   创建时间（ISO 8601）
     * @param updatedAt   更新时间（ISO 8601）
     */
    public VideoMetadataResponse {
        // Record compact constructor
    }
}
