package com.grace.platform.promotion.application.dto;

import com.grace.platform.promotion.domain.PromotionMethod;
import com.grace.platform.promotion.domain.PromotionStatus;

import java.time.LocalDateTime;

/**
 * 推广记录 DTO
 * <p>
 * 用于应用层与接口层之间的数据传输。
 * 对应 API F3 的分页列表项数据结构。
 * </p>
 */
public record PromotionRecordDTO(
        String promotionRecordId,
        String videoId,
        String channelId,
        String channelName,
        String channelType,
        String promotionTitle,
        String promotionBody,
        PromotionMethod method,
        PromotionStatus status,
        String resultUrl,
        String errorMessage,
        LocalDateTime executedAt,
        LocalDateTime createdAt
) {
    /**
     * 创建推广记录 DTO
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
     */
    public PromotionRecordDTO {
        // Record compact constructor
    }
}
