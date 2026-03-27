package com.grace.platform.testutil;

import com.grace.platform.distribution.domain.PublishRecord;
import com.grace.platform.distribution.domain.PublishStatus;
import com.grace.platform.metadata.domain.MetadataSource;
import com.grace.platform.metadata.domain.VideoMetadata;
import com.grace.platform.promotion.domain.*;
import com.grace.platform.shared.domain.id.*;
import com.grace.platform.usersettings.domain.model.ApiKey;
import com.grace.platform.video.domain.UploadSession;
import com.grace.platform.video.domain.Video;
import com.grace.platform.video.domain.VideoFormat;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Test data factory for creating test fixtures.
 * Provides factory methods for all domain entities across all bounded contexts.
 */
public final class TestFixtures {

    private TestFixtures() {}

    // --- ID Generation Helpers ---

    public static VideoId randomVideoId() {
        return VideoId.generate();
    }

    public static MetadataId randomMetadataId() {
        return MetadataId.generate();
    }

    public static PublishRecordId randomPublishRecordId() {
        return PublishRecordId.generate();
    }

    public static OAuthTokenId randomOAuthTokenId() {
        return OAuthTokenId.generate();
    }

    public static ChannelId randomChannelId() {
        return ChannelId.generate();
    }

    public static PromotionRecordId randomPromotionRecordId() {
        return PromotionRecordId.generate();
    }

    public static UserProfileId randomUserProfileId() {
        return UserProfileId.generate();
    }

    public static NotificationPreferenceId randomNotificationPreferenceId() {
        return NotificationPreferenceId.generate();
    }

    public static ApiKeyId randomApiKeyId() {
        return ApiKeyId.generate();
    }

    // --- Common Test Values ---

    public static String randomUploadId() {
        return "upl_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    public static String randomString(int length) {
        return UUID.randomUUID().toString().replace("-", "").substring(0, Math.min(length, 32));
    }

    public static LocalDateTime now() {
        return LocalDateTime.now();
    }

    // --- Video Context Fixtures ---

    /**
     * Create a test Video entity.
     *
     * @return Video with test values
     */
    public static Video createVideo() {
        return Video.create(
            "测试视频.mp4",
            10_000_000L,
            VideoFormat.MP4,
            Duration.ofSeconds(120),
            "/storage/videos/test.mp4"
        );
    }

    /**
     * Create a test UploadSession entity.
     *
     * @return UploadSession with test values
     */
    public static UploadSession createUploadSession() {
        return UploadSession.create(
            "测试视频.mp4",
            10_000_000L,
            VideoFormat.MP4,
            "/storage/temp/upload-xxx",
            null  // Use default chunk size
        );
    }

    // --- Metadata Context Fixtures ---

    /**
     * Create a test VideoMetadata entity.
     *
     * @param videoId The video ID to associate with metadata
     * @return VideoMetadata with test values
     */
    public static VideoMetadata createMetadata(VideoId videoId) {
        return VideoMetadata.create(
            videoId,
            "AI 生成标题",
            "AI 生成的描述内容",
            List.of("美食", "烹饪", "教程", "厨房", "食谱"),
            MetadataSource.AI_GENERATED
        );
    }

    // --- Distribution Context Fixtures ---

    /**
     * Create a test PublishRecord entity.
     *
     * @param videoId The video ID to associate with publish record
     * @return PublishRecord with test values
     */
    public static PublishRecord createPublishRecord(VideoId videoId) {
        return PublishRecord.create(
            videoId,
            randomMetadataId(),
            "youtube"
        );
    }

    /**
     * Create a test PublishRecord entity with specific metadata ID.
     *
     * @param videoId    The video ID to associate with publish record
     * @param metadataId The metadata ID to associate with publish record
     * @return PublishRecord with test values
     */
    public static PublishRecord createPublishRecord(VideoId videoId, MetadataId metadataId) {
        return PublishRecord.create(
            videoId,
            metadataId,
            "youtube"
        );
    }

    // --- Promotion Context Fixtures ---

    /**
     * Create a test PromotionChannel entity.
     *
     * @return PromotionChannel with test values
     */
    public static PromotionChannel createChannel() {
        return PromotionChannel.create(
            "测试渠道",
            ChannelType.SOCIAL_MEDIA,
            "https://example.com",
            1  // priority
        );
    }

    /**
     * Create a test PromotionChannel entity with specific type.
     *
     * @param type     The channel type
     * @param priority The channel priority (1-99)
     * @return PromotionChannel with test values
     */
    public static PromotionChannel createChannel(ChannelType type, int priority) {
        return PromotionChannel.create(
            "测试渠道-" + type.name(),
            type,
            "https://example.com/" + type.name().toLowerCase(),
            priority
        );
    }

    /**
     * Create a test PromotionRecord entity.
     *
     * @param videoId   The video ID to associate with promotion record
     * @param channelId The channel ID to associate with promotion record
     * @return PromotionRecord with test values
     */
    public static PromotionRecord createPromotionRecord(VideoId videoId, ChannelId channelId) {
        return PromotionRecord.create(
            videoId,
            channelId,
            "推广文案内容",
            PromotionMethod.POST
        );
    }

    /**
     * Create a test PromotionRecord entity with specific method.
     *
     * @param videoId   The video ID to associate with promotion record
     * @param channelId The channel ID to associate with promotion record
     * @param method    The promotion method
     * @return PromotionRecord with test values
     */
    public static PromotionRecord createPromotionRecord(VideoId videoId, ChannelId channelId, PromotionMethod method) {
        return PromotionRecord.create(
            videoId,
            channelId,
            "推广文案内容-" + method.name(),
            method
        );
    }

    // --- User & Settings Context Fixtures ---

    /**
     * Create a test ApiKey entity.
     *
     * @return ApiKey with test values
     */
    public static ApiKey createApiKey() {
        return ApiKey.create(
            randomApiKeyId(),
            "Test API Key",
            "$2a$12$hashedKeyValueForTestingPurposesOnly123456",
            "grc_test...key1",
            LocalDateTime.now().plusDays(90)
        );
    }

    /**
     * Create a test ApiKey entity with specific name.
     *
     * @param name The API key name/description
     * @return ApiKey with test values
     */
    public static ApiKey createApiKey(String name) {
        return ApiKey.create(
            randomApiKeyId(),
            name,
            "$2a$12$hashedKeyValueForTestingPurposesOnly123456",
            "grc_" + randomString(8) + "..." + randomString(4),
            LocalDateTime.now().plusDays(90)
        );
    }
}
