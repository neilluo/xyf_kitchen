package com.grace.platform.usersettings.domain.model;

import com.grace.platform.shared.domain.id.UserProfileId;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * UserProfile 聚合根
 * <p>
 * 管理用户个人资料信息。MVP 单用户模式，使用固定 ID。
 * </p>
 */
public class UserProfile {

    // 聚合根字段
    private UserProfileId id;
    private String displayName;
    private String email;          // nullable
    private String avatarUrl;      // nullable
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 私有构造器，通过工厂方法创建
     */
    private UserProfile() {
    }

    /**
     * 创建默认用户（MVP 单用户模式，应用启动时初始化）
     *
     * @param id          用户 ID（固定为 "default-user"）
     * @param displayName 显示名称
     * @return 新建的 UserProfile 实例
     */
    public static UserProfile createDefault(UserProfileId id, String displayName) {
        if (id == null) {
            throw new IllegalArgumentException("UserProfileId must not be null");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Display name must not be blank");
        }

        UserProfile profile = new UserProfile();
        profile.id = id;
        profile.displayName = displayName.trim();
        profile.email = null;
        profile.avatarUrl = null;
        profile.createdAt = LocalDateTime.now();
        profile.updatedAt = profile.createdAt;

        return profile;
    }

    /**
     * 更新资料（部分更新语义）
     *
     * @param displayName 新显示名称（可选，null 表示不修改）
     * @param email       新邮箱（可选，null 表示不修改）
     */
    public void updateProfile(String displayName, String email) {
        if (displayName != null && !displayName.isBlank()) {
            this.displayName = displayName.trim();
        }
        if (email != null) {
            this.email = email.trim();
        }
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 更新头像 URL
     *
     * @param avatarUrl 头像 URL
     * @throws NullPointerException 如果 avatarUrl 为 null
     */
    public void updateAvatar(String avatarUrl) {
        this.avatarUrl = Objects.requireNonNull(avatarUrl, "Avatar URL must not be null");
        this.updatedAt = LocalDateTime.now();
    }

    // Getters
    public UserProfileId getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // Setters for persistence layer (package-private)
    void setId(UserProfileId id) {
        this.id = id;
    }

    void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    void setEmail(String email) {
        this.email = email;
    }

    void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return String.format("UserProfile[id=%s, displayName=%s, email=%s, avatarUrl=%s]",
            id != null ? id.value() : "null", displayName, email, avatarUrl);
    }
}
