package com.grace.platform.usersettings.infrastructure.persistence;

import com.grace.platform.usersettings.domain.model.UserProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * UserProfile 数据访问 Mapper 接口。
 * <p>
 * 定义 UserProfile 聚合根的数据库操作，由 MyBatis 实现。
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
@Mapper
public interface UserProfileMapper {

    /**
     * 根据 ID 查询用户资料。
     *
     * @param id 用户资料 ID
     * @return 用户资料对象，未找到返回 null
     */
    UserProfile findById(@Param("id") String id);

    /**
     * 插入新用户资料。
     *
     * @param profile 用户资料对象
     */
    void insert(UserProfile profile);

    /**
     * 更新用户资料。
     *
     * @param profile 用户资料对象
     */
    void update(UserProfile profile);
}
