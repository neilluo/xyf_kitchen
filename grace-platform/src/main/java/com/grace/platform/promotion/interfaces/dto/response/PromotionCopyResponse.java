package com.grace.platform.promotion.interfaces.dto.response;

/**
 * 推广文案响应
 * <p>
 * 用于返回 AI 生成的推广文案的响应体。
 * 对应 API F1 的响应结构。
 * </p>
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
public record PromotionCopyResponse(
        String channelId,
        String channelName,
        String channelType,
        String promotionTitle,
        String promotionBody,
        String recommendedMethod,
        String methodReason
) {
    /**
     * 创建推广文案响应
     *
     * @param channelId         渠道 ID
     * @param channelName       渠道名称
     * @param channelType       渠道类型：SOCIAL_MEDIA / FORUM / BLOG / OTHER
     * @param promotionTitle    推广标题
     * @param promotionBody     推广正文
     * @param recommendedMethod 推荐推广方式：POST / COMMENT / SHARE
     * @param methodReason      推荐理由
     */
    public PromotionCopyResponse {
        // Record compact constructor
    }
}
