package com.grace.platform.promotion.interfaces.dto.response;

import java.time.LocalDateTime;

/**
 * 推广记录响应
 * <p>
 * 用于返回推广历史记录的响应体。
 * 对应 API F3 的分页列表项结构。
 * </p>
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
public record PromotionRecordResponse(
        String promotionRecordId,
        String videoId,
        String channelId,
        String channelName,
        String channelType,
        String promotionTitle,
        String promotionBody,
        String method,
        String status,
        String resultUrl,
        String errorMessage,
        String executedAt,
        String createdAt
) {
    /**
     * 创建推广记录响应
     *
     * @param promotionRecordId 推广记录 ID
     * @param videoId           视频 ID
     * @param channelId         渠道 ID
     * @param channelName       渠道名称
     * @param channelType       渠道类型
     * @param promotionTitle    推广标题
     * @param promotionBody     推广正文
     * @param method            推广方式
     * @param status            执行状态：PENDING / EXECUTING / COMPLETED / FAILED
     * @param resultUrl         推广结果链接
     * @param errorMessage      错误信息
     * @param executedAt        执行时间（ISO 8601）
     * @param createdAt         记录创建时间（ISO 8601）
     */
    public PromotionRecordResponse {
        // Record compact constructor
    }

    /**
     * 创建推广记录响应（从 LocalDateTime）
     *
     * @param promotionRecordId 推广记录 ID
     * @param videoId           视频 ID
     * @param channelId         渠道 ID
     * @param channelName       渠道名称
     * @param channelType       渠道类型
     * @param promotionTitle    推广标题
     * @param promotionBody     推广正文
     * @param method            推广方式
     * @param status            执行状态
     * @param resultUrl         推广结果链接
     * @param errorMessage      错误信息
     * @param executedAt        执行时间
     * @param createdAt         记录创建时间
     * @return 推广记录响应
     */
    public static PromotionRecordResponse of(
            String promotionRecordId,
            String videoId,
            String channelId,
            String channelName,
            String channelType,
            String promotionTitle,
            String promotionBody,
            String method,
            String status,
            String resultUrl,
            String errorMessage,
            LocalDateTime executedAt,
            LocalDateTime createdAt) {
        return new PromotionRecordResponse(
                promotionRecordId,
                videoId,
                channelId,
                channelName,
                channelType,
                promotionTitle,
                promotionBody,
                method,
                status,
                resultUrl,
                errorMessage,
                executedAt != null ? executedAt.toString() : null,
                createdAt != null ? createdAt.toString() : null
        );
    }
}
