package com.grace.platform.promotion.interfaces.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * 生成推广文案请求
 * <p>
 * 用于为指定视频的推广渠道生成 AI 推广文案的 REST API 请求体。
 * 对应 API F1 的请求结构。
 * </p>
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
public record GenerateCopyRequest(
        @NotBlank(message = "视频 ID 不能为空")
        String videoId,

        List<String> channelIds
) {
    /**
     * 生成推广文案请求
     *
     * @param videoId    视频 ID（必填）
     * @param channelIds 指定渠道列表（可选，为空则生成所有 ENABLED 渠道的文案）
     */
    public GenerateCopyRequest {
        // Record compact constructor
    }
}
