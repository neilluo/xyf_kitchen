package com.grace.platform.usersettings.infrastructure.persistence;

import com.grace.platform.shared.domain.id.NotificationPreferenceId;
import com.grace.platform.usersettings.domain.model.NotificationPreference;
import com.grace.platform.usersettings.domain.repository.NotificationPreferenceRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * NotificationPreference 仓储实现类。
 * <p>
 * 基于 MyBatis 实现 NotificationPreference 聚合根的持久化操作。
 * MyBatis 直接映射到领域对象，无需 Entity ↔ Domain 转换。
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
@Repository
public class NotificationPreferenceRepositoryImpl implements NotificationPreferenceRepository {

    private final NotificationPreferenceMapper notificationPreferenceMapper;

    public NotificationPreferenceRepositoryImpl(NotificationPreferenceMapper notificationPreferenceMapper) {
        this.notificationPreferenceMapper = notificationPreferenceMapper;
    }

    @Override
    public Optional<NotificationPreference> findById(NotificationPreferenceId id) {
        NotificationPreference preference = notificationPreferenceMapper.findById(id.value());
        return Optional.ofNullable(preference);
    }

    @Override
    public NotificationPreference save(NotificationPreference preference) {
        // 根据数据库中是否存在记录决定是插入还是更新
        if (notificationPreferenceMapper.findById(preference.getId().value()) == null) {
            // 新增
            notificationPreferenceMapper.insert(preference);
        } else {
            // 更新
            notificationPreferenceMapper.update(preference);
        }
        return preference;
    }
}
