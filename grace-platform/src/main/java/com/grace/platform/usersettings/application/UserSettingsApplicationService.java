package com.grace.platform.usersettings.application;

import com.grace.platform.shared.ErrorCode;
import com.grace.platform.shared.domain.id.ApiKeyId;
import com.grace.platform.shared.domain.id.NotificationPreferenceId;
import com.grace.platform.shared.domain.id.UserProfileId;
import com.grace.platform.shared.infrastructure.exception.EntityNotFoundException;
import com.grace.platform.usersettings.application.dto.*;
import com.grace.platform.usersettings.domain.model.NotificationPreference;
import com.grace.platform.usersettings.domain.model.UserProfile;
import com.grace.platform.usersettings.domain.repository.ApiKeyRepository;
import com.grace.platform.usersettings.domain.repository.NotificationPreferenceRepository;
import com.grace.platform.usersettings.domain.repository.UserProfileRepository;
import com.grace.platform.usersettings.domain.service.ApiKeyGenerationService;
import com.grace.platform.usersettings.domain.service.GeneratedApiKey;
import com.grace.platform.usersettings.infrastructure.acl.ConnectedAccountQueryService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 用户设置应用服务
 * <p>
 * 处理用户资料、通知偏好、API Key 和已连接账户的业务逻辑。
 * MVP 单用户模式，使用固定 ID。
 * </p>
 */
@Service
@Transactional
public class UserSettingsApplicationService {

    // MVP 单用户模式固定 ID
    private static final UserProfileId DEFAULT_USER_ID = new UserProfileId("default-user");
    private static final NotificationPreferenceId DEFAULT_NOTIFICATION_ID = new NotificationPreferenceId("default-notification");

    // 允许的头像文件类型
    private static final Set<String> ALLOWED_AVATAR_TYPES = Set.of("image/jpeg", "image/png");
    // 默认头像大小限制 2MB
    private static final long DEFAULT_AVATAR_MAX_SIZE = 2 * 1024 * 1024; // 2MB

    private final UserProfileRepository userProfileRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyGenerationService apiKeyGenerationService;
    private final ConnectedAccountQueryService connectedAccountQueryService;

    @Value("${grace.storage.avatar-dir:./storage/avatars}")
    private String avatarDir;

    @Value("${grace.storage.avatar-max-size:2097152}")
    private long avatarMaxSize;

    public UserSettingsApplicationService(
            UserProfileRepository userProfileRepository,
            NotificationPreferenceRepository notificationPreferenceRepository,
            ApiKeyRepository apiKeyRepository,
            ApiKeyGenerationService apiKeyGenerationService,
            ConnectedAccountQueryService connectedAccountQueryService) {
        this.userProfileRepository = userProfileRepository;
        this.notificationPreferenceRepository = notificationPreferenceRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.apiKeyGenerationService = apiKeyGenerationService;
        this.connectedAccountQueryService = connectedAccountQueryService;
    }

    // ==================== G1: 获取用户资料 ====================

    @Transactional(readOnly = true)
    public ProfileResponse getProfile() {
        UserProfile profile = userProfileRepository.findById(DEFAULT_USER_ID)
            .orElseThrow(() -> new EntityNotFoundException(
                ErrorCode.PROFILE_NOT_FOUND, "UserProfile", DEFAULT_USER_ID.value()));
        return ProfileResponse.from(profile);
    }

    // ==================== G2: 更新用户资料 ====================

    public ProfileResponse updateProfile(UpdateProfileRequest request) {
        UserProfile profile = userProfileRepository.findById(DEFAULT_USER_ID)
            .orElseThrow(() -> new EntityNotFoundException(
                ErrorCode.PROFILE_NOT_FOUND, "UserProfile", DEFAULT_USER_ID.value()));

        profile.updateProfile(request.displayName(), request.email());
        userProfileRepository.save(profile);

        return ProfileResponse.from(profile);
    }

    // ==================== G3: 上传头像 ====================

    public String uploadAvatar(MultipartFile file) {
        // 1. 校验文件类型（JPG/PNG）
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_AVATAR_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Avatar must be JPG or PNG image");
        }

