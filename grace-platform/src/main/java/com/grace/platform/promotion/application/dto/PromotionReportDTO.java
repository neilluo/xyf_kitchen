package com.grace.platform.promotion.application.dto;

import java.util.List;

/**
 * 推广报告 DTO
 * <p>
 * 用于应用层与接口层之间的数据传输。
 * 对应 API F4 的响应数据结构。
 * </p>
 */
public record PromotionReportDTO(
        String videoId,
        String videoTitle,
        int totalChannels,
        int successCount,
        int failedCount,
        int pendingCount,
        double overallSuccessRate,
        List<ChannelSummaryDTO> channelSummaries
) {
    /**
     * 创建推广报告 DTO
     *
     * @param videoId             视频 ID
     * @param videoTitle          视频标题
     * @param totalChannels       总渠道数
     * @param successCount        成功数
     * @param failedCount         失败数
     * @param pendingCount        待执行数
     * @param overallSuccessRate  整体成功率
     * @param channelSummaries    各渠道执行摘要
     */
    public PromotionReportDTO {
        // Record compact constructor
    }

    /**
     * 渠道执行摘要 DTO
     * <p>
     * 封装单个渠道的执行摘要信息。
     * </p>
     */
    public record ChannelSummaryDTO(
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
         * 创建渠道执行摘要 DTO
         *
         * @param channelId     渠道 ID
         * @param channelName   渠道名称
         * @param channelType   渠道类型
         * @param method        推广方式
         * @param status        执行状态
         * @param resultUrl     推广结果链接
         * @param errorMessage  错误信息
         * @param executedAt    执行时间
         */
        public ChannelSummaryDTO {
            // Record compact constructor
        }
    }
}
