package com.grace.platform.dashboard.application.dto;

/**
 * 仪表盘统计卡片 DTO
 *
 * @param totalVideos   视频总数
 * @param pendingReview 待审核数量
 * @param published     已发布数量
 * @param promoting     推广中数量
 */
public record StatsDto(
    long totalVideos,
    long pendingReview,
    long published,
    long promoting
) {}
