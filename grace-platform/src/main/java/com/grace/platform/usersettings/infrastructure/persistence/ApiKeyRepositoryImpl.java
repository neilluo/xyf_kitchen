package com.grace.platform.usersettings.infrastructure.persistence;

import com.grace.platform.shared.domain.id.ApiKeyId;
import com.grace.platform.usersettings.domain.model.ApiKey;
import com.grace.platform.usersettings.domain.repository.ApiKeyRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ApiKey 仓储实现类。
 * <p>
 * 基于 MyBatis 实现 ApiKey 聚合根的持久化操作。
 * MyBatis 直接映射到领域对象，无需 Entity ↔ Domain 转换。
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
@Repository
public class ApiKeyRepositoryImpl implements ApiKeyRepository {

    private final ApiKeyMapper apiKeyMapper;

    public ApiKeyRepositoryImpl(ApiKeyMapper apiKeyMapper) {
        this.apiKeyMapper = apiKeyMapper;
    }

    @Override
    public Optional<ApiKey> findById(ApiKeyId id) {
        ApiKey apiKey = apiKeyMapper.findById(id.value());
        return Optional.ofNullable(apiKey);
    }

    @Override
    public List<ApiKey> findAll() {
        return apiKeyMapper.findAll();
    }

    @Override
    public ApiKey save(ApiKey apiKey) {
        // API Key 一旦创建不可更新，只支持插入
        apiKeyMapper.insert(apiKey);
        return apiKey;
    }

    @Override
    public void deleteById(ApiKeyId id) {
        apiKeyMapper.deleteById(id.value());
    }
}
