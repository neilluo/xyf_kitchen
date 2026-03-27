package com.grace.platform.promotion.interfaces.dto.request;

import jakarta.validation.constraints.*;

/**
 * 创建渠道请求
 * <p>
 * 用于创建推广渠道的 REST API 请求体。
 * 对应 API E1 的请求结构。
 * </p>
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
public record CreateChannelRequest(
        @NotBlank(message = "渠道名称不能为空")
        @Size(max = 200, message = "渠道名称不能超过 200 字符")
        String name,

        @NotBlank(message = "渠道类型不能为空")
        @Pattern(regexp = "SOCIAL_MEDIA|FORUM|BLOG|OTHER", message = "渠道类型必须是 SOCIAL_MEDIA, FORUM, BLOG 或 OTHER")
        String type,

        @NotBlank(message = "渠道 URL 不能为空")
        @Size(max = 500, message = "渠道 URL 不能超过 500 字符")
        String channelUrl,

        String apiKey,

        @Min(value = 1, message = "优先级最小为 1")
        @Max(value = 99, message = "优先级最大为 99")
        Integer priority
) {
    /**
     * 创建渠道请求
     *
     * @param name       渠道名称（必填）
     * @param type       渠道类型（必填）：SOCIAL_MEDIA / FORUM / BLOG / OTHER
     * @param channelUrl 渠道 URL（必填）
     * @param apiKey     API Key（可选）
     * @param priority   优先级（可选，默认 1）
     */
    public CreateChannelRequest {
        // Record compact constructor
    }
}
