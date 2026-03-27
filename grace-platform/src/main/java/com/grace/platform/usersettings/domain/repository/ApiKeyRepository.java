package com.grace.platform.usersettings.domain.repository;

import com.grace.platform.shared.domain.id.ApiKeyId;
import com.grace.platform.usersettings.domain.model.ApiKey;

import java.util.List;
import java.util.Optional;

/**
 * ApiKey 仓储接口
 * <p>
 * 管理 API Key 的 CRUD 操作。
 * </p>
 */
public interface ApiKeyRepository {

    /**
     * 根据 ID 查询 API Key
     *
     * @param id API Key ID
     * @return Optional<ApiKey>
     */
    Optional<ApiKey> findById(ApiKeyId id);

    /**
     * 查询所有 API Keys
     *
     * @return List<ApiKey>
     */
    List<ApiKey> findAll();

    /**
     * 保存 API Key
     *
     * @param apiKey API Key
     * @return 保存后的 ApiKey
     */
    ApiKey save(ApiKey apiKey);

    /**
     * 根据 ID 删除 API Key
     *
     * @param id API Key ID
     */
    void deleteById(ApiKeyId id);
}
