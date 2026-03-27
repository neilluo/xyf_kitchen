package com.grace.platform.usersettings;

import com.grace.platform.GracePlatformApplication;
import com.grace.platform.shared.domain.id.ApiKeyId;
import com.grace.platform.shared.domain.id.NotificationPreferenceId;
import com.grace.platform.shared.domain.id.UserProfileId;
import com.grace.platform.testutil.AbstractIntegrationTest;
import com.grace.platform.usersettings.application.UserSettingsApplicationService;
import com.grace.platform.usersettings.application.dto.*;
import com.grace.platform.usersettings.domain.model.ApiKey;
import com.grace.platform.usersettings.domain.model.NotificationPreference;
import com.grace.platform.usersettings.domain.model.UserProfile;
import com.grace.platform.usersettings.domain.repository.ApiKeyRepository;
import com.grace.platform.usersettings.domain.repository.NotificationPreferenceRepository;
import com.grace.platform.usersettings.domain.repository.UserProfileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UserSettings 上下文集成测试。
 * <p>
 * 测试设置 CRUD：获取/更新 Profile → 上传头像 → 通知偏好 → API Key 生命周期。
 * 继承 AbstractIntegrationTest 使用 Testcontainers MySQL 8.0。
 * </p>
 */
@SpringBootTest(classes = GracePlatformApplication.class)
class UserSettingsIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserSettingsApplicationService userSettingsService;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private NotificationPreferenceRepository notificationPreferenceRepository;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    // MVP 单用户模式固定 ID
    private static final UserProfileId DEFAULT_USER_ID = new UserProfileId("default-user");
    private static final NotificationPreferenceId DEFAULT_NOTIFICATION_ID = new NotificationPreferenceId("default-notification");

    @Test
    @DisplayName("用户资料获取与更新往返")
    void profileGetAndUpdateRoundTrip() {
        // 1. Get profile - 获取用户资料
        ProfileResponse profile = userSettingsService.getProfile();
        
        assertThat(profile).isNotNull();
        assertThat(profile.displayName()).isNotBlank();
        String originalName = profile.displayName();

        // 2. Update profile - 更新用户资料
        UpdateProfileRequest updateRequest = new UpdateProfileRequest("更新后的显示名称", "updated@example.com");
        ProfileResponse updated = userSettingsService.updateProfile(updateRequest);
        
        assertThat(updated).isNotNull();
        assertThat(updated.displayName()).isEqualTo("更新后的显示名称");
        assertThat(updated.email()).isEqualTo("updated@example.com");

        // 3. Verify persistence - 验证持久化
        Optional<UserProfile> found = userProfileRepository.findById(DEFAULT_USER_ID);
        assertThat(found).isPresent();
        assertThat(found.get().getDisplayName()).isEqualTo("更新后的显示名称");
        assertThat(found.get().getEmail()).isEqualTo("updated@example.com");

        // Restore original name for other tests
        UpdateProfileRequest restoreRequest = new UpdateProfileRequest(originalName, profile.email());
        userSettingsService.updateProfile(restoreRequest);
    }

    @Test
    @DisplayName("通知偏好获取与更新往返")
    void notificationPreferenceGetAndUpdateRoundTrip() {
        // 1. Get notification preference - 获取通知偏好
        NotificationPreferenceResponse preference = userSettingsService.getNotificationPreference();
        
        assertThat(preference).isNotNull();
        // 记录原始值
        boolean originalUploadComplete = preference.uploadComplete();
        boolean originalPromotionSuccess = preference.promotionSuccess();
        boolean originalSystemUpdates = preference.systemUpdates();

        // 2. Update notification preference - 更新通知偏好
        UpdateNotificationRequest updateRequest = new UpdateNotificationRequest(false, false, true);
        NotificationPreferenceResponse updated = userSettingsService.updateNotificationPreference(updateRequest);
        
        assertThat(updated).isNotNull();
        assertThat(updated.uploadComplete()).isFalse();
        assertThat(updated.promotionSuccess()).isFalse();
        assertThat(updated.systemUpdates()).isTrue();

        // 3. Verify persistence - 验证持久化
        Optional<NotificationPreference> found = notificationPreferenceRepository.findById(DEFAULT_NOTIFICATION_ID);
        assertThat(found).isPresent();
        assertThat(found.get().isUploadComplete()).isFalse();
        assertThat(found.get().isPromotionSuccess()).isFalse();
        assertThat(found.get().isSystemUpdates()).isTrue();

        // Restore original values
        UpdateNotificationRequest restoreRequest = new UpdateNotificationRequest(
            originalUploadComplete, 
            originalPromotionSuccess, 
            originalSystemUpdates
        );
        userSettingsService.updateNotificationPreference(restoreRequest);
    }

    @Test
    @DisplayName("API Key 生命周期：创建 → 列出 → 撤销")
    void apiKeyLifecycle() {
        // 1. List existing API Keys - 列出已有 API Keys
        List<ApiKeyResponse> existingKeys = userSettingsService.listApiKeys();
        int originalCount = existingKeys.size();

        // 2. Create API Key - 创建 API Key
        CreateApiKeyRequest createRequest = new CreateApiKeyRequest("集成测试 API Key", 30);
        ApiKeyCreatedResponse created = userSettingsService.createApiKey(createRequest);
        
        assertThat(created).isNotNull();
        assertThat(created.apiKeyId()).isNotBlank();
        assertThat(created.name()).isEqualTo("集成测试 API Key");
        assertThat(created.key()).isNotBlank(); // 明文仅此一次返回
        assertThat(created.prefix()).startsWith("grc_");
        
        String apiKeyId = created.apiKeyId();

        // 3. List API Keys - 列出 API Keys（应包含新创建的）
        List<ApiKeyResponse> keysAfterCreate = userSettingsService.listApiKeys();
        assertThat(keysAfterCreate).hasSize(originalCount + 1);
        assertThat(keysAfterCreate).anyMatch(k -> k.apiKeyId().equals(apiKeyId));

        // 4. Verify persistence - 验证持久化
        Optional<ApiKey> found = apiKeyRepository.findById(new ApiKeyId(apiKeyId));
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("集成测试 API Key");

        // 5. Revoke API Key - 撤销 API Key
        userSettingsService.revokeApiKey(apiKeyId);

        // 6. Verify deletion - 验证已删除
        Optional<ApiKey> afterRevoke = apiKeyRepository.findById(new ApiKeyId(apiKeyId));
        assertThat(afterRevoke).isEmpty();

        List<ApiKeyResponse> keysAfterRevoke = userSettingsService.listApiKeys();
        assertThat(keysAfterRevoke).hasSize(originalCount);
    }

    @Test
    @DisplayName("用户资料持久化往返：保存后应能通过 ID 正确查询")
    void userProfileRoundTrip() {
        // Query the default user profile
        Optional<UserProfile> found = userProfileRepository.findById(DEFAULT_USER_ID);
        
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(DEFAULT_USER_ID);
        assertThat(found.get().getDisplayName()).isNotBlank();
    }

    @Test
    @DisplayName("通知偏好持久化往返：保存后应能通过 ID 正确查询")
    void notificationPreferenceRoundTrip() {
        // Query the default notification preference
        Optional<NotificationPreference> found = notificationPreferenceRepository.findById(DEFAULT_NOTIFICATION_ID);
        
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(DEFAULT_NOTIFICATION_ID);
    }

    @Test
    @DisplayName("头像上传：JPG 和 PNG 格式应被接受")
    void avatarUpload() throws Exception {
        // Create a mock JPG image
        byte[] jpgContent = new byte[1024]; // 1KB mock content
        MockMultipartFile jpgFile = new MockMultipartFile(
            "avatar", 
            "test_avatar.jpg", 
            "image/jpeg", 
            jpgContent
        );

        // Upload avatar
        String avatarUrl = userSettingsService.uploadAvatar(jpgFile);
        
        assertThat(avatarUrl).isNotBlank();
        assertThat(avatarUrl).contains("/api/avatars/");

        // Verify profile was updated
        ProfileResponse profile = userSettingsService.getProfile();
        assertThat(profile.avatarUrl()).isEqualTo(avatarUrl);

        // Create a mock PNG image
        MockMultipartFile pngFile = new MockMultipartFile(
            "avatar", 
            "test_avatar.png", 
            "image/png", 
            jpgContent
        );

        String pngAvatarUrl = userSettingsService.uploadAvatar(pngFile);
        assertThat(pngAvatarUrl).isNotBlank();
        assertThat(pngAvatarUrl).contains("/api/avatars/");
    }
}
