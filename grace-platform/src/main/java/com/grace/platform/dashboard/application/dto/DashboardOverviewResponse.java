package com.grace.platform.dashboard.application.dto;

import java.util.List;

/**
 * 仪表盘概览聚合响应 DTO
 *
 * @param stats               统计卡片
 * @param recentUploads       最近上传列表
 * @param publishDistribution 发布状态分布
 * @param promotionOverview   推广渠道概览列表
 * @param analytics           分析数据
 */
public record DashboardOverviewResponse(
    StatsDto stats,
    List<RecentUploadDto> recentUploads,
    PublishDistributionDto publishDistribution,
    List<PromotionOverviewDto> promotionOverview,
    AnalyticsDto analytics
) {}
