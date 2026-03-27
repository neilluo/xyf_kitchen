package com.grace.platform.usersettings.domain.repository;

import com.grace.platform.shared.domain.id.NotificationPreferenceId;
import com.grace.platform.usersettings.domain.model.NotificationPreference;

import java.util.Optional;

/**
 * NotificationPreference 仓储接口
 * <p>
 * MVP 单用户模式，NotificationPreference 只有一条记录。
 * </p>
 */
public interface NotificationPreferenceRepository {

    /**
     * 根据 ID 查询通知偏好
     *
     * @param id 通知偏好 ID
     * @return Optional<NotificationPreference>
     */
    Optional<NotificationPreference> findById(NotificationPreferenceId id);

    /**
     * 保存通知偏好
     *
     * @param preference 通知偏好
     * @return 保存后的 NotificationPreference
     */
    NotificationPreference save(NotificationPreference preference);
}
