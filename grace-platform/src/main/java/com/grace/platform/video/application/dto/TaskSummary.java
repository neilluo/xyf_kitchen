package com.grace.platform.video.application.dto;

/**
 * 上传任务摘要 DTO。
 * <p>
 * 用于视频详情响应中展示当前关联的上传任务状态。
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
public record TaskSummary(
    String currentStep,
    String status,
    int attempts,
    String lastError
) {
}
