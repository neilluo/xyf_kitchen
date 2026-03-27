package com.grace.platform.distribution.domain;

import java.util.List;
import java.util.Optional;

/**
 * OAuth Token 仓储接口
 * <p>
 * 定义 OAuthToken 实体的持久化操作，由基础设施层实现。
 * Token 字段（accessToken/refreshToken）在存储时加密，读取时解密。
 * </p>
 */
public interface OAuthTokenRepository {

    /**
     * 保存 OAuth Token（新增或更新）
     *
     * @param token OAuth Token
     * @return 保存后的 OAuth Token
     */
    OAuthToken save(OAuthToken token);

    /**
     * 根据平台标识查询 OAuth Token
     *
     * @param platform 平台标识
     * @return OAuth Token Optional 包装
     */
    Optional<OAuthToken> findByPlatform(String platform);

    /**
     * 根据平台标识删除 OAuth Token
     *
     * @param platform 平台标识
     */
    void deleteByPlatform(String platform);

    /**
     * 查询所有 OAuth Token
     *
     * @return OAuth Token 列表
     */
    List<OAuthToken> findAll();
}
