package com.grace.platform.usersettings.application.dto;

import com.grace.platform.usersettings.domain.model.UserProfile;

import java.time.LocalDateTime;

/**
 * 用户资料响应 DTO
 */
public record ProfileResponse(
    String userId,
    String displayName,
    String email,
    String avatarUrl,
    LocalDateTime createdAt
) {
    public static ProfileResponse from(UserProfile profile) {
        return new ProfileResponse(
            profile.getId().value(),
            profile.getDisplayName(),
            profile.getEmail(),
            profile.getAvatarUrl(),
            profile.getCreatedAt()
        );
    }
}
