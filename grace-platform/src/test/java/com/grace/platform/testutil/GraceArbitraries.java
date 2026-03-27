package com.grace.platform.testutil;

import com.grace.platform.distribution.domain.PublishStatus;
import com.grace.platform.metadata.domain.MetadataSource;
import com.grace.platform.promotion.domain.*;
import com.grace.platform.shared.domain.id.*;
import com.grace.platform.video.domain.VideoFormat;
import com.grace.platform.video.domain.VideoStatus;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;

import java.util.List;

/**
 * jqwik custom arbitrary generators for property-based testing.
 * Provides domain-specific arbitraries for all enums and value objects.
 */
public final class GraceArbitraries {

    private GraceArbitraries() {}

    // --- ID Arbitraries ---

    public static Arbitrary<VideoId> videoIds() {
        return Arbitraries.create(VideoId::generate);
    }

    public static Arbitrary<MetadataId> metadataIds() {
        return Arbitraries.create(MetadataId::generate);
    }

    public static Arbitrary<PublishRecordId> publishRecordIds() {
        return Arbitraries.create(PublishRecordId::generate);
    }

    public static Arbitrary<OAuthTokenId> oAuthTokenIds() {
        return Arbitraries.create(OAuthTokenId::generate);
    }

    public static Arbitrary<ChannelId> channelIds() {
        return Arbitraries.create(ChannelId::generate);
    }

    public static Arbitrary<PromotionRecordId> promotionRecordIds() {
        return Arbitraries.create(PromotionRecordId::generate);
    }

    public static Arbitrary<UserProfileId> userProfileIds() {
        return Arbitraries.create(UserProfileId::generate);
    }

    public static Arbitrary<NotificationPreferenceId> notificationPreferenceIds() {
        return Arbitraries.create(NotificationPreferenceId::generate);
    }

    public static Arbitrary<ApiKeyId> apiKeyIds() {
        return Arbitraries.create(ApiKeyId::generate);
    }

    // --- Common Value Arbitraries ---

    public static Arbitrary<String> uploadIds() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(12)
                .ofMaxLength(32)
                .map(s -> "upl_" + s.toLowerCase());
    }

    public static Arbitrary<String> videoFileNames() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(50)
                .map(name -> name + "." + Arbitraries.of("mp4", "mov", "avi", "mkv").sample());
    }

    public static Arbitrary<Long> validFileSizes() {
        return Arbitraries.longs().between(1, 5L * 1024 * 1024 * 1024);
    }

    public static Arbitrary<Long> validDurations() {
        return Arbitraries.longs().between(1, 86400);
    }

    public static Arbitrary<String> metadataTitles() {
        return Arbitraries.strings().ofMinLength(1).ofMaxLength(100);
    }

    public static Arbitrary<String> metadataDescriptions() {
        return Arbitraries.strings().ofMinLength(0).ofMaxLength(5000);
    }

    public static Arbitrary<List<String>> metadataTags() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(20)
                .list()
                .ofMinSize(5)
                .ofMaxSize(15);
    }

    public static Arbitrary<String> platformIdentifiers() {
        // Mix of known platforms and random strings
        return Arbitraries.oneOf(
                Arbitraries.of("youtube", "bilibili", "douyin"),
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20)
        );
    }

    // --- Video Domain Enum Arbitraries ---

    /**
     * Arbitrary for VideoFormat enum values.
     *
     * @return Arbitrary of VideoFormat
     */
    public static Arbitrary<VideoFormat> videoFormats() {
        return Arbitraries.of(VideoFormat.values());
    }

    /**
     * Arbitrary for VideoStatus enum values.
     *
     * @return Arbitrary of VideoStatus
     */
    public static Arbitrary<VideoStatus> videoStatuses() {
        return Arbitraries.of(VideoStatus.values());
    }

    // --- Metadata Domain Enum Arbitraries ---

    /**
     * Arbitrary for MetadataSource enum values.
     *
     * @return Arbitrary of MetadataSource
     */
    public static Arbitrary<MetadataSource> metadataSources() {
        return Arbitraries.of(MetadataSource.values());
    }

    // --- Distribution Domain Enum Arbitraries ---

    /**
     * Arbitrary for PublishStatus enum values.
     *
     * @return Arbitrary of PublishStatus
     */
    public static Arbitrary<PublishStatus> publishStatuses() {
        return Arbitraries.of(PublishStatus.values());
    }

    // --- Promotion Domain Enum Arbitraries ---

    /**
     * Arbitrary for ChannelType enum values.
     *
     * @return Arbitrary of ChannelType
     */
    public static Arbitrary<ChannelType> channelTypes() {
        return Arbitraries.of(ChannelType.values());
    }

    /**
     * Arbitrary for ChannelStatus enum values.
     *
     * @return Arbitrary of ChannelStatus
     */
    public static Arbitrary<ChannelStatus> channelStatuses() {
        return Arbitraries.of(ChannelStatus.values());
    }

    /**
     * Arbitrary for PromotionMethod enum values.
     *
     * @return Arbitrary of PromotionMethod
     */
    public static Arbitrary<PromotionMethod> promotionMethods() {
        return Arbitraries.of(PromotionMethod.values());
    }

    /**
     * Arbitrary for PromotionStatus enum values.
     *
     * @return Arbitrary of PromotionStatus
     */
    public static Arbitrary<PromotionStatus> promotionStatuses() {
        return Arbitraries.of(PromotionStatus.values());
    }

    // --- Raw Value Arbitraries for Testing ---

    /**
     * Arbitrary for raw API keys (plaintext).
     *
     * @return Arbitrary of String representing API keys
     */
    public static Arbitrary<String> rawApiKeys() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(10)
                .ofMaxLength(100);
    }

    /**
     * Arbitrary for channel names.
     *
     * @return Arbitrary of String representing channel names
     */
    public static Arbitrary<String> channelNames() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(50);
    }

    /**
     * Arbitrary for channel URLs.
     *
     * @return Arbitrary of String representing channel URLs
     */
    public static Arbitrary<String> channelUrls() {
        return Arbitraries.of(
                "https://twitter.com/test",
                "https://weibo.com/test",
                "https://www.xiaohongshu.com/test",
                "https://www.douban.com/test",
                "https://medium.com/test"
        );
    }

    /**
     * Arbitrary for promotion copy content.
     *
     * @return Arbitrary of String representing promotion copy
     */
    public static Arbitrary<String> promotionCopies() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(10)
                .ofMaxLength(500)
                .map(s -> "Check out this video: https://example.com/video " + s);
    }

    /**
     * Arbitrary for valid YouTube video URLs.
     *
     * @return Arbitrary of String representing YouTube video URLs
     */
    public static Arbitrary<String> youtubeVideoUrls() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(11)
                .ofMaxLength(11)
                .map(id -> "https://www.youtube.com/watch?v=" + id);
    }

    /**
     * Arbitrary for valid video URLs (generic).
     *
     * @return Arbitrary of String representing video URLs
     */
    public static Arbitrary<String> videoUrls() {
        return Arbitraries.oneOf(
                youtubeVideoUrls(),
                Arbitraries.of(
                        "https://www.bilibili.com/video/BV1xx411c7mD",
                        "https://v.douyin.com/xxxxx",
                        "https://example.com/video/12345"
                )
        );
    }
}
