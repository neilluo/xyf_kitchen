package com.grace.platform.usersettings.application.dto;

/**
 * 更新通知偏好请求 DTO
 */
public record UpdateNotificationRequest(
    Boolean uploadComplete,
    Boolean promotionSuccess,
    Boolean systemUpdates
) {}
