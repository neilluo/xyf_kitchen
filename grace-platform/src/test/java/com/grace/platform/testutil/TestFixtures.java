package com.grace.platform.testutil;

import com.grace.platform.shared.domain.id.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Test data factory for creating test fixtures.
 * Domain entity factory methods will be added as entities are implemented.
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

    // --- Video Context Fixtures (to be implemented with domain entities) ---
    // public static Video createVideo() { ... }
    // public static UploadSession createUploadSession() { ... }

    // --- Metadata Context Fixtures (to be implemented with domain entities) ---
    // public static VideoMetadata createMetadata(VideoId videoId) { ... }

    // --- Distribution Context Fixtures (to be implemented with domain entities) ---
    // public static PublishRecord createPublishRecord(VideoId videoId) { ... }

    // --- Promotion Context Fixtures (to be implemented with domain entities) ---
    // public static PromotionChannel createChannel() { ... }
    // public static PromotionRecord createPromotionRecord(VideoId videoId, ChannelId channelId) { ... }

    // --- User & Settings Context Fixtures (to be implemented with domain entities) ---
    // public static ApiKey createApiKey() { ... }
}
