package com.grace.platform.usersettings.domain.service;

import com.grace.platform.usersettings.domain.model.ApiKey;

/**
 * API Key 生成结果值对象
 * <p>
 * 包含明文密钥（仅在创建响应中返回一次）和持久化用的 ApiKey 聚合根（含哈希值）。
 * </p>
 *
 * @param rawKey 明文密钥，仅在创建响应中返回一次
 * @param apiKey 持久化用的聚合根（含 hashedKey）
 */
public record GeneratedApiKey(
    String rawKey,
    ApiKey apiKey
) {}
