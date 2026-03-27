package com.grace.platform.usersettings.domain.model;

import com.grace.platform.shared.domain.id.NotificationPreferenceId;

import java.time.LocalDateTime;

/**
 * NotificationPreference 聚合根
 * <p>
 * 管理用户通知偏好设置。MVP 单用户模式，使用固定 ID。
 * </p>
 */
public class NotificationPreference {

    // 聚合根字段
    private NotificationPreferenceId id;
    private boolean uploadComplete;       // 上传完成时通知，默认 true
    private boolean promotionSuccess;     // 推广成功时通知，默认 true
    private boolean systemUpdates;        // 系统更新通知，默认 false
    private LocalDateTime updatedAt;

    /**
     * 私有构造器，通过工厂方法创建
     */
    private NotificationPreference() {
    }

    /**
     * 创建默认偏好（MVP 单用户模式，应用启动时初始化）
     * <p>
     * 默认设置：uploadComplete=true, promotionSuccess=true, systemUpdates=false
     * </p>
     *
     * @param id 通知偏好 ID（固定为 "default-notification"）
     * @return 新建的 NotificationPreference 实例
     */
    public static NotificationPreference createDefault(NotificationPreferenceId id) {
        if (id == null) {
            throw new IllegalArgumentException("NotificationPreferenceId must not be null");
        }

        NotificationPreference preference = new NotificationPreference();
        preference.id = id;
        preference.uploadComplete = true;
        preference.promotionSuccess = true;
        preference.systemUpdates = false;
        preference.updatedAt = LocalDateTime.now();

        return preference;
    }

    /**
     * 部分更新（传入 null 表示不修改该字段）
     *
     * @param uploadComplete   上传完成通知（可选）
     * @param promotionSuccess 推广成功通知（可选）
     * @param systemUpdates    系统更新通知（可选）
     */
    public void update(Boolean uploadComplete, Boolean promotionSuccess, Boolean systemUpdates) {
        if (uploadComplete != null) {
            this.uploadComplete = uploadComplete;
        }
        if (promotionSuccess != null) {
            this.promotionSuccess = promotionSuccess;
        }
        if (systemUpdates != null) {
            this.systemUpdates = systemUpdates;
        }
        this.updatedAt = LocalDateTime.now();
    }

    // Getters
    public NotificationPreferenceId getId() {
        return id;
    }

    public boolean isUploadComplete() {
        return uploadComplete;
    }

    public boolean isPromotionSuccess() {
        return promotionSuccess;
    }

    public boolean isSystemUpdates() {
        return systemUpdates;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // Setters for persistence layer (package-private)
    void setId(NotificationPreferenceId id) {
        this.id = id;
    }

    void setUploadComplete(boolean uploadComplete) {
        this.uploadComplete = uploadComplete;
    }

    void setPromotionSuccess(boolean promotionSuccess) {
        this.promotionSuccess = promotionSuccess;
    }

    void setSystemUpdates(boolean systemUpdates) {
        this.systemUpdates = systemUpdates;
    }

    void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return String.format("NotificationPreference[id=%s, uploadComplete=%s, promotionSuccess=%s, systemUpdates=%s]",
            id != null ? id.value() : "null", uploadComplete, promotionSuccess, systemUpdates);
    }
}
