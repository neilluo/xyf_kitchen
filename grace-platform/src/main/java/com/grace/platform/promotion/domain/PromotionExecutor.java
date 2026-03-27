package com.grace.platform.promotion.domain;

import com.grace.platform.promotion.domain.vo.PromotionCopy;

/**
 * 推广执行器策略接口
 * <p>
 * 定义不同推广渠道的执行策略，通过 Strategy 模式实现渠道的可扩展性。
 * </p>
 *
 * @see PromotionExecutorRegistry
 */
public interface PromotionExecutor {

    /**
     * 返回渠道类型标识
     * <p>
     * 例如: "opencrawl", "weibo_native" 等
     * </p>
     *
     * @return 渠道类型标识字符串
     */
    String channelType();

    /**
     * 执行推广
     *
     * @param copy    推广文案
     * @param channel 推广渠道配置
     * @return 推广执行结果
     */
    PromotionResult execute(PromotionCopy copy, PromotionChannel channel);
}
