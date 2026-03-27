package com.grace.platform.usersettings.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 存储配置属性
 * <p>
 * 从 application.yml 加载用户头像存储相关配置。
 * </p>
 */
@Component
@ConfigurationProperties(prefix = "grace.storage")
public class StorageProperties {

    private String avatarDir;
    private String avatarMaxSize;
    private List<String> avatarAllowedTypes;

    public String getAvatarDir() {
        return avatarDir;
    }

    public void setAvatarDir(String avatarDir) {
        this.avatarDir = avatarDir;
    }

    public String getAvatarMaxSize() {
        return avatarMaxSize;
    }

    public void setAvatarMaxSize(String avatarMaxSize) {
        this.avatarMaxSize = avatarMaxSize;
    }

    public List<String> getAvatarAllowedTypes() {
        return avatarAllowedTypes;
    }

    public void setAvatarAllowedTypes(List<String> avatarAllowedTypes) {
        this.avatarAllowedTypes = avatarAllowedTypes;
    }
}
