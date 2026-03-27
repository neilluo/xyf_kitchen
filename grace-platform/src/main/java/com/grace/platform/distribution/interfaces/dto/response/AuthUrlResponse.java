package com.grace.platform.distribution.interfaces.dto.response;

/**
 * OAuth 授权 URL 响应 DTO
 * <p>
 * 对应 API D3 响应的 data 字段。
 * 包含完整的 OAuth 授权 URL 和 CSRF 防护用的 state 参数。
 * </p>
 *
 * @param authUrl 完整的 OAuth 授权 URL
 * @param state   CSRF 防护用的 state 参数
 */
public record AuthUrlResponse(
    String authUrl,
    String state
) {
}
