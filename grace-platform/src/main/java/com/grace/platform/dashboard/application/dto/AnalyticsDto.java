package com.grace.platform.dashboard.application.dto;

/**
 * 分析数据 DTO
 *
 * @param avgEngagementRate 平均互动率
 * @param totalImpressions  总曝光量
 */
public record AnalyticsDto(
    double avgEngagementRate,
    long totalImpressions
) {}
