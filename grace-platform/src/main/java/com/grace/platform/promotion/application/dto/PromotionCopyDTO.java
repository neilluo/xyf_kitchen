package com.grace.platform.promotion.application.dto;

import com.grace.platform.promotion.domain.PromotionMethod;

import java.time.LocalDateTime;

/**
 * 推广文案 DTO
 * <p>
 * 用于应用层与接口层之间的数据传输。
 * 对应 API F1 的响应数据结构。
 * </p>
 */
public record PromotionCopyDTO(
        String channelId,
        String channelName,
        String channelType,
        String promotionTitle,
        String promotionBody,
        PromotionMethod recommendedMethod,
        String methodReason
) {
    /**
     * 创建推广文案 DTO
     *
     * @param channelId         渠道 ID
     * @param channelName       渠道名称
     * @param channelType       渠道类型
     * @param promotionTitle    推广标题
     * @param promotionBody     推广正文
     * @param recommendedMethod 推荐推广方式
     * @param methodReason      推荐理由
     */
    public PromotionCopyDTO {
        // Record compact constructor
    }
}
