package com.grace.platform.distribution.infrastructure.youtube;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * YouTube API 配置类
 */
@Configuration
public class YouTubeConfig {
    
    @Value("${youtube.client-id}")
    private String clientId;
    
    @Value("${youtube.client-secret}")
    private String clientSecret;
    
    @Value("${youtube.redirect-uri}")
    private String redirectUri;
    
    @Value("${youtube.scopes:https://www.googleapis.com/auth/youtube.upload,https://www.googleapis.com/auth/youtube.readonly}")
    private List<String> scopes;
    
    public String getClientId() {
        return clientId;
    }
    
    public String getClientSecret() {
        return clientSecret;
    }
    
    public String getRedirectUri() {
        return redirectUri;
    }
    
    public List<String> getScopes() {
        return scopes;
    }
}
