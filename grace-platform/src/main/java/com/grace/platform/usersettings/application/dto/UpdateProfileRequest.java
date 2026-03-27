package com.grace.platform.usersettings.application.dto;

/**
 * 更新用户资料请求 DTO
 */
public record UpdateProfileRequest(
    String displayName,
    String email
) {}
