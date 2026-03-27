package com.grace.platform.promotion.interfaces.dto.response;

import java.time.LocalDateTime;

/**
 * 推广结果响应
 * <p>
 * 用于返回推广执行结果的响应体。
 * 对应 API F2 和 F5 的响应结构。
 * </p>
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
public record PromotionResultResponse(
        String promotionRecordId,
        String channelId,
        String channelName,
        String method,
        String status,
        String resultUrl,
        String errorMessage,
        String executedAt
) {
    /**
     * 创建推广结果响应
     *
     * @param promotionRecordId 推广记录 ID
     * @param channelId         渠道 ID
     * @param channelName       渠道名称
     * @param method            推广方式：POST / COMMENT / SHARE
     * @param status            执行状态：COMPLETED / FAILED
     * @param resultUrl         推广结果链接
     * @param errorMessage      错误信息
     * @param executedAt        执行时间（ISO 8601）
     */
    public PromotionResultResponse {
        // Record compact constructor
    }

    /**
     * 创建推广结果响应（从 LocalDateTime）
     *
     * @param promotionRecordId 推广记录 ID
     * @param channelId         渠道 ID
     * @param channelName       渠道名称
     * @param method            推广方式
     * @param status            执行状态
     * @param resultUrl         推广结果链接
     * @param errorMessage      错误信息
     * @param executedAt        执行时间
     * @return 推广结果响应
     */
    public static PromotionResultResponse of(
            String promotionRecordId,
            String channelId,
            String channelName,
            String method,
            String status,
            String resultUrl,
            String errorMessage,
            LocalDateTime executedAt) {
        return new PromotionResultResponse(
                promotionRecordId,
                channelId,
                channelName,
                method,
                status,
                resultUrl,
                errorMessage,
                executedAt != null ? executedAt.toString() : null
        );
    }
}
