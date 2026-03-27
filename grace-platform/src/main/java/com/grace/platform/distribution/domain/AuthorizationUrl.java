package com.grace.platform.distribution.domain;

/**
 * 授权 URL 值对象
 * <p>
 * 封装 OAuth 授权 URL 和关联的状态参数。
 * </p>
 *
 * @param authUrl 授权 URL
 * @param state   状态参数（用于 CSRF 防护）
 */
public record AuthorizationUrl(String authUrl, String state) {

    public AuthorizationUrl {
        if (authUrl == null || authUrl.isBlank()) {
            throw new IllegalArgumentException("Auth URL must not be blank");
        }
        if (state == null || state.isBlank()) {
            throw new IllegalArgumentException("State must not be blank");
        }
    }
}