        // 2. 校验文件大小（≤2MB）
        long maxSize = avatarMaxSize > 0 ? avatarMaxSize : DEFAULT_AVATAR_MAX_SIZE;
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("Avatar file size must be ≤ 2MB");
        }

        // 3. 保存文件到 ${grace.storage.avatar-dir}/<userId>_<timestamp>.<ext>
        String extension = contentType.equals("image/jpeg") ? "jpg" : "png";
        String fileName = String.format("%s_%d.%s", DEFAULT_USER_ID.value(), System.currentTimeMillis(), extension);

        Path avatarPath = Paths.get(avatarDir, fileName);
        try {
            Files.createDirectories(avatarPath.getParent());
            Files.write(avatarPath, file.getBytes());
        } catch (IOException e) {
            throw new RuntimeException("Failed to save avatar file", e);
        }

        // 4. 更新 UserProfile.avatarUrl
        String avatarUrl = "/api/avatars/" + fileName;
        UserProfile profile = userProfileRepository.findById(DEFAULT_USER_ID)
            .orElseThrow(() -> new EntityNotFoundException(
                ErrorCode.PROFILE_NOT_FOUND, "UserProfile", DEFAULT_USER_ID.value()));
        profile.updateAvatar(avatarUrl);
        userProfileRepository.save(profile);

        // 5. 返回新的 avatarUrl
        return avatarUrl;
    }

    // ==================== G4: 获取已连接账户列表 ====================

    @Transactional(readOnly = true)
    public List<ConnectedAccountResponse> getConnectedAccounts() {
        return connectedAccountQueryService.queryConnectedAccounts();
    }

    // ==================== G5: 断开平台连接 ====================

    public void disconnectPlatform(String platform) {
        Objects.requireNonNull(platform, "Platform must not be null");
        connectedAccountQueryService.disconnectPlatform(platform);
    }

    // ==================== G6: 获取通知偏好 ====================

    @Transactional(readOnly = true)
    public NotificationPreferenceResponse getNotificationPreference() {
        NotificationPreference preference = notificationPreferenceRepository
            .findById(DEFAULT_NOTIFICATION_ID)
            .orElseThrow(() -> new EntityNotFoundException(
                ErrorCode.PROFILE_NOT_FOUND, "NotificationPreference", DEFAULT_NOTIFICATION_ID.value()));
        return NotificationPreferenceResponse.from(preference);
    }

    // ==================== G7: 更新通知偏好 ====================

    public NotificationPreferenceResponse updateNotificationPreference(UpdateNotificationRequest request) {
        NotificationPreference preference = notificationPreferenceRepository
            .findById(DEFAULT_NOTIFICATION_ID)
            .orElseThrow(() -> new EntityNotFoundException(
                ErrorCode.PROFILE_NOT_FOUND, "NotificationPreference", DEFAULT_NOTIFICATION_ID.value()));

        preference.update(request.uploadComplete(), request.promotionSuccess(), request.systemUpdates());
        notificationPreferenceRepository.save(preference);

        return NotificationPreferenceResponse.from(preference);
    }

    // ==================== G8: 创建 API Key ====================

    public ApiKeyCreatedResponse createApiKey(CreateApiKeyRequest request) {
        int expiresInDays = request.expiresInDays() != null ? request.expiresInDays() : 90;
        GeneratedApiKey generated = apiKeyGenerationService.generate(request.name(), expiresInDays);
        apiKeyRepository.save(generated.apiKey());

        // 返回包含 rawKey 的响应（明文仅此一次）
        return ApiKeyCreatedResponse.from(generated);
    }

    // ==================== G9: 列出所有 API Keys ====================

    @Transactional(readOnly = true)
    public List<ApiKeyResponse> listApiKeys() {
        return apiKeyRepository.findAll().stream()
            .map(ApiKeyResponse::from)
            .toList();
    }

    // ==================== G10: 撤销 API Key ====================

    public void revokeApiKey(String apiKeyId) {
        ApiKeyId id = new ApiKeyId(apiKeyId);
        apiKeyRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException(
                ErrorCode.API_KEY_NOT_FOUND, "ApiKey", apiKeyId));
        apiKeyRepository.deleteById(id);
    }
}
