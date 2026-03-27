package com.grace.platform.dashboard.application.dto;

/**
 * 推广渠道概览 DTO
 *
 * @param channelId       渠道 ID
 * @param channelName     渠道名称
 * @param totalExecutions 总执行次数
 * @param successCount    成功次数
 * @param failedCount     失败次数
 * @param successRate     成功率（0.0-1.0）
 */
public record PromotionOverviewDto(
    String channelId,
    String channelName,
    long totalExecutions,
    long successCount,
    long failedCount,
    double successRate
) {}
