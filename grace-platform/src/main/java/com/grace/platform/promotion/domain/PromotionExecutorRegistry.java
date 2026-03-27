package com.grace.platform.promotion.domain;

import com.grace.platform.shared.ErrorCode;
import com.grace.platform.shared.infrastructure.exception.BusinessRuleViolationException;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 推广执行器注册表
 * <p>
 * 使用 Registry 模式管理所有 {@link PromotionExecutor} 实现，
 * 支持根据渠道类型动态获取对应的执行器。
 * </p>
 *
 * <p><strong>使用方式：</strong></p>
 * <pre>
 * // 注册为 Spring Bean（自动注入所有 PromotionExecutor）
 * &#64;Configuration
 * public class PromotionConfig {
 *     &#64;Bean
 *     public PromotionExecutorRegistry promotionExecutorRegistry(List&lt;PromotionExecutor&gt; executors) {
 *         return new PromotionExecutorRegistry(executors);
 *     }
 * }
 *
 * // 使用
 * PromotionExecutor executor = registry.getExecutor("opencrawl");
 * </pre>
 *
 * @see PromotionExecutor
 */
public class PromotionExecutorRegistry {

    private final Map<String, PromotionExecutor> executors;

    /**
     * 构造注册表
     *
     * @param executorList 所有可用的推广执行器列表
     */
    public PromotionExecutorRegistry(List<PromotionExecutor> executorList) {
        this.executors = executorList.stream()
                .collect(Collectors.toMap(PromotionExecutor::channelType, Function.identity()));
    }

    /**
     * 根据渠道类型获取对应的推广执行器
     *
     * @param channelType 渠道类型标识（如 "opencrawl"）
     * @return 对应的 PromotionExecutor 实例
     * @throws BusinessRuleViolationException 当找不到对应渠道类型的执行器时抛出
     */
    public PromotionExecutor getExecutor(String channelType) {
        PromotionExecutor executor = executors.get(channelType);
        if (executor == null) {
            throw new BusinessRuleViolationException(
                    ErrorCode.INVALID_CHANNEL_CONFIG,
                    "不支持的推广渠道类型: " + channelType
            );
        }
        return executor;
    }
}
