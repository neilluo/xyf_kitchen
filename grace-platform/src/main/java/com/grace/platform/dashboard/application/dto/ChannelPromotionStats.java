package com.grace.platform.dashboard.application.dto;

/**
 * 渠道推广统计投影接口。
 * <p>
 * 用于 Dashboard 查询中按渠道分组统计推广记录的结果映射。
 * MyBatis 将通过此接口的 getter 方法进行结果映射。
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
public interface ChannelPromotionStats {

    /**
     * 获取渠道 ID。
     *
     * @return 渠道 ID
     */
    String getChannelId();

    /**
     * 获取总执行次数。
     *
     * @return 总执行次数
     */
    long getTotalExecutions();

    /**
     * 获取成功次数。
     *
     * @return 成功次数
     */
    long getSuccessCount();

    /**
     * 获取失败次数。
     *
     * @return 失败次数
     */
    long getFailedCount();
}
