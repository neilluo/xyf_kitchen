package com.grace.platform.promotion.domain;

import com.grace.platform.shared.domain.id.ChannelId;

import java.util.List;
import java.util.Optional;

/**
 * 推广渠道仓储接口
 * <p>
 * 定义 PromotionChannel 聚合根的持久化操作，由基础设施层实现。
 * </p>
 */
public interface PromotionChannelRepository {

    /**
     * 保存渠道（新增或更新）
     *
     * @param channel 渠道实体
     * @return 保存后的渠道实体
     */
    PromotionChannel save(PromotionChannel channel);

    /**
     * 根据 ID 查询渠道
     *
     * @param id 渠道 ID
     * @return 渠道 Optional 包装
     */
    Optional<PromotionChannel> findById(ChannelId id);

    /**
     * 查询所有渠道
     *
     * @return 渠道列表
     */
    List<PromotionChannel> findAll();

    /**
     * 按状态筛选渠道
     *
     * @param status 渠道状态
     * @return 符合条件的渠道列表
     */
    List<PromotionChannel> findByStatus(ChannelStatus status);

    /**
     * 删除指定渠道
     *
     * @param id 渠道 ID
     */
    void deleteById(ChannelId id);

    /**
     * 检查是否有关联的推广记录
     * <p>
     * 用于决定是软删除（有关联记录时禁用）还是硬删除（无关联记录时删除）。
     * </p>
     *
     * @param channelId 渠道 ID
     * @return true 如果存在关联的推广记录
     */
    boolean existsPromotionRecordByChannelId(ChannelId channelId);
}
