package com.grace.platform.distribution.domain;

/**
 * OAuth 服务领域接口
 * <p>
 * 定义平台 OAuth 2.0 授权流程的领域服务契约。
 * 负责生成授权 URL、处理回调、管理 Token 生命周期。
 * </p>
 */
public interface OAuthService {

    /**
     * 生成 OAuth 授权 URL
     *
     * @param platform   平台标识（如 "youtube"）
     * @param redirectUri 回调地址
     * @return 授权 URL 包装对象
     */
    AuthorizationUrl initiateAuth(String platform, String redirectUri);

    /**
     * 处理 OAuth 回调，交换 Token 并加密存储
     *
     * @param platform   平台标识
     * @param code       授权码
     * @param state      状态参数（用于 CSRF 防护）
     */
    void handleCallback(String platform, String code, String state);

    /**
     * 获取有效 Token（自动刷新过期 Token）
     *
     * @param platform 平台标识
     * @return 有效的 OAuthToken（若过期会自动刷新）
     */
    OAuthToken getValidToken(String platform);
}
