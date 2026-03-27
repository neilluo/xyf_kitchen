package com.grace.platform.promotion.interfaces.dto.response;

import java.time.LocalDateTime;

/**
 * 渠道响应
 * <p>
 * 用于返回渠道信息的响应体。
 * 对应 API E1-E5 的响应结构。
 * </p>
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
public record ChannelResponse(
        String channelId,
        String name,
        String type,
        String channelUrl,
        boolean hasApiKey,
        int priority,
        String status,
        String createdAt,
        String updatedAt
) {
    /**
     * 创建渠道响应
     *
     * @param channelId  渠道 ID
     * @param name       渠道名称
     * @param type       渠道类型：SOCIAL_MEDIA / FORUM / BLOG / OTHER
     * @param channelUrl 渠道 URL
     * @param hasApiKey  是否已配置 API Key（不返回明文）
     * @param priority   优先级
     * @param status     状态：ENABLED / DISABLED
     * @param createdAt  创建时间（ISO 8601）
     * @param updatedAt  更新时间（ISO 8601）
     */
    public ChannelResponse {
        // Record compact constructor
    }

    /**
     * 创建渠道响应（从 LocalDateTime）
     *
     * @param channelId  渠道 ID
     * @param name       渠道名称
     * @param type       渠道类型
     * @param channelUrl 渠道 URL
     * @param hasApiKey  是否已配置 API Key
     * @param priority   优先级
     * @param status     状态
     * @param createdAt  创建时间
     * @param updatedAt  更新时间
     * @return 渠道响应
     */
    public static ChannelResponse of(
            String channelId,
            String name,
            String type,
            String channelUrl,
            boolean hasApiKey,
            int priority,
            String status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        return new ChannelResponse(
                channelId,
                name,
                type,
                channelUrl,
                hasApiKey,
                priority,
                status,
                createdAt != null ? createdAt.toString() : null,
                updatedAt != null ? updatedAt.toString() : null
        );
    }
}
