package com.grace.platform.distribution.infrastructure.persistence;

import com.grace.platform.distribution.domain.OAuthToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * OAuthToken 数据访问 Mapper 接口。
 * <p>
 * 定义 OAuthToken 实体的数据库操作，由 MyBatis 实现。
 * 注意：access_token 和 refresh_token 存储为加密后的密文。
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
@Mapper
public interface OAuthTokenMapper {

    /**
     * 根据 ID 查询 OAuth Token。
     *
     * @param id Token ID
     * @return OAuth Token 对象，未找到返回 null
     */
    OAuthToken findById(@Param("id") String id);

    /**
     * 根据平台标识查询 OAuth Token。
     *
     * @param platform 平台标识
     * @return OAuth Token 对象，未找到返回 null
     */
    OAuthToken findByPlatform(@Param("platform") String platform);

    /**
     * 查询所有 OAuth Token。
     *
     * @return OAuth Token 列表
     */
    List<OAuthToken> findAll();

    /**
     * 插入新 OAuth Token。
     *
     * @param token OAuth Token 对象
     */
    void insert(OAuthToken token);

    /**
     * 更新 OAuth Token 信息。
     *
     * @param token OAuth Token 对象
     */
    void update(OAuthToken token);

    /**
     * 根据平台标识删除 OAuth Token。
     *
     * @param platform 平台标识
     */
    void deleteByPlatform(@Param("platform") String platform);
}
