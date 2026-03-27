package com.grace.platform.usersettings.application.dto;

import com.grace.platform.usersettings.domain.model.ApiKey;

import java.time.LocalDateTime;

/**
 * API Key 响应 DTO（列表查询用，不含明文）
 */
public record ApiKeyResponse(
    String apiKeyId,
    String name,
    String prefix,
    LocalDateTime expiresAt,
    LocalDateTime lastUsedAt,
    LocalDateTime createdAt
) {
    public static ApiKeyResponse from(ApiKey apiKey) {
        return new ApiKeyResponse(
            apiKey.getId().value(),
            apiKey.getName(),
            apiKey.getPrefix(),
            apiKey.getExpiresAt(),
            apiKey.getLastUsedAt(),
            apiKey.getCreatedAt()
        );
    }
}
