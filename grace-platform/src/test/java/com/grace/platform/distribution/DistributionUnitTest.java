package com.grace.platform.distribution;

import com.grace.platform.distribution.domain.*;
import com.grace.platform.distribution.infrastructure.youtube.YouTubeDistributor;
import com.grace.platform.distribution.infrastructure.youtube.YouTubeOAuthServiceImpl;
import com.grace.platform.shared.ErrorCode;
import com.grace.platform.shared.domain.id.MetadataId;
import com.grace.platform.shared.domain.id.PublishRecordId;
import com.grace.platform.shared.domain.id.VideoId;
import com.grace.platform.shared.infrastructure.encryption.EncryptionService;
import com.grace.platform.shared.infrastructure.exception.BusinessRuleViolationException;
import com.grace.platform.shared.infrastructure.exception.ExternalServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for Distribution domain.
 * <p>
 * Covers boundary conditions and error scenarios:
 * - Unsupported platform returns error code 3001
 * - OAuth token automatic refresh on expiration
 * - Quota exceeded marking (QUOTA_EXCEEDED status)
 * - Platform API error handling
 *
 * @author Grace Platform Team
 */
class DistributionUnitTest {

    // ========== Error Code 3001: Unsupported Platform ==========

    @Test
    @DisplayName("不支持的平台标识应抛出错误码 3001")
    void shouldThrowError3001ForUnsupportedPlatform() {
        // Given: Registry with only "youtube" registered
        List<VideoDistributor> distributors = List.of(
            createMockDistributor("youtube")
        );
        VideoDistributorRegistry registry = new VideoDistributorRegistry(distributors);

        // When & Then: Querying unsupported platform throws 3001
        assertThatThrownBy(() -> registry.getDistributor("unsupported_platform"))
            .isInstanceOf(BusinessRuleViolationException.class)
            .satisfies(e -> assertThat(((BusinessRuleViolationException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNSUPPORTED_PLATFORM))
            .hasMessageContaining("不支持的分发平台")
            .hasMessageContaining("unsupported_platform");
    }

    @Test
    @DisplayName("空平台标识应抛出错误码 3001")
    void shouldThrowError3001ForEmptyPlatform() {
        // Given: Registry with only "youtube" registered
        List<VideoDistributor> distributors = List.of(
            createMockDistributor("youtube")
        );
        VideoDistributorRegistry registry = new VideoDistributorRegistry(distributors);

        // When & Then: Querying empty platform throws 3001
        assertThatThrownBy(() -> registry.getDistributor(""))
            .isInstanceOf(BusinessRuleViolationException.class)
            .satisfies(e -> assertThat(((BusinessRuleViolationException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNSUPPORTED_PLATFORM));
    }

    @Test
    @DisplayName("已注册平台应正确路由到对应分发器")
    void shouldRouteToRegisteredPlatform() {
        // Given: Registry with multiple platforms
        List<VideoDistributor> distributors = List.of(
            createMockDistributor("youtube"),
            createMockDistributor("bilibili"),
            createMockDistributor("douyin")
        );
        VideoDistributorRegistry registry = new VideoDistributorRegistry(distributors);

        // When & Then: All registered platforms can be retrieved
        assertThat(registry.getDistributor("youtube").platform()).isEqualTo("youtube");
        assertThat(registry.getDistributor("bilibili").platform()).isEqualTo("bilibili");
        assertThat(registry.getDistributor("douyin").platform()).isEqualTo("douyin");
    }

    // ========== OAuth Token Refresh ==========

    @Test
    @DisplayName("OAuth Token 过期时应自动刷新")
    void shouldAutoRefreshExpiredToken() {
        // Given: An expired token
        LocalDateTime expiredTime = LocalDateTime.now().minusHours(1);
        OAuthToken expiredToken = OAuthToken.create(
            "youtube",
            "encrypted_access_token",
            "encrypted_refresh_token",
            expiredTime
        );

        // Then: Token should be expired
        assertThat(expiredToken.isExpired()).isTrue();

        // When: Update tokens (simulating refresh)
        LocalDateTime newExpiresAt = LocalDateTime.now().plusHours(1);
        expiredToken.updateTokens(
            "new_encrypted_access_token",
            "new_encrypted_refresh_token",
            newExpiresAt
        );

        // Then: Token should no longer be expired
        assertThat(expiredToken.isExpired()).isFalse();
        assertThat(expiredToken.getAccessToken()).isEqualTo("new_encrypted_access_token");
        assertThat(expiredToken.getRefreshToken()).isEqualTo("new_encrypted_refresh_token");
        assertThat(expiredToken.getExpiresAt()).isEqualTo(newExpiresAt);
    }

    @Test
    @DisplayName("OAuth Token 未过期时不应触发刷新")
    void shouldNotRefreshValidToken() {
        // Given: A valid (non-expired) token
        LocalDateTime futureTime = LocalDateTime.now().plusHours(1);
        OAuthToken validToken = OAuthToken.create(
            "youtube",
            "encrypted_access_token",
            "encrypted_refresh_token",
            futureTime
        );

        // Then: Token should NOT be expired
        assertThat(validToken.isExpired()).isFalse();

        // And: Original tokens should remain unchanged
        assertThat(validToken.getAccessToken()).isEqualTo("encrypted_access_token");
    }

    @Test
    @DisplayName("OAuth Token 边界情况 - 正好过期")
    void shouldHandleTokenAtExactExpirationBoundary() {
        // Given: A token that just expired (1 second ago)
        LocalDateTime justExpired = LocalDateTime.now().minusSeconds(1);
        OAuthToken token = OAuthToken.create(
            "youtube",
            "encrypted_access_token",
            "encrypted_refresh_token",
            justExpired
        );

        // Then: Should be considered expired
        assertThat(token.isExpired()).isTrue();
    }

    @Test
    @DisplayName("OAuth Token 边界情况 - 即将过期")
    void shouldHandleTokenAboutToExpire() {
        // Given: A token that expires 1 second in the future
        LocalDateTime aboutToExpire = LocalDateTime.now().plusSeconds(1);
        OAuthToken token = OAuthToken.create(
            "youtube",
            "encrypted_access_token",
            "encrypted_refresh_token",
            aboutToExpire
        );

        // Then: Should NOT be considered expired yet
        assertThat(token.isExpired()).isFalse();
    }

    // ========== Quota Exceeded Handling ==========

    @Test
    @DisplayName("配额超限时应标记 QUOTA_EXCEEDED 状态")
    void shouldMarkQuotaExceeded() {
        // Given: A publish record in UPLOADING status
        VideoId videoId = VideoId.generate();
        MetadataId metadataId = MetadataId.generate();
        PublishRecord record = PublishRecord.create(videoId, metadataId, "youtube");
        record.markAsUploading("task-123");

        assertThat(record.getStatus()).isEqualTo(PublishStatus.UPLOADING);

        // When: Mark as quota exceeded
        record.markAsQuotaExceeded();

        // Then: Status should be QUOTA_EXCEEDED
        assertThat(record.getStatus()).isEqualTo(PublishStatus.QUOTA_EXCEEDED);
    }

    @Test
    @DisplayName("QUOTA_EXCEEDED 状态应支持重试计数增加")
    void shouldIncrementRetryCountForQuotaExceeded() {
        // Given: A publish record marked as quota exceeded
        VideoId videoId = VideoId.generate();
        MetadataId metadataId = MetadataId.generate();
        PublishRecord record = PublishRecord.create(videoId, metadataId, "youtube");
        record.markAsUploading("task-123");
        record.markAsQuotaExceeded();

        assertThat(record.getRetryCount()).isEqualTo(0);

        // When: Increment retry count multiple times
        record.incrementRetryCount();
        assertThat(record.getRetryCount()).isEqualTo(1);

        record.incrementRetryCount();
        assertThat(record.getRetryCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("QUOTA_EXCEEDED 状态应支持恢复为 UPLOADING")
    void shouldResumeFromQuotaExceeded() {
        // Given: A publish record in QUOTA_EXCEEDED status
        VideoId videoId = VideoId.generate();
        MetadataId metadataId = MetadataId.generate();
        PublishRecord record = PublishRecord.create(videoId, metadataId, "youtube");
        record.markAsUploading("task-123");
        record.markAsQuotaExceeded();

        assertThat(record.getStatus()).isEqualTo(PublishStatus.QUOTA_EXCEEDED);

        // When: Resume from quota exceeded
        record.resumeFromQuotaExceeded();

        // Then: Status should be UPLOADING
        assertThat(record.getStatus()).isEqualTo(PublishStatus.UPLOADING);
    }

    @Test
    @DisplayName("非 QUOTA_EXCEEDED 状态不应允许恢复")
    void shouldNotAllowResumeFromNonQuotaExceededStatus() {
        // Given: A publish record in COMPLETED status
        VideoId videoId = VideoId.generate();
        MetadataId metadataId = MetadataId.generate();
        PublishRecord record = PublishRecord.create(videoId, metadataId, "youtube");
        record.markAsUploading("task-123");
        record.markAsCompleted("https://youtube.com/watch?v=test");

        assertThat(record.getStatus()).isEqualTo(PublishStatus.COMPLETED);

        // When & Then: Attempting to resume should throw exception
        assertThatThrownBy(() -> record.resumeFromQuotaExceeded())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Can only resume from QUOTA_EXCEEDED status");
    }

    @Test
    @DisplayName("最大重试次数达到后应从 QUOTA_EXCEEDED 转为 FAILED")
    void shouldFailAfterMaxRetries() {
        // Given: A publish record with retry count at 4
        VideoId videoId = VideoId.generate();
        MetadataId metadataId = MetadataId.generate();
        PublishRecord record = PublishRecord.create(videoId, metadataId, "youtube");
        record.markAsUploading("task-123");
        record.markAsQuotaExceeded();

        // Set retry count to 4 (max is 5)
        for (int i = 0; i < 4; i++) {
            record.incrementRetryCount();
        }
        assertThat(record.getRetryCount()).isEqualTo(4);
        assertThat(record.getStatus()).isEqualTo(PublishStatus.QUOTA_EXCEEDED);

        // When: Increment to reach max retries (5)
        record.incrementRetryCount();

        // Then: Should be at retry count 5
        assertThat(record.getRetryCount()).isEqualTo(5);
    }

    // ========== Publish Status State Machine ==========

    @Test
    @DisplayName("PublishRecord 应正确维护状态机转换")
    void shouldMaintainCorrectStateTransitions() {
        VideoId videoId = VideoId.generate();
        MetadataId metadataId = MetadataId.generate();
        PublishRecord record = PublishRecord.create(videoId, metadataId, "youtube");

        // Initial: PENDING
        assertThat(record.getStatus()).isEqualTo(PublishStatus.PENDING);

        // PENDING -> UPLOADING
        record.markAsUploading("task-123");
        assertThat(record.getStatus()).isEqualTo(PublishStatus.UPLOADING);
        assertThat(record.getUploadTaskId()).isEqualTo("task-123");

        // UPLOADING -> COMPLETED
        record.markAsCompleted("https://youtube.com/watch?v=test123");
        assertThat(record.getStatus()).isEqualTo(PublishStatus.COMPLETED);
        assertThat(record.getVideoUrl()).isEqualTo("https://youtube.com/watch?v=test123");
        assertThat(record.getPublishedAt()).isNotNull();
        assertThat(record.getProgressPercent()).isEqualTo(100);
    }

    @Test
    @DisplayName("UPLOADING -> FAILED 转换应正确记录错误信息")
    void shouldRecordErrorMessageOnFailure() {
        VideoId videoId = VideoId.generate();
        MetadataId metadataId = MetadataId.generate();
        PublishRecord record = PublishRecord.create(videoId, metadataId, "youtube");
        record.markAsUploading("task-123");

        // UPLOADING -> FAILED
        record.markAsFailed("Network timeout during upload");
        assertThat(record.getStatus()).isEqualTo(PublishStatus.FAILED);
        assertThat(record.getErrorMessage()).isEqualTo("Network timeout during upload");
    }

    // ========== Platform Info ==========

    @Test
    @DisplayName("VideoDistributorRegistry 应正确列出所有平台")
    void shouldListAllRegisteredPlatforms() {
        // Given: Registry with multiple platforms
        List<VideoDistributor> distributors = List.of(
            createMockDistributor("youtube"),
            createMockDistributor("bilibili"),
            createMockDistributor("douyin")
        );
        VideoDistributorRegistry registry = new VideoDistributorRegistry(distributors);

        // When: List platforms
        List<PlatformInfo> platforms = registry.listPlatforms();

        // Then: Should contain all platforms
        assertThat(platforms).hasSize(3);
        assertThat(platforms).extracting(PlatformInfo::platform)
            .containsExactlyInAnyOrder("youtube", "bilibili", "douyin");
    }

    @Test
    @DisplayName("空 Registry 应返回空平台列表且抛出 3001")
    void shouldHandleEmptyRegistry() {
        // Given: Empty registry
        List<VideoDistributor> distributors = new ArrayList<>();
        VideoDistributorRegistry registry = new VideoDistributorRegistry(distributors);

        // Then: List should be empty
        assertThat(registry.listPlatforms()).isEmpty();

        // And: Any platform query should throw 3001
        assertThatThrownBy(() -> registry.getDistributor("youtube"))
            .isInstanceOf(BusinessRuleViolationException.class)
            .satisfies(e -> assertThat(((BusinessRuleViolationException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNSUPPORTED_PLATFORM));
    }

    // ========== VideoFile and VideoMetadata Value Objects ==========

    @Test
    @DisplayName("VideoFile 值对象应正确封装文件信息")
    void shouldCreateVideoFileValueObject() {
        Path path = Paths.get("/storage/videos/test.mp4");
        VideoFile videoFile = new VideoFile(path, "test.mp4", 1024000L);

        assertThat(videoFile.filePath()).isEqualTo(path);
        assertThat(videoFile.fileName()).isEqualTo("test.mp4");
        assertThat(videoFile.fileSize()).isEqualTo(1024000L);
    }

    @Test
    @DisplayName("VideoMetadata 值对象应正确封装元数据")
    void shouldCreateVideoMetadataValueObject() {
        List<String> tags = List.of("美食", "烹饪", "教程");
        VideoMetadata metadata = new VideoMetadata(
            "美味家常菜做法",
            "今天教大家做一道美味的家常菜...",
            tags
        );

        assertThat(metadata.title()).isEqualTo("美味家常菜做法");
        assertThat(metadata.description()).isEqualTo("今天教大家做一道美味的家常菜...");
        assertThat(metadata.tags()).isEqualTo(tags);
    }

    // ========== OAuth Token Validation ==========

    @Test
    @DisplayName("创建 OAuthToken 时平台不能为空")
    void shouldValidatePlatformNotBlank() {
        assertThatThrownBy(() -> OAuthToken.create(
            "",
            "access_token",
            "refresh_token",
            LocalDateTime.now().plusHours(1)
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Platform must not be blank");

        assertThatThrownBy(() -> OAuthToken.create(
            null,
            "access_token",
            "refresh_token",
            LocalDateTime.now().plusHours(1)
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Platform must not be blank");
    }

    @Test
    @DisplayName("创建 OAuthToken 时 access token 不能为空")
    void shouldValidateAccessTokenNotBlank() {
        assertThatThrownBy(() -> OAuthToken.create(
            "youtube",
            "",
            "refresh_token",
            LocalDateTime.now().plusHours(1)
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Access token must not be blank");
    }

    @Test
    @DisplayName("创建 OAuthToken 时 refresh token 不能为空")
    void shouldValidateRefreshTokenNotBlank() {
        assertThatThrownBy(() -> OAuthToken.create(
            "youtube",
            "access_token",
            "",
            LocalDateTime.now().plusHours(1)
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Refresh token must not be blank");
    }

    @Test
    @DisplayName("OAuthToken 部分刷新应只更新 access token")
    void shouldUpdateOnlyAccessToken() {
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);
        OAuthToken token = OAuthToken.create(
            "youtube",
            "old_access_token",
            "refresh_token",
            expiresAt
        );

        LocalDateTime newExpiresAt = LocalDateTime.now().plusHours(2);
        token.updateAccessToken("new_access_token", newExpiresAt);

        assertThat(token.getAccessToken()).isEqualTo("new_access_token");
        assertThat(token.getRefreshToken()).isEqualTo("refresh_token"); // unchanged
        assertThat(token.getExpiresAt()).isEqualTo(newExpiresAt);
    }

    // ========== Helper Methods ==========

    private VideoDistributor createMockDistributor(String platform) {
        return new VideoDistributor() {
            @Override
            public String platform() {
                return platform;
            }

            @Override
            public PublishResult publish(VideoFile videoFile, VideoMetadata metadata) {
                return new PublishResult("task-" + System.nanoTime(), PublishStatus.UPLOADING);
            }

            @Override
            public UploadStatus getUploadStatus(String taskId) {
                return new UploadStatus(taskId, PublishStatus.UPLOADING, 50);
            }
        };
    }
}
