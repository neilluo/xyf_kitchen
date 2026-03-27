package com.grace.platform.usersettings.interfaces.rest;

import com.grace.platform.shared.application.dto.ApiResponse;
import com.grace.platform.usersettings.application.UserSettingsApplicationService;
import com.grace.platform.usersettings.application.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 用户设置控制器
 * <p>
 * 处理用户设置相关的 REST API 端点，包括：
 * <ul>
 *   <li>用户资料管理</li>
 *   <li>头像上传</li>
 *   <li>已连接账户管理</li>
 *   <li>通知偏好设置</li>
 *   <li>API Key 管理</li>
 * </ul>
 * <p>
 * 端点列表：
 * <ul>
 *   <li>G1: GET /api/settings/profile - 获取用户资料</li>
 *   <li>G2: PUT /api/settings/profile - 更新用户资料</li>
 *   <li>G3: POST /api/settings/profile/avatar - 上传头像</li>
 *   <li>G4: GET /api/settings/connected-accounts - 已连接账户列表</li>
 *   <li>G5: DELETE /api/settings/connected-accounts/{platform} - 断开平台连接</li>
 *   <li>G6: GET /api/settings/notifications - 获取通知偏好</li>
 *   <li>G7: PUT /api/settings/notifications - 更新通知偏好</li>
 *   <li>G8: POST /api/settings/api-keys - 创建 API Key</li>
 *   <li>G9: GET /api/settings/api-keys - 列出 API Keys</li>
 *   <li>G10: DELETE /api/settings/api-keys/{apiKeyId} - 撤销 API Key</li>
 * </ul>
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final UserSettingsApplicationService applicationService;

    /**
     * 构造器注入依赖。
     *
     * @param applicationService 用户设置应用服务
     */
    public SettingsController(UserSettingsApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    // ==================== G1: 获取用户资料 ====================

    /**
     * 获取当前用户资料。
     *
     * @return 用户资料
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile() {
        ProfileResponse profile = applicationService.getProfile();
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    // ==================== G2: 更新用户资料 ====================

    /**
     * 更新用户资料。支持部分更新。
     *
     * @param request 更新资料请求
     * @return 更新后的用户资料
     */
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        ProfileResponse profile = applicationService.updateProfile(request);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    // ==================== G3: 上传头像 ====================

    /**
     * 上传用户头像。
     * <p>
     * 支持 JPG/PNG 格式，最大 2MB。
     *
     * @param avatar 头像文件
     * @return 新的头像 URL
     */
    @PostMapping("/profile/avatar")
    public ResponseEntity<ApiResponse<AvatarResponse>> uploadAvatar(
            @RequestParam("avatar") MultipartFile avatar) {
        String avatarUrl = applicationService.uploadAvatar(avatar);
        return ResponseEntity.ok(ApiResponse.success(new AvatarResponse(avatarUrl)));
    }

    // ==================== G4: 获取已连接账户列表 ====================

    /**
     * 获取已连接的第三方平台账户列表。
     *
     * @return 已连接账户列表
     */
    @GetMapping("/connected-accounts")
    public ResponseEntity<ApiResponse<List<ConnectedAccountResponse>>> getConnectedAccounts() {
        List<ConnectedAccountResponse> accounts = applicationService.getConnectedAccounts();
        return ResponseEntity.ok(ApiResponse.success(accounts));
    }

    // ==================== G5: 断开平台连接 ====================

    /**
     * 断开与指定平台的 OAuth 连接。
     *
     * @param platform 平台标识
     * @return 空响应
     */
    @DeleteMapping("/connected-accounts/{platform}")
    public ResponseEntity<ApiResponse<Void>> disconnectPlatform(
            @PathVariable String platform) {
        applicationService.disconnectPlatform(platform);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ==================== G6: 获取通知偏好 ====================

    /**
     * 获取通知偏好设置。
     *
     * @return 通知偏好
     */
    @GetMapping("/notifications")
    public ResponseEntity<ApiResponse<NotificationPreferenceResponse>> getNotificationPreference() {
        NotificationPreferenceResponse preference = applicationService.getNotificationPreference();
        return ResponseEntity.ok(ApiResponse.success(preference));
    }

    // ==================== G7: 更新通知偏好 ====================

    /**
     * 更新通知偏好。支持部分更新。
     *
     * @param request 更新通知偏好请求
     * @return 更新后的通知偏好
     */
    @PutMapping("/notifications")
    public ResponseEntity<ApiResponse<NotificationPreferenceResponse>> updateNotificationPreference(
            @Valid @RequestBody UpdateNotificationRequest request) {
        NotificationPreferenceResponse preference = applicationService.updateNotificationPreference(request);
        return ResponseEntity.ok(ApiResponse.success(preference));
    }

    // ==================== G8: 创建 API Key ====================

    /**
     * 生成平台 API Key。
     * <p>
     * 密钥仅在创建时返回一次明文。
     *
     * @param request 创建 API Key 请求
     * @return 创建的 API Key（含明文）
     */
    @PostMapping("/api-keys")
    public ResponseEntity<ApiResponse<ApiKeyCreatedResponse>> createApiKey(
            @Valid @RequestBody CreateApiKeyRequest request) {
        ApiKeyCreatedResponse apiKey = applicationService.createApiKey(request);
        return ResponseEntity.ok(ApiResponse.success(apiKey));
    }

    // ==================== G9: 列出所有 API Keys ====================

    /**
     * 列出所有已创建的 API Key（不含明文）。
     *
     * @return API Key 列表
     */
    @GetMapping("/api-keys")
    public ResponseEntity<ApiResponse<List<ApiKeyResponse>>> listApiKeys() {
        List<ApiKeyResponse> apiKeys = applicationService.listApiKeys();
        return ResponseEntity.ok(ApiResponse.success(apiKeys));
    }

    // ==================== G10: 撤销 API Key ====================

    /**
     * 撤销（删除）指定 API Key。
     *
     * @param apiKeyId API Key ID
     * @return 空响应
     */
    @DeleteMapping("/api-keys/{apiKeyId}")
    public ResponseEntity<ApiResponse<Void>> revokeApiKey(
            @PathVariable String apiKeyId) {
        applicationService.revokeApiKey(apiKeyId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 头像上传响应
     */
    public record AvatarResponse(String avatarUrl) {}
}
