package com.grace.platform.distribution.infrastructure.persistence;

import com.grace.platform.distribution.domain.OAuthToken;
import com.grace.platform.distribution.domain.OAuthTokenRepository;
import com.grace.platform.shared.domain.id.OAuthTokenId;
import com.grace.platform.shared.infrastructure.encryption.EncryptionService;
import org.springframework.stereotype.Repository;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * OAuthToken 仓储实现类。
 * <p>
 * 基于 MyBatis 实现 OAuthToken 实体的持久化操作。
 * 注意：读取时调用 EncryptionService.decrypt() 解密，存储时调用 encrypt() 加密。
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
@Repository
public class OAuthTokenRepositoryImpl implements OAuthTokenRepository {

    private final OAuthTokenMapper oAuthTokenMapper;
    private final EncryptionService encryptionService;

    // 反射缓存
    private final Constructor<OAuthToken> tokenConstructor;
    private final Method setIdMethod;
    private final Method setPlatformMethod;
    private final Method setAccessTokenMethod;
    private final Method setRefreshTokenMethod;
    private final Method setExpiresAtMethod;
    private final Method setCreatedAtMethod;
    private final Method setUpdatedAtMethod;
    private final Method getAccessTokenMethod;
    private final Method getRefreshTokenMethod;

    @SuppressWarnings("unchecked")
    public OAuthTokenRepositoryImpl(OAuthTokenMapper oAuthTokenMapper, EncryptionService encryptionService) {
        this.oAuthTokenMapper = oAuthTokenMapper;
        this.encryptionService = encryptionService;

        try {
            // 缓存反射方法
            this.tokenConstructor = (Constructor<OAuthToken>) OAuthToken.class.getDeclaredConstructor();
            this.tokenConstructor.setAccessible(true);

            this.setIdMethod = OAuthToken.class.getDeclaredMethod("setId", OAuthTokenId.class);
            this.setPlatformMethod = OAuthToken.class.getDeclaredMethod("setPlatform", String.class);
            this.setAccessTokenMethod = OAuthToken.class.getDeclaredMethod("setAccessToken", String.class);
            this.setRefreshTokenMethod = OAuthToken.class.getDeclaredMethod("setRefreshToken", String.class);
            this.setExpiresAtMethod = OAuthToken.class.getDeclaredMethod("setExpiresAt", LocalDateTime.class);
            this.setCreatedAtMethod = OAuthToken.class.getDeclaredMethod("setCreatedAt", LocalDateTime.class);
            this.setUpdatedAtMethod = OAuthToken.class.getDeclaredMethod("setUpdatedAt", LocalDateTime.class);
            this.getAccessTokenMethod = OAuthToken.class.getDeclaredMethod("getAccessToken");
            this.getRefreshTokenMethod = OAuthToken.class.getDeclaredMethod("getRefreshToken");

            // 设置可访问
            this.setIdMethod.setAccessible(true);
            this.setPlatformMethod.setAccessible(true);
            this.setAccessTokenMethod.setAccessible(true);
            this.setRefreshTokenMethod.setAccessible(true);
            this.setExpiresAtMethod.setAccessible(true);
            this.setCreatedAtMethod.setAccessible(true);
            this.setUpdatedAtMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Failed to initialize OAuthToken reflection methods", e);
        }
    }

    @Override
    public OAuthToken save(OAuthToken token) {
        try {
            if (token.getId() == null) {
                // 新增：创建加密后的副本并保存
                OAuthToken encryptedToken = createEncryptedCopy(token);
                oAuthTokenMapper.insert(encryptedToken);
            } else {
                // 更新：创建加密后的副本并保存
                OAuthToken encryptedToken = createEncryptedCopy(token);
                oAuthTokenMapper.update(encryptedToken);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to save OAuth token", e);
        }

        return token;
    }

    @Override
    public Optional<OAuthToken> findByPlatform(String platform) {
        OAuthToken token = oAuthTokenMapper.findByPlatform(platform);
        if (token != null) {
            decryptTokenFields(token);
        }
        return Optional.ofNullable(token);
    }

    @Override
    public void deleteByPlatform(String platform) {
        oAuthTokenMapper.deleteByPlatform(platform);
    }

    @Override
    public List<OAuthToken> findAll() {
        List<OAuthToken> tokens = oAuthTokenMapper.findAll();
        tokens.forEach(this::decryptTokenFields);
        return tokens;
    }

    /**
     * 创建加密后的 Token 副本。
     *
     * @param token 原始 Token
     * @return 加密后的 Token 副本
     */
    private OAuthToken createEncryptedCopy(OAuthToken token) {
        try {
            // 获取原始值
            String plainAccessToken = (String) getAccessTokenMethod.invoke(token);
            String plainRefreshToken = (String) getRefreshTokenMethod.invoke(token);

            // 加密
            String encryptedAccessToken = encryptionService.encrypt(plainAccessToken);
            String encryptedRefreshToken = encryptionService.encrypt(plainRefreshToken);

            // 创建副本并设置加密值
            OAuthToken encrypted = tokenConstructor.newInstance();
            setIdMethod.invoke(encrypted, token.getId());
            setPlatformMethod.invoke(encrypted, token.getPlatform());
            setAccessTokenMethod.invoke(encrypted, encryptedAccessToken);
            setRefreshTokenMethod.invoke(encrypted, encryptedRefreshToken);
            setExpiresAtMethod.invoke(encrypted, token.getExpiresAt());
            setCreatedAtMethod.invoke(encrypted, token.getCreatedAt());
            setUpdatedAtMethod.invoke(encrypted, token.getUpdatedAt());

            return encrypted;
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt OAuth token fields", e);
        }
    }

    /**
     * 解密 OAuth Token 的敏感字段。
     *
     * @param token 加密存储的 OAuth Token
     */
    private void decryptTokenFields(OAuthToken token) {
        try {
            // 获取加密值
            String encryptedAccessToken = (String) getAccessTokenMethod.invoke(token);
            String encryptedRefreshToken = (String) getRefreshTokenMethod.invoke(token);

            // 解密并设置
            setAccessTokenMethod.invoke(token, encryptionService.decrypt(encryptedAccessToken));
            setRefreshTokenMethod.invoke(token, encryptionService.decrypt(encryptedRefreshToken));
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt OAuth token fields", e);
        }
    }
}
