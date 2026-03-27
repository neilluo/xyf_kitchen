package com.grace.platform.distribution.domain;

import com.grace.platform.shared.domain.id.OAuthTokenId;

import java.time.LocalDateTime;

/**
 * OAuth Token 实体
 * <p>
 * 存储平台 OAuth 2.0 授权信息。
 * accessToken 和 refreshToken 为加密存储字段（AES-256-GCM）。
 * </p>
 */
public class OAuthToken {

    // 实体字段
    private OAuthTokenId id;
    private String platform;
    private String accessToken;
    private String refreshToken;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 私有构造器，通过工厂方法创建
     */
    private OAuthToken() {
    }

    /**
     * 创建新的 OAuthToken 实例
     *
     * @param platform     平台标识
     * @param accessToken  访问令牌（加密存储）
     * @param refreshToken 刷新令牌（加密存储）
     * @param expiresAt    Token 过期时间
     * @return 新建的 OAuthToken 实例
     */
    public static OAuthToken create(String platform, String accessToken, String refreshToken, LocalDateTime expiresAt) {
        if (platform == null || platform.isBlank()) {
            throw new IllegalArgumentException("Platform must not be blank");
        }
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("Access token must not be blank");
        }
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh token must not be blank");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("Expires at must not be null");
        }

        OAuthToken token = new OAuthToken();
        token.id = OAuthTokenId.generate();
        token.platform = platform;
        token.accessToken = accessToken;
        token.refreshToken = refreshToken;
        token.expiresAt = expiresAt;
        token.createdAt = LocalDateTime.now();
        token.updatedAt = token.createdAt;

        return token;
    }

    /**
     * 检查 Token 是否已过期
     *
     * @return true 如果 Token 已过期
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * 更新 Token 信息（用于刷新 Token）
     *
     * @param accessToken  新的访问令牌
     * @param refreshToken 新的刷新令牌
     * @param expiresAt    新的过期时间
     */
    public void updateTokens(String accessToken, String refreshToken, LocalDateTime expiresAt) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("Access token must not be blank");
        }
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh token must not be blank");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("Expires at must not be null");
        }

        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresAt = expiresAt;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 更新访问令牌（用于仅刷新 access token）
     *
     * @param accessToken 新的访问令牌
     * @param expiresAt   新的过期时间
     */
    public void updateAccessToken(String accessToken, LocalDateTime expiresAt) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("Access token must not be blank");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("Expires at must not be null");
        }

        this.accessToken = accessToken;
        this.expiresAt = expiresAt;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters
    public OAuthTokenId getId() {
        return id;
    }

    public String getPlatform() {
        return platform;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // Setters for persistence layer (package-private)
    void setId(OAuthTokenId id) {
        this.id = id;
    }

    void setPlatform(String platform) {
        this.platform = platform;
    }

    void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return String.format("OAuthToken[id=%s, platform=%s, expired=%s]",
            id != null ? id.value() : "null",
            platform,
            isExpired());
    }
}
