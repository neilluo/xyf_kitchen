package com.grace.platform.distribution;

import com.grace.platform.distribution.domain.*;
import com.grace.platform.shared.domain.id.MetadataId;
import com.grace.platform.shared.domain.id.PublishRecordId;
import com.grace.platform.shared.domain.id.VideoId;
import com.grace.platform.shared.infrastructure.exception.BusinessRuleViolationException;
import net.jqwik.api.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Property-based tests for Distribution domain.
 * <p>
 * Covers Properties 6-8 from testing strategy:
 * - Property 6: Platform routing correctness (VideoDistributorRegistry)
 * - Property 7: Publish result contains valid video URL
 * - Property 8: Publish record persistence round-trip
 * <p>
 * Note: Property 8 (persistence round-trip) requires database access
 * and is covered in DistributionIntegrationTest.
 *
 * @author Grace Platform Team
 */
class DistributionPropertyTest {

    // ========== Property 6: Platform Routing Correctness ==========

    /**
     * Property 6: 平台路由正确性
     * <p>
     * 验证：VideoDistributorRegistry 根据平台标识正确路由到对应的分发器实现。
     * - 已注册平台应返回对应的分发器
     * - 未注册平台应抛出 UnsupportedPlatformException (错误码 3001)
     */
    @Property(tries = 100)
    @Label("Feature: video-distribution-platform, Property 6: 平台路由正确性")
    void property_6_platformRouting(
            @ForAll("platformIdentifiersWithMockDistributors") PlatformRoutingFixture fixture) {
        // Given: 创建 Registry 并注册测试分发器
        List<VideoDistributor> distributors = fixture.registeredDistributors();
        VideoDistributorRegistry registry = new VideoDistributorRegistry(distributors);

        // When & Then: 验证已注册平台的路由
        for (String platform : fixture.registeredPlatforms()) {
            VideoDistributor distributor = registry.getDistributor(platform);
            assertThat(distributor).isNotNull();
            assertThat(distributor.platform()).isEqualTo(platform);
        }

        // When & Then: 验证未注册平台的异常抛出
        for (String platform : fixture.unregisteredPlatforms()) {
            assertThatThrownBy(() -> registry.getDistributor(platform))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasFieldOrPropertyWithValue("errorCode", 3001)
                    .hasMessageContaining(platform)
                    .hasMessageContaining("不支持的分发平台");
        }
    }

    @Provide
    Arbitrary<PlatformRoutingFixture> platformIdentifiersWithMockDistributors() {
        // 预定义的已知平台
        Arbitrary<List<String>> registeredPlatforms = Arbitraries.of(
                List.of("youtube"),
                List.of("youtube", "bilibili"),
                List.of("youtube", "bilibili", "douyin")
        );

        // 未注册的随机平台
        Arbitrary<List<String>> unregisteredPlatforms = Arbitraries.strings()
                .alpha()
                .ofMinLength(3)
                .ofMaxLength(10)
                .list()
                .ofMinSize(1)
                .ofMaxSize(3);

        return Combinators.combine(registeredPlatforms, unregisteredPlatforms)
                .as((registered, unregistered) -> {
                    // 过滤掉与已注册平台重复的
                    List<String> filteredUnregistered = unregistered.stream()
                            .filter(p -> !registered.contains(p.toLowerCase()))
                            .distinct()
                            .toList();

                    // 创建测试分发器
                    List<VideoDistributor> distributors = registered.stream()
                            .map(MockVideoDistributor::new)
                            .map(d -> (VideoDistributor) d)
                            .toList();

                    return new PlatformRoutingFixture(registered, filteredUnregistered, distributors);
                });
    }

    // ========== Property 7: Publish Result Contains Valid Video URL ==========

