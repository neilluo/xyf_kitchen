package com.grace.platform.metadata.application.dto;

import com.grace.platform.metadata.domain.MetadataSource;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 视频元数据 DTO
 * <p>
 * 用于应用层与接口层之间的数据传输。
 * 对应 API C1-C5 的响应数据结构。
 * </p>
 */
public record VideoMetadataDTO(
        String metadataId,
        String videoId,
        String title,
        String description,
        List<String> tags,
        MetadataSource source,
        boolean confirmed,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    /**
     * 创建视频元数据 DTO
     *
     * @param metadataId  元数据 ID
     * @param videoId     视频 ID
     * @param title       标题
     * @param description 描述
     * @param tags        标签列表
     * @param source      来源
     * @param confirmed   是否已确认
     * @param createdAt   创建时间
     * @param updatedAt   更新时间
     */
    public VideoMetadataDTO {
        // Record compact constructor
    }
}
