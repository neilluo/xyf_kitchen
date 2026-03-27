package com.grace.platform.distribution.infrastructure.youtube;

import com.grace.platform.distribution.domain.*;
import com.grace.platform.distribution.infrastructure.config.YouTubeProperties;
import com.grace.platform.shared.ErrorCode;
import com.grace.platform.shared.infrastructure.encryption.EncryptionService;
import com.grace.platform.shared.infrastructure.exception.BusinessRuleViolationException;
import com.grace.platform.shared.infrastructure.exception.ExternalServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

/**
 * YouTube OAuth 服务实现
 * <p>
 * 实现 {@link OAuthService} 接口，处理 YouTube（Google OAuth 2.0）授权流程。
 * Token 使用 AES-256-GCM 加密存储。
 * </p>
 * <p>
 * <strong>OAuth 流程：</strong>
 * <ol>
 *   <li>initiateAuth: 构建 Google OAuth URL，生成 state 参数</li>
 *   <li>handleCallback: 用 code 换取 access_token + refresh_token，加密存储</li>
 *   <li>getValidToken: 检查过期时间，自动刷新过期 Token</li>
 * </ol>
 * </p>
 *
 * @author Grace Platform Team
 * @see OAuthService
 */
@Component
public class YouTubeOAuthServiceImpl implements OAuthService {

    private static final Logger logger = LoggerFactory.getLogger(YouTubeOAuthServiceImpl.class);

    private final OAuthTokenRepository tokenRepository;
    private final EncryptionService encryptionService;
    private final YouTubeProperties youTubeProperties;
    private final RestTemplate restTemplate;
    private final SecureRandom secureRandom;

    public YouTubeOAuthServiceImpl(OAuthTokenRepository tokenRepository,
                                   EncryptionService encryptionService,
                                   YouTubeProperties youTubeProperties) {
        this.tokenRepository = tokenRepository;
        this.encryptionService = encryptionService;
        this.youTubeProperties = youTubeProperties;
        this.restTemplate = new RestTemplate();
        this.secureRandom = new SecureRandom();
    }

    @Override
    public AuthorizationUrl initiateAuth(String platform, String redirectUri) {
        if (!"youtube".equals(platform)) {
            throw new IllegalArgumentException("This service only supports 'youtube' platform, got: " + platform);
        }

        logger.info("Initiating OAuth flow for platform: {}", platform);

        try {
            // 生成随机 state 参数（CSRF 防护）
            String state = generateState();
            logger.debug("Generated OAuth state parameter: {}", state);

            // 构建 Google OAuth URL
            String authUrl = buildAuthUrl(redirectUri, state);

            logger.info("OAuth authorization URL generated for {}", platform);

            return new AuthorizationUrl(authUrl, state);

        } catch (Exception e) {
            logger.error("Failed to initiate OAuth flow: {}", e.getMessage(), e);
            throw new ExternalServiceException(
                ErrorCode.PLATFORM_API_ERROR,
                "YouTube OAuth",
                "Failed to initiate auth: " + e.getMessage()
            );
        }
    }

    @Override
    public void handleCallback(String platform, String code, String state) {
        if (!"youtube".equals(platform)) {
            throw new IllegalArgumentException("This service only supports 'youtube' platform, got: " + platform);
        }

        logger.info("Handling OAuth callback for platform: {}, state: {}", platform, state);

        try {
            // Step 1: 用 code 换取 token
            TokenResponse tokenResponse = exchangeCodeForTokens(code);

            logger.debug("Token exchange successful: access_token_expires_in={}",
                tokenResponse.expiresIn);

            // Step 2: 加密 Token
            String encryptedAccessToken = encryptionService.encrypt(tokenResponse.accessToken);
            String encryptedRefreshToken = encryptionService.encrypt(tokenResponse.refreshToken);

            // Step 3: 计算过期时间
            LocalDateTime expiresAt = LocalDateTime.now()
                .plus(tokenResponse.expiresIn, ChronoUnit.SECONDS);

            // Step 4: 保存到数据库
            OAuthToken token = OAuthToken.create(
                platform,
                encryptedAccessToken,
                encryptedRefreshToken,
                expiresAt
            );

            // 检查是否已存在，如果存在则更新
            Optional<OAuthToken> existingToken = tokenRepository.findByPlatform(platform);
            if (existingToken.isPresent()) {
                OAuthToken existing = existingToken.get();
                existing.updateTokens(encryptedAccessToken, encryptedRefreshToken, expiresAt);
                tokenRepository.save(existing);
                logger.info("Updated existing OAuth token for platform: {}", platform);
            } else {
                tokenRepository.save(token);
                logger.info("Saved new OAuth token for platform: {}", platform);
            }

            logger.info("OAuth callback handled successfully for platform: {}", platform);

        } catch (Exception e) {
            logger.error("Failed to handle OAuth callback: {}", e.getMessage(), e);
            throw new ExternalServiceException(
                ErrorCode.PLATFORM_API_ERROR,
                "YouTube OAuth",
                "Failed to handle callback: " + e.getMessage()
            );
        }
    }

