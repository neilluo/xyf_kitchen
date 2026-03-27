package com.grace.platform.distribution.domain;

import java.util.Objects;

/**
 * 平台信息值对象
 * <p>
 * 表示分发平台的元数据信息，用于前端展示平台列表和授权状态。
 * </p>
 *
 * @param platform    平台标识（如 "youtube"）
 * @param displayName 显示名称（如 "YouTube"）
 * @param enabled     平台是否可用
 */
public record PlatformInfo(
    String platform,
    String displayName,
    boolean enabled
) {
    /**
     * 创建平台信息
     *
     * @param platform    平台标识，不能为空
     * @param displayName 显示名称，不能为空
     * @param enabled     是否启用
     */
    public PlatformInfo {
        if (platform == null || platform.isBlank()) {
            throw new IllegalArgumentException("platform must not be blank");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }
    }
}
