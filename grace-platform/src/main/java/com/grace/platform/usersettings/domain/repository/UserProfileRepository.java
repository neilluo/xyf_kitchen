package com.grace.platform.usersettings.domain.repository;

import com.grace.platform.shared.domain.id.UserProfileId;
import com.grace.platform.usersettings.domain.model.UserProfile;

import java.util.Optional;

/**
 * UserProfile 仓储接口
 * <p>
 * MVP 单用户模式，UserProfile 只有一条记录。
 * </p>
 */
public interface UserProfileRepository {

    /**
     * 根据 ID 查询用户资料
     *
     * @param id 用户资料 ID
     * @return Optional<UserProfile>
     */
    Optional<UserProfile> findById(UserProfileId id);

    /**
     * 保存用户资料
     *
     * @param profile 用户资料
     * @return 保存后的 UserProfile
     */
    UserProfile save(UserProfile profile);
}
