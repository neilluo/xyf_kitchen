package com.grace.platform.promotion.application.command;

import com.grace.platform.promotion.domain.PromotionMethod;
import com.grace.platform.shared.domain.id.ChannelId;
import com.grace.platform.shared.domain.id.VideoId;

import java.util.List;

/**
 * 执行推广命令
 * <p>
 * 用于封装批量执行推广任务的请求参数。
 * 对应 API F2 的请求体字段。
 * </p>
 *
 * @see com.grace.platform.promotion.application.PromotionApplicationService#executePromotion(ExecutePromotionCommand)
 */
public record ExecutePromotionCommand(
        VideoId videoId,
        List<PromotionItem> promotionItems
) {

    /**
     * 推广任务项
     * <p>
     * 封装单个渠道的推广任务信息。
     * </p>
     */
    public record PromotionItem(
            ChannelId channelId,
            String promotionTitle,
            String promotionBody,
            PromotionMethod method
    ) {
        /**
         * 创建推广任务项
         *
         * @param channelId       渠道 ID
         * @param promotionTitle  推广标题
         * @param promotionBody   推广正文
         * @param method          推广方式
         */
        public PromotionItem {
            // Record compact constructor - validation happens in domain layer
        }
    }

    /**
     * 创建执行推广命令
     *
     * @param videoId        视频 ID
     * @param promotionItems 推广任务列表
     */
    public ExecutePromotionCommand {
        // Record compact constructor - validation happens in domain layer
    }
}