    /**
     * Property 7: 发布结果包含有效视频 URL
     * <p>
     * 验证：发布成功后，PublishRecord 和 PublishResult 应包含非空且格式有效的视频 URL。
     * - 状态为 COMPLETED 时，videoUrl 应为非空
     * - videoUrl 应为合法的 URL 格式
     * - YouTube 平台 URL 应符合 YouTube 视频链接格式
     */
    @Property(tries = 100)
    @Label("Feature: video-distribution-platform, Property 7: 发布结果包含有效视频 URL")
    void property_7_publishResultContainsValidUrl(
            @ForAll("successfulPublishRecords") PublishRecord record) {
        // Then: 状态应为 COMPLETED
        assertThat(record.getStatus()).isEqualTo(PublishStatus.COMPLETED);

        // Then: videoUrl 应为非空
        assertThat(record.getVideoUrl()).isNotBlank();

        // Then: videoUrl 应为有效的 URL 格式
        assertThatCode(() -> new URL(record.getVideoUrl())).doesNotThrowAnyException();

        // Then: 当平台为 YouTube 时，URL 应匹配特定前缀
        if ("youtube".equals(record.getPlatform())) {
            assertThat(record.getVideoUrl()).satisfiesAnyOf(
                    url -> assertThat(url).startsWith("https://www.youtube.com/watch?v="),
                    url -> assertThat(url).startsWith("https://youtu.be/")
            );
        } else {
            // For non-YouTube platforms, just verify URL is valid
            assertThat(record.getVideoUrl()).startsWith("https://");
        }
    }

    @Property(tries = 100)
    @Label("Feature: video-distribution-platform, Property 7b: PublishResult 视频 URL 格式正确")
    void property_7b_publishResultValidUrl(
            @ForAll("validPublishResults") PublishResult result) {
        // When: 状态为 COMPLETED
        if (result.status() == PublishStatus.COMPLETED) {
            // Then: videoUrl 应为非空
            assertThat(result.videoUrl()).isNotBlank();

            // Then: videoUrl 应为有效的 URL 格式
            assertThatCode(() -> new URL(result.videoUrl())).doesNotThrowAnyException();
        }
    }

    @Provide
    Arbitrary<PublishRecord> successfulPublishRecords() {
        Arbitrary<VideoId> videoIds = Arbitraries.create(VideoId::generate);
        Arbitrary<MetadataId> metadataIds = Arbitraries.create(MetadataId::generate);

        // 为 YouTube 平台生成专用测试数据
        Arbitrary<PublishRecord> youtubeRecords = Arbitraries.strings()
                .alpha()
                .numeric()
                .withChars('-', '_')
                .ofLength(11)
                .map(videoIdPart -> "https://www.youtube.com/watch?v=" + videoIdPart)
                .map(youtubeUrl -> {
                    PublishRecord record = PublishRecord.create(
                            VideoId.generate(), MetadataId.generate(), "youtube"
                    );
                    record.markAsCompleted(youtubeUrl);
                    return record;
                });

        // 为其他平台生成测试数据（不包含 YouTube 格式验证）
        Arbitrary<PublishRecord> otherPlatformRecords = Arbitraries.of("bilibili", "douyin")
                .flatMap(platform -> Arbitraries.strings()
                        .alpha()
                        .numeric()
                        .ofMinLength(5)
                        .ofMaxLength(20)
                        .map(videoId -> "https://" + platform + ".com/watch/" + videoId)
                        .map(url -> {
                            String platformName = platform;
                            PublishRecord record = PublishRecord.create(
                                    VideoId.generate(), MetadataId.generate(), platformName
                            );
                            record.markAsCompleted(url);
                            return record;
                        }));

        return Arbitraries.oneOf(youtubeRecords, otherPlatformRecords);
    }

    @Provide
    Arbitrary<PublishResult> validPublishResults() {
        Arbitrary<String> taskIds = Arbitraries.strings()
                .alpha()
                .numeric()
                .ofMinLength(10)
                .ofMaxLength(50);
        Arbitrary<PublishStatus> statuses = Arbitraries.of(PublishStatus.COMPLETED);
        Arbitrary<String> videoUrls = Arbitraries.of(
                "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                "https://youtu.be/dQw4w9WgXcQ",
                "https://bilibili.com/video/BV1xx411c7mD",
                "https://douyin.com/video/1234567890"
        );

        return Combinators.combine(taskIds, statuses, videoUrls)
                .as(PublishResult::new);
    }

