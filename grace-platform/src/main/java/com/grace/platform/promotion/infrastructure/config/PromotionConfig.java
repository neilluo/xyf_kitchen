package com.grace.platform.promotion.infrastructure.config;

import com.grace.platform.promotion.domain.PromotionExecutor;
import com.grace.platform.promotion.domain.PromotionExecutorRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Promotion 上下文配置类
 * <p>
 * 注册 Promotion 相关的 Spring Bean，包括：
 * - PromotionExecutorRegistry：推广执行器注册表（Strategy + Registry 模式）
 * </p>
 *
 * @author Grace Platform
 * @since 1.0.0
 */
@Configuration
public class PromotionConfig {

    /**
     * 注册 PromotionExecutorRegistry Bean
     * <p>
     * 自动收集所有实现了 PromotionExecutor 接口的 Spring Bean，
     * 构建执行器注册表，用于根据渠道类型路由到对应的执行器。
     * </p>
     *
     * @param executors 所有推广执行器实现列表（由 Spring 自动注入）
     * @return 推广执行器注册表
     */
    @Bean
    public PromotionExecutorRegistry promotionExecutorRegistry(List<PromotionExecutor> executors) {
        return new PromotionExecutorRegistry(executors);
    }
}
