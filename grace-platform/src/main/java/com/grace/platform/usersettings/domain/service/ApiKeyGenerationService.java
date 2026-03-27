package com.grace.platform.usersettings.domain.service;

/**
 * API Key 生成服务接口
 * <p>
 * 负责生成安全的 API Key，包括随机字节生成、Base62 编码、BCrypt 哈希等。
 * 明文密钥仅在创建时返回一次。
 * </p>
 */
public interface ApiKeyGenerationService {

    /**
     * 生成新的 API Key
     *
     * @param name          API Key 用途描述
     * @param expiresInDays 有效期天数
     * @return GeneratedApiKey 包含 rawKey（明文，仅此一次）和 ApiKey 聚合（含哈希值）
     */
    GeneratedApiKey generate(String name, int expiresInDays);
}
