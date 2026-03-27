package com.grace.platform.promotion.interfaces.dto.response;

import java.util.List;

/**
 * 推广报告响应
 * <p>
 * 用于返回推广执行报告汇总的响应体。
 * 对应 API F4 的响应结构。
 * </p>
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
public record PromotionReportResponse(
        String videoId,
        String videoTitle,
        int totalChannels,
        int successCount,
        int failedCount,
        int pendingCount,
        double overallSuccessRate,
        List<ChannelSummaryResponse> channelSummaries
) {
    /**
     * 创建推广报告响应
     *
     * @param videoId            视频 ID
     * @param videoTitle         视频标题
     * @param totalChannels      总渠道数
     * @param successCount       成功数
     * @param failedCount        失败数
     * @param pendingCount       待执行数
     * @param overallSuccessRate 整体成功率（0.0-1.0）
     * @param channelSummaries   各渠道执行摘要列表
     */
    public PromotionReportResponse {
        // Record compact constructor
    }

    /**
     * 渠道执行摘要响应
     * <p>
     * 封装单个渠道的执行摘要信息。
     * </p>
     */
    public record ChannelSummaryResponse(
            String channelId,
            String channelName,
            String channelType,
            String method,
            String status,
            String resultUrl,
            String errorMessage,
            String executedAt
    ) {
        /**
         * 创建渠道执行摘要响应
         *
         * @param channelId    渠道 ID
         * @param channelName  渠道名称
         * @param channelType  渠道类型
         * @param method       推广方式
         * @param status       执行状态
         * @param resultUrl    推广结果链接
         * @param errorMessage 错误信息
         * @param executedAt   执行时间（ISO 8601）
         */
        public ChannelSummaryResponse {
            // Record compact constructor
        }
    }
}
