package com.grace.platform.dashboard.application;

import com.grace.platform.dashboard.application.dto.AnalyticsDto;
import com.grace.platform.dashboard.application.dto.DashboardOverviewResponse;
import com.grace.platform.dashboard.application.dto.PromotionOverviewDto;
import com.grace.platform.dashboard.application.dto.PublishDistributionDto;
import com.grace.platform.dashboard.application.dto.RecentUploadDto;
import com.grace.platform.dashboard.application.dto.StatsDto;
import com.grace.platform.distribution.domain.PublishRecordRepository;
import com.grace.platform.distribution.domain.PublishStatus;
import com.grace.platform.promotion.domain.PromotionRecordRepository;
import com.grace.platform.promotion.domain.PromotionStatus;
import com.grace.platform.video.domain.Video;
import com.grace.platform.video.domain.VideoRepository;
import com.grace.platform.video.domain.VideoStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Dashboard 聚合查询服务。
 * <p>
 * 跨限界上下文的只读查询服务，聚合来自 Video、Distribution、Promotion
 * 三个上下文的数据，提供仪表盘概览数据。
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
@Service
@Transactional(readOnly = true)
public class DashboardQueryService {

    private final VideoRepository videoRepository;
    private final PublishRecordRepository publishRecordRepository;
    private final PromotionRecordRepository promotionRecordRepository;

    /**
     * 构造器注入依赖。
     *
     * @param videoRepository           视频仓储
     * @param publishRecordRepository   发布记录仓储
     * @param promotionRecordRepository 推广记录仓储
     */
    public DashboardQueryService(VideoRepository videoRepository,
                                 PublishRecordRepository publishRecordRepository,
                                 PromotionRecordRepository promotionRecordRepository) {
        this.videoRepository = videoRepository;
        this.publishRecordRepository = publishRecordRepository;
        this.promotionRecordRepository = promotionRecordRepository;
    }

    /**
     * 聚合查询仪表盘全部概览数据。
     *
     * @param dateRange 时间范围过滤：7d / 30d / 90d / all
     * @return 仪表盘概览响应
     */
    public DashboardOverviewResponse getOverview(String dateRange) {
        LocalDateTime since = resolveDateRange(dateRange);

        return new DashboardOverviewResponse(
            queryStats(),
            queryRecentUploads(),
            queryPublishDistribution(),
            queryPromotionOverview(since),
            queryAnalytics(since)
        );
    }

    // --- 私有查询方法 ---

    /**
     * 查询统计卡片数据。
     *
     * @return 统计卡片 DTO
     */
    private StatsDto queryStats() {
        long totalVideos = videoRepository.count();
        long pendingReview = videoRepository.countByStatuses(
            List.of(VideoStatus.UPLOADED, VideoStatus.METADATA_GENERATED));
        long published = videoRepository.countByStatuses(
            List.of(VideoStatus.PUBLISHED, VideoStatus.PROMOTION_DONE));
        long promoting = promotionRecordRepository.countDistinctVideosByStatus(
            PromotionStatus.EXECUTING);

        return new StatsDto(totalVideos, pendingReview, published, promoting);
    }

    /**
     * 查询最近上传的视频。
     *
     * @return 最近上传列表
     */
    private List<RecentUploadDto> queryRecentUploads() {
        return videoRepository.findTop5ByOrderByCreatedAtDesc().stream()
            .map(video -> new RecentUploadDto(
                video.getId().value(),
                video.getFileName(),
                null, // thumbnailUrl — MVP 阶段暂不生成缩略图
                video.getStatus().name(),
                video.getCreatedAt().toString()
            ))
            .toList();
    }

    /**
     * 查询发布状态分布。
     *
     * @return 发布分布 DTO
     */
    private PublishDistributionDto queryPublishDistribution() {
        // 使用 countGroupByStatus 获取各状态分布
        var statusCounts = publishRecordRepository.countGroupByStatus();

        long published = statusCounts.getOrDefault(PublishStatus.COMPLETED, 0L);
        long pending = statusCounts.getOrDefault(PublishStatus.PENDING, 0L)
            + statusCounts.getOrDefault(PublishStatus.UPLOADING, 0L);
        long failed = statusCounts.getOrDefault(PublishStatus.FAILED, 0L)
            + statusCounts.getOrDefault(PublishStatus.QUOTA_EXCEEDED, 0L);

        return new PublishDistributionDto(published, pending, failed);
    }

    /**
     * 查询推广渠道概览。
     *
     * @param since 起始时间
     * @return 推广渠道概览列表
     */
    private List<PromotionOverviewDto> queryPromotionOverview(LocalDateTime since) {
        // 使用 getChannelSuccessRates 获取渠道成功率统计
        var channelRates = promotionRecordRepository.getChannelSuccessRates(since, LocalDateTime.now());

        return channelRates.stream()
            .map(rate -> {
                long failedCount = rate.totalCount() - rate.successCount();
                double successRate = rate.totalCount() > 0
                    ? (double) rate.successCount() / rate.totalCount()
                    : 0.0;

                return new PromotionOverviewDto(
                    rate.channelId().value(),
                    rate.channelName(),
                    rate.totalCount(),
                    rate.successCount(),
                    failedCount,
                    successRate
                );
            })
            .toList();
    }

    /**
     * 查询分析数据。
     *
     * @param since 起始时间
     * @return 分析数据 DTO
     */
    private AnalyticsDto queryAnalytics(LocalDateTime since) {
        // MVP 阶段：使用 getChannelSuccessRates 的汇总数据近似计算
        var channelRates = promotionRecordRepository.getChannelSuccessRates(since, LocalDateTime.now());

        long totalPromotions = channelRates.stream()
            .mapToLong(PromotionRecordRepository.ChannelSuccessRate::totalCount)
            .sum();
        long successCount = channelRates.stream()
            .mapToLong(PromotionRecordRepository.ChannelSuccessRate::successCount)
            .sum();

        double avgEngagementRate = totalPromotions > 0
            ? (double) successCount / totalPromotions
            : 0.0;

        return new AnalyticsDto(avgEngagementRate, successCount);
    }

    /**
     * 解析时间范围字符串为 LocalDateTime。
     *
     * @param dateRange 时间范围：7d / 30d / 90d / all
     * @return 起始时间
     */
    private LocalDateTime resolveDateRange(String dateRange) {
        if (dateRange == null) {
            dateRange = "30d";
        }
        return switch (dateRange) {
            case "7d" -> LocalDateTime.now().minusDays(7);
            case "30d" -> LocalDateTime.now().minusDays(30);
            case "90d" -> LocalDateTime.now().minusDays(90);
            case "all" -> LocalDateTime.of(2000, 1, 1, 0, 0);
            default -> LocalDateTime.now().minusDays(30);
        };
    }
}
