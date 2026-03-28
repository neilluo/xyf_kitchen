package com.grace.platform.distribution.infrastructure.youtube;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.AuthorizationCodeTokenRequest;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.MemoryDataStoreFactory;
import com.grace.platform.distribution.domain.OAuthToken;
import com.grace.platform.distribution.domain.OAuthTokenRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;

/**
 * YouTube OAuth 2.0 认证服务
 */
@Service
public class YouTubeOAuthService {
    
    private static final Logger logger = LoggerFactory.getLogger(YouTubeOAuthService.class);
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String APPLICATION_NAME = "Grace Platform";
    private static final String PLATFORM_YOUTUBE = "youtube";
    
    private final YouTubeConfig config;
    private final OAuthTokenRepository tokenRepository;
    private AuthorizationCodeFlow flow;
    
    public YouTubeOAuthService(YouTubeConfig config, OAuthTokenRepository tokenRepository) {
        this.config = config;
        this.tokenRepository = tokenRepository;
    }
    
    @PostConstruct
    public void init() {
        try {
            GoogleClientSecrets clientSecrets = new GoogleClientSecrets()
                .setWeb(new GoogleClientSecrets.Details()
                    .setClientId(config.getClientId())
                    .setClientSecret(config.getClientSecret()));
            
            flow = new GoogleAuthorizationCodeFlow.Builder(
                    new NetHttpTransport(),
                    JSON_FACTORY,
                    clientSecrets,
                    Collections.singletonList("https://www.googleapis.com/auth/youtube.upload"))
                .setDataStoreFactory(new MemoryDataStoreFactory())
                .setAccessType("offline")
                .build();
            
            logger.info("YouTube OAuth service initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize YouTube OAuth service", e);
        }
    }
    
    /**
     * 获取授权 URL
     */
    public String getAuthorizationUrl() {
        if (flow == null) {
            throw new IllegalStateException("YouTube OAuth not initialized");
        }
        return flow.newAuthorizationUrl()
            .setRedirectUri(config.getRedirectUri())
            .build();
    }
    
    /**
     * 处理 OAuth 回调
     */
    public void handleCallback(String code) throws IOException {
        if (flow == null) {
            throw new IllegalStateException("YouTube OAuth not initialized");
        }
        
        AuthorizationCodeTokenRequest tokenRequest = flow
            .newTokenRequest(code)
            .setRedirectUri(config.getRedirectUri());
        
        TokenResponse tokenResponse = tokenRequest.execute();
        Credential credential = flow.createAndStoreCredential(tokenResponse, "user");
        
        // 保存到数据库
        saveToken(credential);
        
        logger.info("YouTube OAuth callback handled successfully");
    }
    
    /**
     * 获取已保存的 Token
     */
    public OAuthToken getStoredToken() {
        return tokenRepository.findByPlatform(PLATFORM_YOUTUBE).orElse(null);
    }
    
    /**
     * 检查是否已授权
     */
    public boolean isAuthorized() {
        OAuthToken token = getStoredToken();
        return token != null && token.getExpiresAt().isAfter(LocalDateTime.now());
    }
    
    private void saveToken(Credential credential) {
        OAuthToken token = OAuthToken.create(
            PLATFORM_YOUTUBE,
            encrypt(credential.getAccessToken()),
            encrypt(credential.getRefreshToken()),
            LocalDateTime.now().plusSeconds(credential.getExpiresInSeconds())
        );
        
        tokenRepository.save(token);
    }
    
    private String encrypt(String value) {
        // TODO: 使用 GRACE_ENCRYPTION_KEY 加密
        // 暂时直接存储，后续实现加密
        return value;
    }
    
    public AuthorizationCodeFlow getFlow() {
        return flow;
    }
}
