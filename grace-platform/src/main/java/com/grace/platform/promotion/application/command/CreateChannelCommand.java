package com.grace.platform.promotion.application.command;

import com.grace.platform.promotion.domain.ChannelType;

/**
 * 创建渠道命令
 * <p>
 * 用于封装创建推广渠道的请求参数。
 * 对应 API E1 的请求体字段。
 * </p>
 */
public record CreateChannelCommand(
        String name,
        ChannelType type,
        String channelUrl,
        String apiKey,
        Integer priority
) {
    /**
     * 创建渠道命令
     * <p>
     * priority 为可选，默认值为 1。
     * apiKey 为可选，如果提供则会被加密存储。
     * </p>
     *
     * @param name       渠道名称（必填）
     * @param type       渠道类型（必填）
     * @param channelUrl 渠道 URL（必填）
     * @param apiKey     API Key（可选）
     * @param priority   优先级（可选，默认 1）
     */
    public CreateChannelCommand {
        // Record compact constructor - validation happens in domain layer
    }

    /**
     * 获取优先级，如果未设置则返回默认值 1
     *
     * @return 优先级值
     */
    public int priorityOrDefault() {
        return priority != null ? priority : 1;
    }
}
