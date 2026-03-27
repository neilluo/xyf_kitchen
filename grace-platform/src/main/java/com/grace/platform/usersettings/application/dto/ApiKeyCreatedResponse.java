package com.grace.platform.usersettings.application.dto;

import com.grace.platform.usersettings.domain.service.GeneratedApiKey;

import java.time.LocalDateTime;

/**
 * API Key 创建响应 DTO（仅在创建时返回，包含明文密钥）
 */
public record ApiKeyCreatedResponse(
    String apiKeyId,
    String name,
    String key,
    String prefix,
    LocalDateTime expiresAt,
    LocalDateTime createdAt
) {
    public static ApiKeyCreatedResponse from(GeneratedApiKey generatedApiKey) {
        return new ApiKeyCreatedResponse(
            generatedApiKey.apiKey().getId().value(),
            generatedApiKey.apiKey().getName(),
            generatedApiKey.rawKey(),
            generatedApiKey.apiKey().getPrefix(),
            generatedApiKey.apiKey().getExpiresAt(),
            generatedApiKey.apiKey().getCreatedAt()
        );
    }
}
