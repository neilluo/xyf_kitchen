package com.grace.platform.dashboard.application.dto;

/**
 * 发布状态分布 DTO
 *
 * @param published 已发布数
 * @param pending   处理中数
 * @param failed    失败数
 */
public record PublishDistributionDto(
    long published,
    long pending,
    long failed
) {}
