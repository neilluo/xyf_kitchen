package com.grace.platform.distribution.infrastructure.config;

import com.grace.platform.distribution.domain.VideoDistributor;
import com.grace.platform.distribution.domain.VideoDistributorRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Distribution 上下文配置类
 * <p>
 * 负责配置 Distribution 限界上下文的基础设施层 Bean，
 * 包括视频分发器注册表（VideoDistributorRegistry）的初始化。
 * </p>
 * <p>
 * Registry 模式是平台可扩展架构的核心。所有标注 {@code @Component} 的
 * {@link VideoDistributor} 实现会被 Spring 自动装配，注入到注册表中。
 * 新增平台只需实现 {@link VideoDistributor} 接口并添加 {@code @Component}
 * 注解，无需修改任何已有代码即可自动注册。
 * </p>
 *
 * @author Grace Platform Team
 * @see VideoDistributorRegistry
 * @see VideoDistributor
 */
@Configuration
public class DistributionConfig {

    /**
     * 创建视频分发器注册表 Bean
     * <p>
     * Spring 自动装配所有 {@link VideoDistributor} 实现（标注 {@code @Component} 的类），
     * 注入到 {@link VideoDistributorRegistry} 的构造器中。注册表将分发器按平台标识
     * （{@link VideoDistributor#platform()} 返回值）索引，支持快速查找。
     * </p>
     * <p>
     * 当前实现：{@link com.grace.platform.distribution.infrastructure.youtube.YouTubeDistributor}
     * 未来可通过添加新的 {@link VideoDistributor} 实现来支持更多平台（如抖音、B站等），
     * 无需修改此配置类或注册表代码。
     * </p>
     *
     * @param distributors 所有已注册的 VideoDistributor 实现列表（Spring 自动注入）
     * @return 视频分发器注册表实例
     */
    @Bean
    public VideoDistributorRegistry videoDistributorRegistry(List<VideoDistributor> distributors) {
        return new VideoDistributorRegistry(distributors);
    }
}
