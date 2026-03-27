package com.grace.platform.promotion.interfaces.dto.request;

import jakarta.validation.constraints.*;

/**
 * 更新渠道请求
 * <p>
 * 用于更新推广渠道的 REST API 请求体。
 * 对应 API E2 的请求结构，支持部分更新。
 * </p>
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
public record UpdateChannelRequest(
        @Size(max = 200, message = "渠道名称不能超过 200 字符")
        String name,

        @Pattern(regexp = "SOCIAL_MEDIA|FORUM|BLOG|OTHER", message = "渠道类型必须是 SOCIAL_MEDIA, FORUM, BLOG 或 OTHER")
        String type,

        @Size(max = 500, message = "渠道 URL 不能超过 500 字符")
        String channelUrl,

        String apiKey,

        @Min(value = 1, message = "优先级最小为 1")
        @Max(value = 99, message = "优先级最大为 99")
        Integer priority,

        @Pattern(regexp = "ENABLED|DISABLED", message = "状态必须是 ENABLED 或 DISABLED")
        String status
) {
    /**
     * 更新渠道请求
     * <p>
     * 所有字段均为可选，null 表示不更新该字段。
     * </p>
     *
     * @param name       渠道名称（可选）
     * @param type       渠道类型（可选）
     * @param channelUrl 渠道 URL（可选）
     * @param apiKey     API Key（可选）
     * @param priority   优先级（可选）
     * @param status     状态（可选）：ENABLED / DISABLED
     */
    public UpdateChannelRequest {
        // Record compact constructor
    }
}
