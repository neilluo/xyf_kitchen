package com.grace.platform.usersettings.infrastructure.persistence;

import com.grace.platform.usersettings.domain.model.NotificationPreference;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * NotificationPreference 数据访问 Mapper 接口。
 * <p>
 * 定义 NotificationPreference 聚合根的数据库操作，由 MyBatis 实现。
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
@Mapper
public interface NotificationPreferenceMapper {

    /**
     * 根据 ID 查询通知偏好。
     *
     * @param id 通知偏好 ID
     * @return 通知偏好对象，未找到返回 null
     */
    NotificationPreference findById(@Param("id") String id);

    /**
     * 插入新通知偏好。
     *
     * @param preference 通知偏好对象
     */
    void insert(NotificationPreference preference);

    /**
     * 更新通知偏好。
     *
     * @param preference 通知偏好对象
     */
    void update(NotificationPreference preference);
}
