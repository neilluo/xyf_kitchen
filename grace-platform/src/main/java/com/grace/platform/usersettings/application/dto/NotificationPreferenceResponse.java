package com.grace.platform.usersettings.application.dto;

import com.grace.platform.usersettings.domain.model.NotificationPreference;

import java.time.LocalDateTime;

/**
 * 通知偏好响应 DTO
 */
public record NotificationPreferenceResponse(
    String preferenceId,
    boolean uploadComplete,
    boolean promotionSuccess,
    boolean systemUpdates,
    LocalDateTime updatedAt
) {
    public static NotificationPreferenceResponse from(NotificationPreference preference) {
        return new NotificationPreferenceResponse(
            preference.getId().value(),
            preference.isUploadComplete(),
            preference.isPromotionSuccess(),
            preference.isSystemUpdates(),
            preference.getUpdatedAt()
        );
    }
}
