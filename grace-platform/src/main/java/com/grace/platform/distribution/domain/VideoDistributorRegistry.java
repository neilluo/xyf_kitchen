package com.grace.platform.distribution.domain;

import com.grace.platform.shared.ErrorCode;
import com.grace.platform.shared.infrastructure.exception.BusinessRuleViolationException;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 视频分发器注册表
 * <p>
 * Registry 模式实现。管理所有已注册的 {@link VideoDistributor} 实现，
 * 提供平台到分发器的路由查找功能。
 * </p>
 * <p>
 * Spring 自动装配机制：所有标注 {@code @Component} 的 {@link VideoDistributor}
 * 实现会被自动注入到构造器的 {@code List<VideoDistributor>} 参数中。
 * </p>
 *
 * @author Grace Platform Team
 * @see VideoDistributor
 * @see com.grace.platform.distribution.infrastructure.config.DistributionConfig
 */
public class VideoDistributorRegistry {

    private final Map<String, VideoDistributor> distributors;

    /**
     * 构造分发器注册表
     * <p>
     * Spring 自动装配所有 {@link VideoDistributor} 实现，构造器将其转换为
     * {@code Map<platform, distributor>} 以支持快速查找。
     * </p>
     *
     * @param distributorList 所有已注册的 VideoDistributor 实现列表
     */
    public VideoDistributorRegistry(List<VideoDistributor> distributorList) {
        this.distributors = distributorList.stream()
                .collect(Collectors.toMap(VideoDistributor::platform, Function.identity()));
    }

    /**
     * 获取指定平台的分发器
     * <p>
     * 根据平台标识查找对应的分发器实现。如果平台未注册，抛出
     * {@link BusinessRuleViolationException}（错误码 3001）。
     * </p>
     *
     * @param platform 平台标识（如 "youtube"）
     * @return 该平台的分发器实现
     * @throws BusinessRuleViolationException 如果平台未注册（错误码 3001）
     */
    public VideoDistributor getDistributor(String platform) {
        VideoDistributor distributor = distributors.get(platform);
        if (distributor == null) {
            throw new BusinessRuleViolationException(
                    ErrorCode.UNSUPPORTED_PLATFORM,
                    "不支持的分发平台: " + platform + "。支持的平台: " + distributors.keySet()
            );
        }
        return distributor;
    }

    /**
     * 列出所有已注册的平台信息
     * <p>
     * 返回系统中所有可用平台的列表，包含平台标识、显示名称和启用状态。
     * </p>
     *
     * @return 平台信息列表
     */
    public List<PlatformInfo> listPlatforms() {
        return distributors.values().stream()
                .map(d -> new PlatformInfo(d.platform(), getDisplayName(d), isEnabled(d)))
                .toList();
    }

    /**
     * 获取分发器的显示名称
     * <p>
     * 默认实现返回平台标识的首字母大写形式。子类可覆盖此方法
     * 提供更友好的显示名称。
     * </p>
     *
     * @param distributor 分发器
     * @return 显示名称
     */
    private String getDisplayName(VideoDistributor distributor) {
        String platform = distributor.platform();
        if (platform == null || platform.isEmpty()) {
            return "Unknown";
        }
        return platform.substring(0, 1).toUpperCase() + platform.substring(1).toLowerCase();
    }

    /**
     * 判断分发器是否启用
     * <p>
     * 默认实现始终返回 true。子类可根据配置或健康检查
     * 覆盖此方法。
     * </p>
     *
     * @param distributor 分发器
     * @return 是否启用
     */
    private boolean isEnabled(VideoDistributor distributor) {
        return true;
    }
}
