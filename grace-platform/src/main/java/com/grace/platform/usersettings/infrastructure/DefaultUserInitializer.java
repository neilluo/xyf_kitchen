package com.grace.platform.usersettings.infrastructure;

import com.grace.platform.shared.domain.id.NotificationPreferenceId;
import com.grace.platform.shared.domain.id.UserProfileId;
import com.grace.platform.usersettings.domain.model.NotificationPreference;
import com.grace.platform.usersettings.domain.model.UserProfile;
import com.grace.platform.usersettings.domain.repository.NotificationPreferenceRepository;
import com.grace.platform.usersettings.domain.repository.UserProfileRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 默认用户初始化器
 * <p>
 * MVP 单用户模式，应用启动时自动创建默认用户和通知偏好。
 * </p>
 */
@Component
public class DefaultUserInitializer implements ApplicationRunner {

    // MVP 单用户模式的固定 ID
    public static final UserProfileId DEFAULT_USER_ID = new UserProfileId("default-user");
    public static final NotificationPreferenceId DEFAULT_NOTIFICATION_ID =
        new NotificationPreferenceId("default-notification");

    private final UserProfileRepository userProfileRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;

    public DefaultUserInitializer(
            UserProfileRepository userProfileRepository,
            NotificationPreferenceRepository notificationPreferenceRepository) {
        this.userProfileRepository = userProfileRepository;
        this.notificationPreferenceRepository = notificationPreferenceRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        // 如果默认用户不存在，创建之
        if (userProfileRepository.findById(DEFAULT_USER_ID).isEmpty()) {
            UserProfile profile = UserProfile.createDefault(DEFAULT_USER_ID, "Grace Chef");
            userProfileRepository.save(profile);
        }

        // 如果默认通知偏好不存在，创建之
        if (notificationPreferenceRepository.findById(DEFAULT_NOTIFICATION_ID).isEmpty()) {
            NotificationPreference preference =
                NotificationPreference.createDefault(DEFAULT_NOTIFICATION_ID);
            notificationPreferenceRepository.save(preference);
        }
    }
}
