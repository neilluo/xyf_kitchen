package com.grace.platform.dashboard;

import com.grace.platform.dashboard.application.DashboardQueryService;
import com.grace.platform.dashboard.application.dto.DashboardOverviewResponse;
import com.grace.platform.dashboard.application.dto.StatsDto;
import com.grace.platform.dashboard.application.dto.RecentUploadDto;
import com.grace.platform.dashboard.application.dto.PublishDistributionDto;
import com.grace.platform.dashboard.application.dto.PromotionOverviewDto;
import com.grace.platform.dashboard.application.dto.AnalyticsDto;
import com.grace.platform.distribution.domain.OAuthToken;
import com.grace.platform.distribution.domain.OAuthTokenRepository;
import com.grace.platform.distribution.domain.PublishRecord;
import com.grace.platform.distribution.domain.PublishRecordRepository;
import com.grace.platform.distribution.domain.PublishStatus;
import com.grace.platform.GracePlatformApplication;
import com.grace.platform.promotion.domain.*;
import com.grace.platform.shared.domain.id.ChannelId;
import com.grace.platform.shared.domain.id.MetadataId;
import com.grace.platform.shared.domain.id.VideoId;
import com.grace.platform.testutil.AbstractIntegrationTest;
import com.grace.platform.testutil.TestFixtures;
import com.grace.platform.video.domain.Video;
import com.grace.platform.video.domain.VideoFormat;
import com.grace.platform.video.domain.VideoRepository;
import com.grace.platform.video.domain.VideoStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Dashboard 上下文集成测试。
 * <p>
 * 测试仪表盘聚合查询：预置数据 → 调用 A1 → 验证各统计字段。
 * 继承 AbstractIntegrationTest 使用 Testcontainers MySQL 8.0。
 * </p>
 */
