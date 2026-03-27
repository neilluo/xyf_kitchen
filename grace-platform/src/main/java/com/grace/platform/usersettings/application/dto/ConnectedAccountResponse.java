package com.grace.platform.usersettings.application.dto;

/**
 * 已连接账户响应 DTO
 */
public record ConnectedAccountResponse(
    String platform,
    String displayName,
    boolean authorized,
    String accountName,
    String connectedAt
) {}
