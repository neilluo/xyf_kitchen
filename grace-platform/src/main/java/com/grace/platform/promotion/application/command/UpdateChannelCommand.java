package com.grace.platform.promotion.application.command;

import com.grace.platform.promotion.domain.ChannelStatus;
import com.grace.platform.promotion.domain.ChannelType;

/**
 * 更新渠道命令
 * <p>
 * 用于封装更新推广渠道的请求参数。
 * 对应 API E2 的请求体字段，支持部分更新。
 * </p>
 */
public record UpdateChannelCommand(
        String name,
        ChannelType type,
        String channelUrl,
        String apiKey,
        Integer priority,
        ChannelStatus status
) {
    /**
     * 更新渠道命令
     * <p>
     * 所有字段均为可选，null 表示不更新该字段。
     * apiKey 如果提供则会被重新加密存储。
     * status 可用于启用/禁用渠道。
     * </p>
     *
     * @param name       渠道名称（可选）
     * @param type       渠道类型（可选）
     * @param channelUrl 渠道 URL（可选）
     * @param apiKey     API Key（可选，提供则重新加密）
     * @param priority   优先级（可选）
     * @param status     状态（可选，ENABLED/DISABLED）
     */
    public UpdateChannelCommand {
        // Record compact constructor - validation happens in domain layer
    }
}
