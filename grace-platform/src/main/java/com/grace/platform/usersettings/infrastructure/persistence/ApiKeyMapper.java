package com.grace.platform.usersettings.infrastructure.persistence;

import com.grace.platform.usersettings.domain.model.ApiKey;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * ApiKey 数据访问 Mapper 接口。
 * <p>
 * 定义 ApiKey 聚合根的数据库操作，由 MyBatis 实现。
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
@Mapper
public interface ApiKeyMapper {

    /**
     * 根据 ID 查询 API Key。
     *
     * @param id API Key ID
     * @return API Key 对象，未找到返回 null
     */
    ApiKey findById(@Param("id") String id);

    /**
     * 查询所有 API Keys。
     *
     * @return API Key 列表
     */
    List<ApiKey> findAll();

    /**
     * 插入新 API Key。
     *
     * @param apiKey API Key 对象
     */
    void insert(ApiKey apiKey);

    /**
     * 根据 ID 删除 API Key。
     *
     * @param id API Key ID
     */
    void deleteById(@Param("id") String id);
}
