package com.grace.platform.usersettings.domain.model;

import com.grace.platform.shared.domain.id.ApiKeyId;

import java.time.LocalDateTime;

/**
 * ApiKey 聚合根
 * <p>
 * 管理 API Key 生命周期。hashedKey 为 BCrypt 哈希不可逆存储。
 * </p>
 */
public class ApiKey {

    // 聚合根字段
    private ApiKeyId id;
    private String name;                  // 用途描述
    private String hashedKey;             // BCrypt 哈希存储（不可逆）
    private String prefix;                // 密钥前缀（如 "grc_a1b2...o5p6"），用于识别
    private LocalDateTime expiresAt;
    private LocalDateTime lastUsedAt;     // nullable
    private LocalDateTime createdAt;

    /**
     * 私有构造器，通过工厂方法创建
     */
    private ApiKey() {
    }

    /**
     * 创建 ApiKey 实例
     * <p>
     * 由 ApiKeyGenerationService 调用。rawKey 为明文密钥，hashedKey 为 BCrypt 哈希后的值。
     * </p>
     *
     * @param id          API Key ID
     * @param name        用途描述
     * @param hashedKey   BCrypt 哈希后的密钥
     * @param prefix      密钥前缀（用于列表识别）
     * @param expiresAt   过期时间
     * @return 新建的 ApiKey 实例
     */
    public static ApiKey create(ApiKeyId id, String name, String hashedKey,
                                 String prefix, LocalDateTime expiresAt) {
        if (id == null) {
            throw new IllegalArgumentException("ApiKeyId must not be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name must not be blank");
        }
        if (hashedKey == null || hashedKey.isBlank()) {
            throw new IllegalArgumentException("Hashed key must not be blank");
        }
        if (prefix == null || prefix.isBlank()) {
            throw new IllegalArgumentException("Prefix must not be blank");
        }

        ApiKey apiKey = new ApiKey();
        apiKey.id = id;
        apiKey.name = name.trim();
        apiKey.hashedKey = hashedKey;
        apiKey.prefix = prefix;
        apiKey.expiresAt = expiresAt;
        apiKey.lastUsedAt = null;
        apiKey.createdAt = LocalDateTime.now();

        return apiKey;
    }

    /**
     * 记录最后使用时间
     */
    public void recordUsage() {
        this.lastUsedAt = LocalDateTime.now();
    }

    /**
     * 检查是否已过期
     *
     * @return true 如果已设置过期时间且当前时间已超过过期时间
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    // Getters
    public ApiKeyId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getHashedKey() {
        return hashedKey;
    }

    public String getPrefix() {
        return prefix;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // Setters for persistence layer (package-private)
    void setId(ApiKeyId id) {
        this.id = id;
    }

    void setName(String name) {
        this.name = name;
    }

    void setHashedKey(String hashedKey) {
        this.hashedKey = hashedKey;
    }

    void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    void setLastUsedAt(LocalDateTime lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return String.format("ApiKey[id=%s, name=%s, prefix=%s, expiresAt=%s, lastUsedAt=%s]",
            id != null ? id.value() : "null", name, prefix, expiresAt, lastUsedAt);
    }
}
