package com.grace.platform.video.application.dto;

/**
 * 手动重试元数据生成响应 DTO。
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
public record RetryMetadataDTO(
    String taskId,
    String currentStep,
    String status
) {
}
