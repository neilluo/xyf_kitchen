package com.grace.platform.promotion.application.dto;

import com.grace.platform.promotion.domain.ChannelStatus;
import com.grace.platform.promotion.domain.ChannelType;

import java.time.LocalDateTime;

/**
 * 推广渠道 DTO
 * <p>
 * 用于应用层与接口层之间的数据传输。
 * 对应 API E1-E5 的响应数据结构。
 * </p>
 */
public record ChannelDTO(
        String channelId,
        String name,
        ChannelType type,
        String channelUrl,
        boolean hasApiKey,
        int priority,
        ChannelStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    /**
     * 创建渠道 DTO
     *
     * @param channelId   渠道 ID
     * @param name        渠道名称
     * @param type        渠道类型
     * @param channelUrl  渠道 URL
     * @param hasApiKey   是否已配置 API Key
     * @param priority    优先级
     * @param status      状态
     * @param createdAt   创建时间
     * @param updatedAt   更新时间
     */
    public ChannelDTO {
        // Record compact constructor
    }
}