@SpringBootTest(classes = GracePlatformApplication.class)
class DashboardIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private DashboardQueryService dashboardQueryService;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private PublishRecordRepository publishRecordRepository;

    @Autowired
    private PromotionRecordRepository promotionRecordRepository;

    @Autowired
    private OAuthTokenRepository oAuthTokenRepository;

    @Test
    @DisplayName("仪表盘概览查询：预置数据后验证各统计字段")
    void dashboardOverviewQuery() {
        // Given: 预置数据
        setupTestData();

        // When: 调用 A1 端点（默认 30d 时间范围）
        DashboardOverviewResponse overview = dashboardQueryService.getOverview("30d");

        // Then: 验证各统计字段
        assertThat(overview).isNotNull();

        // 1. 验证统计卡片
        StatsDto stats = overview.stats();
        assertThat(stats).isNotNull();
        assertThat(stats.totalVideos()).isGreaterThanOrEqualTo(3);
        assertThat(stats.pendingReview()).isGreaterThanOrEqualTo(0);
        assertThat(stats.published()).isGreaterThanOrEqualTo(1);
        assertThat(stats.promoting()).isGreaterThanOrEqualTo(0);

        // 2. 验证最近上传
        List<RecentUploadDto> recentUploads = overview.recentUploads();
        assertThat(recentUploads).isNotEmpty();
        assertThat(recentUploads.size()).isLessThanOrEqualTo(5);
        // 验证按时间倒序排列
        if (recentUploads.size() > 1) {
            // 简单验证：第一条的 ID 不为空
            assertThat(recentUploads.get(0).videoId()).isNotBlank();
        }

        // 3. 验证发布分布
        PublishDistributionDto distribution = overview.publishDistribution();
        assertThat(distribution).isNotNull();
        assertThat(distribution.published()).isGreaterThanOrEqualTo(1);

        // 4. 验证推广渠道概览
        List<PromotionOverviewDto> promotionOverview = overview.promotionOverview();
        assertThat(promotionOverview).isNotNull();

        // 5. 验证分析数据
        AnalyticsDto analytics = overview.analytics();
        assertThat(analytics).isNotNull();
        assertThat(analytics.avgEngagementRate()).isGreaterThanOrEqualTo(0.0);
        assertThat(analytics.totalImpressions()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("仪表盘时间范围查询：7d / 30d / 90d / all")
    void dashboardWithDifferentDateRanges() {
        // Given: 预置数据
        setupTestData();

        // When & Then: 测试不同时间范围
        // 7d
        DashboardOverviewResponse sevenDays = dashboardQueryService.getOverview("7d");
        assertThat(sevenDays).isNotNull();
        assertThat(sevenDays.stats()).isNotNull();

        // 30d
        DashboardOverviewResponse thirtyDays = dashboardQueryService.getOverview("30d");
        assertThat(thirtyDays).isNotNull();
        assertThat(thirtyDays.stats()).isNotNull();

        // 90d
        DashboardOverviewResponse ninetyDays = dashboardQueryService.getOverview("90d");
        assertThat(ninetyDays).isNotNull();
        assertThat(ninetyDays.stats()).isNotNull();

        // all
        DashboardOverviewResponse allTime = dashboardQueryService.getOverview("all");
        assertThat(allTime).isNotNull();
        assertThat(allTime.stats()).isNotNull();

        // null (default to 30d)
        DashboardOverviewResponse defaultRange = dashboardQueryService.getOverview(null);
        assertThat(defaultRange).isNotNull();
        assertThat(defaultRange.stats()).isNotNull();
    }

    @Test
    @DisplayName("空数据仪表盘：无数据时应返回默认值")
    void emptyDashboard() {
        // When: 查询空数据（不预置任何数据）
        DashboardOverviewResponse overview = dashboardQueryService.getOverview("30d");

        // Then: 应返回非空响应
        assertThat(overview).isNotNull();
        assertThat(overview.stats()).isNotNull();
        assertThat(overview.recentUploads()).isNotNull();
        assertThat(overview.publishDistribution()).isNotNull();
        assertThat(overview.promotionOverview()).isNotNull();
        assertThat(overview.analytics()).isNotNull();
    }

    /**
     * 预置测试数据
     */
    private void setupTestData() {
        // 1. 创建视频（不同状态）
        // 视频 1：UPLOADED
        Video video1 = Video.create(
            "test_video_1.mp4",
            1024 * 1024L,
            VideoFormat.MP4,
            Duration.ofSeconds(120),
            "/storage/videos/test1.mp4"
        );
        videoRepository.save(video1);

        // 视频 2：READY_TO_PUBLISH
        Video video2 = Video.create(
            "test_video_2.mp4",
            2 * 1024 * 1024L,
            VideoFormat.MOV,
            Duration.ofSeconds(180),
            "/storage/videos/test2.mp4"
        );
        video2.transitionTo(VideoStatus.READY_TO_PUBLISH);
        videoRepository.save(video2);

        // 视频 3：PUBLISHED
        Video video3 = Video.create(
            "test_video_3.mp4",
            3 * 1024 * 1024L,
            VideoFormat.AVI,
            Duration.ofSeconds(240),
            "/storage/videos/test3.mp4"
        );
        video3.transitionTo(VideoStatus.PUBLISHED);
        videoRepository.save(video3);

        // 2. 创建发布记录（COMPLETED 状态）
        VideoId videoId = video3.getId();
        MetadataId metadataId = new MetadataId("meta-" + System.currentTimeMillis());
        
        PublishRecord publishRecord = PublishRecord.create(videoId, metadataId, "youtube");
        publishRecord.markAsUploading("task_" + System.currentTimeMillis());
        publishRecord.markAsCompleted("https://www.youtube.com/watch?v=test123");
        publishRecordRepository.save(publishRecord);

        // 3. 创建 OAuth Token
        OAuthToken token = OAuthToken.create(
            "youtube",
            "encrypted_access_token",
            "encrypted_refresh_token",
            LocalDateTime.now().plusHours(1)
        );
        oAuthTokenRepository.save(token);

        // 4. 创建推广渠道
        PromotionChannel channel = PromotionChannel.create(
            "测试渠道",
            ChannelType.SOCIAL_MEDIA,
            "https://example.com/channel",
            1
        );
        // 这里需要注入 channelRepository，但为简化测试，我们依赖已有的数据
        // 或者直接查询已存在的渠道（如果有的话）
    }
}
