package com.grace.platform.promotion.application.dto;

import com.grace.platform.promotion.domain.PromotionMethod;
import com.grace.platform.promotion.domain.PromotionStatus;

import java.time.LocalDateTime;

/**
 * 推广结果 DTO
 * <p>
 * 用于应用层与接口层之间的数据传输。
 * 对应 API F2 和 F5 的响应数据结构。
 * </p>
 */
public record PromotionResultDTO(
        String promotionRecordId,
        String channelId,
        String channelName,
        PromotionMethod method,
        PromotionStatus status,
        String resultUrl,
        String errorMessage,
        LocalDateTime executedAt
) {
    /**
     * 创建推广结果 DTO
     *
     * @param promotionRecordId 推广记录 ID
     * @param channelId         渠道 ID
     * @param channelName       渠道名称
     * @param method            推广方式
     * @param status            执行状态
     * @param resultUrl         推广结果链接
     * @param errorMessage      错误信息
     * @param executedAt        执行时间
     */
    public PromotionResultDTO {
        // Record compact constructor
    }
}