    // ========== Property 8: Publish Record Round-trip (Domain-level) ==========

    /**
     * Property 8: 发布记录持久化往返
     * <p>
     * 验证：PublishRecord 实体在创建和状态转换后，所有字段值保持正确。
     * 注意：完整的持久化往返测试（数据库读写）在 DistributionIntegrationTest 中覆盖。
     */
    @Property(tries = 100)
    @Label("Feature: video-distribution-platform, Property 8: 发布记录领域往返")
    void property_8_publishRecordRoundTrip(
            @ForAll("validPublishRecords") PublishRecord record) {
        // Then: 基本字段验证
        assertThat(record.getId()).isNotNull();
        assertThat(record.getVideoId()).isNotNull();
        assertThat(record.getMetadataId()).isNotNull();
        assertThat(record.getPlatform()).isNotBlank();
        assertThat(record.getStatus()).isNotNull();
        assertThat(record.getCreatedAt()).isNotNull();
        assertThat(record.getProgressPercent()).isBetween(0, 100);
        assertThat(record.getRetryCount()).isGreaterThanOrEqualTo(0);

        // Then: 状态一致性验证
        switch (record.getStatus()) {
            case COMPLETED -> {
                assertThat(record.getVideoUrl()).isNotBlank();
                assertThat(record.getPublishedAt()).isNotNull();
                assertThat(record.getProgressPercent()).isEqualTo(100);
            }
            case FAILED -> assertThat(record.getErrorMessage()).isNotBlank();
            case QUOTA_EXCEEDED -> {
                assertThat(record.getRetryCount()).isGreaterThanOrEqualTo(0);
            }
            default -> {
                // PENDING, UPLOADING - no special invariants
            }
        }
    }

    @Provide
    Arbitrary<PublishRecord> validPublishRecords() {
        Arbitrary<VideoId> videoIds = Arbitraries.create(VideoId::generate);
        Arbitrary<MetadataId> metadataIds = Arbitraries.create(MetadataId::generate);
        Arbitrary<String> platforms = Arbitraries.of("youtube", "bilibili", "douyin");
        Arbitrary<PublishStatus> statuses = Arbitraries.of(PublishStatus.values());

        return Combinators.combine(videoIds, metadataIds, platforms, statuses)
                .as((videoId, metadataId, platform, status) -> {
                    PublishRecord record = PublishRecord.create(videoId, metadataId, platform);

                    // 根据状态设置相应字段
                    switch (status) {
                        case UPLOADING -> record.markAsUploading("task-" + System.nanoTime());
                        case COMPLETED -> {
                            String videoUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ";
                            record.markAsUploading("task-" + System.nanoTime());
                            record.markAsCompleted(videoUrl);
                        }
                        case FAILED -> record.markAsFailed("Upload failed due to network error");
                        case QUOTA_EXCEEDED -> {
                            record.markAsUploading("task-" + System.nanoTime());
                            record.markAsQuotaExceeded();
                            record.incrementRetryCount();
                        }
                        default -> {
                            // PENDING - keep initial state
                        }
                    }

                    return record;
                });
    }

    // ========== Property 8b: PublishRecord 状态机正确性 ==========

    /**
     * Property 8b: 发布记录状态转换正确性
     * <p>
     * 验证：PublishRecord 状态机按照预期规则转换。
     */
    @Property(tries = 100)
    @Label("Feature: video-distribution-platform, Property 8b: 发布记录状态转换正确性")
    void property_8b_publishRecordStateTransitions(
            @ForAll("publishRecordTransitions") PublishRecordTransitionFixture fixture) {
        PublishRecord record = fixture.record();

        // 应用状态转换
        fixture.transitions().forEach(transition -> {
            switch (transition) {
                case MARK_UPLOADING -> record.markAsUploading("task-" + System.nanoTime());
                case MARK_COMPLETED -> record.markAsCompleted("https://www.youtube.com/watch?v=test123");
                case MARK_FAILED -> record.markAsFailed("Test error");
                case MARK_QUOTA_EXCEEDED -> record.markAsQuotaExceeded();
                case INCREMENT_RETRY -> record.incrementRetryCount();
                case UPDATE_PROGRESS -> record.updateProgress(50);
            }
        });

        // 验证最终状态
        assertThat(record.getStatus()).isEqualTo(fixture.expectedFinalStatus());
    }