    @Override
    public OAuthToken getValidToken(String platform) {
        if (!"youtube".equals(platform)) {
            throw new IllegalArgumentException("This service only supports 'youtube' platform, got: " + platform);
        }

        logger.debug("Getting valid OAuth token for platform: {}", platform);

        // 1. 查询数据库
        Optional<OAuthToken> tokenOpt = tokenRepository.findByPlatform(platform);

        if (tokenOpt.isEmpty()) {
            logger.warn("No OAuth token found for platform: {}", platform);
            throw new BusinessRuleViolationException(
                ErrorCode.PLATFORM_NOT_AUTHORIZED,
                "Platform not authorized: " + platform + ". Please connect your account first."
            );
        }

        OAuthToken token = tokenOpt.get();

        // 2. 解密 access token（用于检查是否过期）
        String decryptedAccessToken;
        try {
            decryptedAccessToken = encryptionService.decrypt(token.getAccessToken());
        } catch (Exception e) {
            logger.error("Failed to decrypt access token: {}", e.getMessage(), e);
            throw new ExternalServiceException(
                ErrorCode.ENCRYPTION_ERROR,
                "Encryption",
                "Failed to decrypt token"
            );
        }

        // 3. 检查是否过期
        if (!token.isExpired()) {
            logger.debug("OAuth token is still valid for platform: {}", platform);
            return token;
        }

        // 4. Token 已过期，需要刷新
        logger.info("OAuth token expired for platform: {}, refreshing...", platform);

        try {
            // 解密 refresh token
            String decryptedRefreshToken = encryptionService.decrypt(token.getRefreshToken());

            // 调用刷新接口
            TokenResponse refreshedTokens = refreshAccessToken(decryptedRefreshToken);

            // 加密新 token
            String newEncryptedAccessToken = encryptionService.encrypt(refreshedTokens.accessToken);
            String newEncryptedRefreshToken = encryptionService.encrypt(refreshedTokens.refreshToken);

            // 计算新的过期时间
            LocalDateTime newExpiresAt = LocalDateTime.now()
                .plus(refreshedTokens.expiresIn, ChronoUnit.SECONDS);

            // 更新 token
            token.updateTokens(newEncryptedAccessToken, newEncryptedRefreshToken, newExpiresAt);
            tokenRepository.save(token);

            logger.info("OAuth token refreshed successfully for platform: {}", platform);

            return token;

        } catch (Exception e) {
            logger.error("Failed to refresh OAuth token: {}", e.getMessage(), e);
            throw new ExternalServiceException(
                ErrorCode.PLATFORM_AUTH_EXPIRED,
                "YouTube OAuth",
                "Token expired and refresh failed: " + e.getMessage()
            );
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 生成随机 state 参数
     */
    private String generateState() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * 构建 Google OAuth 授权 URL
     */
    private String buildAuthUrl(String redirectUri, String state) {
        StringBuilder url = new StringBuilder(youTubeProperties.getGoogleAuthUrl());
        url.append("?");
        url.append("client_id=").append(urlEncode(youTubeProperties.getClientId()));
        url.append("&redirect_uri=").append(urlEncode(redirectUri));
        url.append("&response_type=code");
        url.append("&scope=").append(urlEncode(youTubeProperties.getScopesAsString()));
        url.append("&state=").append(urlEncode(state));
        url.append("&access_type=offline");  // 请求 refresh_token
        url.append("&prompt=consent");        // 强制显示授权页面
        return url.toString();
    }

    /**
     * URL 编码辅助方法
     */
    private String urlEncode(String value) {
        if (value == null) return "";
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * 用 code 换取 access_token 和 refresh_token
     * <p>
     * <strong>注意：</strong>此为骨架实现。实际应调用 Google OAuth token endpoint。
     * </p>
     */
    private TokenResponse exchangeCodeForTokens(String code) {
        logger.debug("Exchanging authorization code for tokens");

        // TODO: 实现实际的 HTTP 调用
        // POST https://oauth2.googleapis.com/token
        // Headers:
        //   Content-Type: application/x-www-form-urlencoded
        // Body:
        //   code={code}
        //   client_id={clientId}
        //   client_secret={clientSecret}
        //   redirect_uri={redirectUri}
        //   grant_type=authorization_code

        // 模拟返回（实际实现中应调用真实 API）
        // 注意：需要 client_id 和 client_secret 配置正确
        if (youTubeProperties.getClientId() == null || youTubeProperties.getClientId().isBlank()) {
            throw new IllegalStateException("YouTube client ID not configured");
        }

        logger.debug("Token exchange request prepared (skeleton implementation)");

        // 模拟 token 响应
        return new TokenResponse(
            "mock_access_token_" + System.currentTimeMillis(),
            "mock_refresh_token_" + System.currentTimeMillis(),
            3600  // 1 hour
        );
    }

    /**
     * 刷新 access token
     * <p>
     * <strong>注意：</strong>此为骨架实现。实际应调用 Google OAuth token endpoint。
     * </p>
     */
    private TokenResponse refreshAccessToken(String refreshToken) {
        logger.debug("Refreshing access token");

        // TODO: 实现实际的 HTTP 调用
        // POST https://oauth2.googleapis.com/token
        // Headers:
        //   Content-Type: application/x-www-form-urlencoded
        // Body:
        //   refresh_token={refreshToken}
        //   client_id={clientId}
        //   client_secret={clientSecret}
        //   grant_type=refresh_token

        logger.debug("Token refresh request prepared (skeleton implementation)");

        // 模拟刷新响应
        return new TokenResponse(
            "mock_refreshed_access_token_" + System.currentTimeMillis(),
            refreshToken,  // refresh_token 通常不变
            3600  // 1 hour
        );
    }

    /**
     * Token 响应内部类
     */
    private record TokenResponse(String accessToken, String refreshToken, int expiresIn) {}
}
