package com.grace.platform.usersettings.application.dto;

/**
 * 创建 API Key 请求 DTO
 */
public record CreateApiKeyRequest(
    String name,
    Integer expiresInDays
) {}
