package com.grace.platform.testutil;

import com.grace.platform.shared.domain.id.*;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;

import java.util.List;

/**
 * jqwik custom arbitrary generators for property-based testing.
 * Domain-specific arbitraries will be added as domain entities are implemented.
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

    // --- Domain Enum Arbitraries (to be implemented with domain entities) ---
    // public static Arbitrary<VideoFormat> videoFormats() { ... }
    // public static Arbitrary<ChannelType> channelTypes() { ... }
    // public static Arbitrary<PromotionMethod> promotionMethods() { ... }
}
