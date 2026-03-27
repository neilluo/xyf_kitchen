package com.grace.platform.usersettings.infrastructure.persistence;

import com.grace.platform.shared.domain.id.UserProfileId;
import com.grace.platform.usersettings.domain.model.UserProfile;
import com.grace.platform.usersettings.domain.repository.UserProfileRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * UserProfile 仓储实现类。
 * <p>
 * 基于 MyBatis 实现 UserProfile 聚合根的持久化操作。
 * MyBatis 直接映射到领域对象，无需 Entity ↔ Domain 转换。
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
@Repository
public class UserProfileRepositoryImpl implements UserProfileRepository {

    private final UserProfileMapper userProfileMapper;

    public UserProfileRepositoryImpl(UserProfileMapper userProfileMapper) {
        this.userProfileMapper = userProfileMapper;
    }

    @Override
    public Optional<UserProfile> findById(UserProfileId id) {
        UserProfile profile = userProfileMapper.findById(id.value());
        return Optional.ofNullable(profile);
    }

    @Override
    public UserProfile save(UserProfile profile) {
        if (profile.getId() == null) {
            // 新增
            userProfileMapper.insert(profile);
        } else {
            // 更新
            userProfileMapper.update(profile);
        }
        return profile;
    }
}
