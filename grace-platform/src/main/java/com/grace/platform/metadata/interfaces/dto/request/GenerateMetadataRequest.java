package com.grace.platform.metadata.interfaces.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 生成元数据请求
 * <p>
 * 用于手动触发视频元数据 AI 生成的请求体。
 * 对应 API C1 的请求字段。
 * </p>
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
public record GenerateMetadataRequest(
        @NotBlank(message = "视频ID不能为空")
        String videoId
) {
    /**
     * 创建生成元数据请求
     *
     * @param videoId 视频 ID
     */
    public GenerateMetadataRequest {
        // Record compact constructor
    }
}
