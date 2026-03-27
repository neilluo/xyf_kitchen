package com.grace.platform.distribution.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * YouTube 配置属性
 * <p>
 * 从 application.yml 加载 YouTube API 相关配置。
 * </p>
 *
 * @author Grace Platform Team
 */
@Component
@ConfigurationProperties(prefix = "grace.youtube")
public class YouTubeProperties {

    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private List<String> scopes;

    // OAuth 端点常量
    private static final String GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }

    public String getGoogleAuthUrl() {
        return GOOGLE_AUTH_URL;
    }

    public String getGoogleTokenUrl() {
        return GOOGLE_TOKEN_URL;
    }

    /**
     * 获取 scopes 字符串（空格分隔）
     */
    public String getScopesAsString() {
        if (scopes == null || scopes.isEmpty()) {
            return "https://www.googleapis.com/auth/youtube.upload https://www.googleapis.com/auth/youtube.readonly";
        }
        return String.join(" ", scopes);
    }
}
