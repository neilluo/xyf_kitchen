package com.grace.platform.promotion.interfaces.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.List;

/**
 * 执行推广请求
 * <p>
 * 用于批量执行推广任务的 REST API 请求体。
 * 对应 API F2 的请求结构。
 * </p>
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
public record ExecutePromotionRequest(
        @NotBlank(message = "视频 ID 不能为空")
        String videoId,

        @NotEmpty(message = "推广任务列表不能为空")
        @Valid
        List<PromotionItemRequest> promotionItems
) {
    /**
     * 执行推广请求
     *
     * @param videoId        视频 ID（必填）
     * @param promotionItems 推广任务列表（必填）
     */
    public ExecutePromotionRequest {
        // Record compact constructor
    }

    /**
     * 推广任务项请求
     * <p>
     * 封装单个渠道的推广任务信息。
     * </p>
     */
    public record PromotionItemRequest(
            @NotBlank(message = "渠道 ID 不能为空")
            String channelId,

            @NotBlank(message = "推广标题不能为空")
            String promotionTitle,

            @NotBlank(message = "推广正文不能为空")
            String promotionBody,

            @NotNull(message = "推广方式不能为空")
            @Pattern(regexp = "POST|COMMENT|SHARE", message = "推广方式必须是 POST, COMMENT 或 SHARE")
            String method
    ) {
        /**
         * 创建推广任务项请求
         *
         * @param channelId      渠道 ID（必填）
         * @param promotionTitle 推广标题（必填）
         * @param promotionBody  推广正文（必填）
         * @param method         推广方式（必填）：POST / COMMENT / SHARE
         */
        public PromotionItemRequest {
            // Record compact constructor
        }
    }
}
