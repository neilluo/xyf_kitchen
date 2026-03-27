package com.grace.platform.usersettings.domain.service;

import com.grace.platform.shared.domain.id.ApiKeyId;
import com.grace.platform.shared.infrastructure.encryption.ApiKeyHashService;
import com.grace.platform.shared.infrastructure.encoding.Base62;
import com.grace.platform.usersettings.domain.model.ApiKey;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * API Key 生成服务实现
 * <p>
 * 实现安全的 API Key 生成流程：
 * 1. 使用 SecureRandom 生成 32 字节随机数（256 bit）
 * 2. Base62 编码生成可读字符串
 * 3. 添加前缀 "grc_" 形成 rawKey
 * 4. 构造前缀显示字符串（前8位 + "..." + 后4位）
 * 5. BCrypt 哈希存储
 * </p>
 */
@Service
public class ApiKeyGenerationServiceImpl implements ApiKeyGenerationService {

    private static final String KEY_PREFIX = "grc_";
    private static final int KEY_LENGTH = 32; // 32 字节 = 256 bit

    private final ApiKeyHashService apiKeyHashService;

    public ApiKeyGenerationServiceImpl(ApiKeyHashService apiKeyHashService) {
        this.apiKeyHashService = apiKeyHashService;
    }

    @Override
    public GeneratedApiKey generate(String name, int expiresInDays) {
        // 1. 使用 SecureRandom 生成随机字节
        byte[] randomBytes = new byte[KEY_LENGTH];
        new SecureRandom().nextBytes(randomBytes);

        // 2. Base62 编码 → rawKey = "grc_" + encoded
        String encoded = Base62.encode(randomBytes);
        String rawKey = KEY_PREFIX + encoded;

        // 3. 构造前缀：grc_<前4位>...<后4位>
        // rawKey 格式为 "grc_" + base62，所以前8位是 "grc_" + 前4位编码
        String prefix = rawKey.substring(0, 8) + "..." + rawKey.substring(rawKey.length() - 4);

        // 4. BCrypt 哈希
        String hashedKey = apiKeyHashService.hash(rawKey);

        // 5. 构造 ApiKey 聚合
        ApiKey apiKey = ApiKey.create(
            new ApiKeyId(UUID.randomUUID().toString()),
            name,
            hashedKey,
            prefix,
            LocalDateTime.now().plusDays(expiresInDays)
        );

        return new GeneratedApiKey(rawKey, apiKey);
    }
}