    @Provide
    Arbitrary<PublishRecordTransitionFixture> publishRecordTransitions() {
        Arbitrary<VideoId> videoIds = Arbitraries.create(VideoId::generate);
        Arbitrary<MetadataId> metadataIds = Arbitraries.create(MetadataId::generate);

        // 定义合法的状态转换序列
        Arbitrary<List<StateTransition>> transitionSequences = Arbitraries.of(
                List.of(StateTransition.MARK_UPLOADING),
                List.of(StateTransition.MARK_UPLOADING, StateTransition.MARK_COMPLETED),
                List.of(StateTransition.MARK_UPLOADING, StateTransition.MARK_FAILED),
                List.of(StateTransition.MARK_UPLOADING, StateTransition.MARK_QUOTA_EXCEEDED),
                List.of(StateTransition.MARK_UPLOADING, StateTransition.UPDATE_PROGRESS),
                List.of(StateTransition.MARK_UPLOADING, StateTransition.MARK_QUOTA_EXCEEDED, StateTransition.INCREMENT_RETRY)
        );

        Arbitrary<String> platforms = Arbitraries.of("youtube", "bilibili");

        return Combinators.combine(videoIds, metadataIds, platforms, transitionSequences)
                .as((videoId, metadataId, platform, transitions) -> {
                    PublishRecord record = PublishRecord.create(videoId, metadataId, platform);

                    // 计算期望的最终状态
                    PublishStatus finalStatus = computeFinalStatus(transitions);

                    return new PublishRecordTransitionFixture(record, transitions, finalStatus);
                });
    }

    private PublishStatus computeFinalStatus(List<StateTransition> transitions) {
        PublishStatus status = PublishStatus.PENDING;
        for (StateTransition t : transitions) {
            switch (t) {
                case MARK_UPLOADING -> status = PublishStatus.UPLOADING;
                case MARK_COMPLETED -> status = PublishStatus.COMPLETED;
                case MARK_FAILED -> status = PublishStatus.FAILED;
                case MARK_QUOTA_EXCEEDED -> status = PublishStatus.QUOTA_EXCEEDED;
                default -> { /* 其他转换不改变状态 */ }
            }
        }
        return status;
    }

    // ========== Helper Classes ==========

    /**
     * 平台路由测试夹具
     */
    private record PlatformRoutingFixture(
            List<String> registeredPlatforms,
            List<String> unregisteredPlatforms,
            List<VideoDistributor> registeredDistributors
    ) {}

    /**
     * 发布记录状态转换夹具
     */
    private record PublishRecordTransitionFixture(
            PublishRecord record,
            List<StateTransition> transitions,
            PublishStatus expectedFinalStatus
    ) {}

    /**
     * 状态转换枚举
     */
    private enum StateTransition {
        MARK_UPLOADING,
        MARK_COMPLETED,
        MARK_FAILED,
        MARK_QUOTA_EXCEEDED,
        INCREMENT_RETRY,
        UPDATE_PROGRESS
    }

    /**
     * 模拟的视频分发器，用于测试 Registry
     */
    private static class MockVideoDistributor implements VideoDistributor {
        private final String platform;

        MockVideoDistributor(String platform) {
            this.platform = platform;
        }

        @Override
        public String platform() {
            return platform;
        }

        @Override
        public PublishResult publish(String storageUrl, VideoMetadata metadata) {
            return new PublishResult("task-" + System.nanoTime(), PublishStatus.UPLOADING);
        }

        @Override
        public UploadStatus getUploadStatus(String taskId) {
            return new UploadStatus(taskId, PublishStatus.UPLOADING, 50);
        }
    }
}
