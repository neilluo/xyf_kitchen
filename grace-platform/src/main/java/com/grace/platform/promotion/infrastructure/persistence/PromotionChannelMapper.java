package com.grace.platform.promotion.infrastructure.persistence;

import com.grace.platform.promotion.domain.ChannelStatus;
import com.grace.platform.promotion.domain.PromotionChannel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * PromotionChannel 数据访问 Mapper 接口。
 * <p>
 * 定义 PromotionChannel 聚合根的数据库操作，由 MyBatis 实现。
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
@Mapper
public interface PromotionChannelMapper {

    /**
     * 根据 ID 查询渠道。
     *
     * @param id 渠道 ID
     * @return 渠道对象，未找到返回 null
     */
    PromotionChannel findById(@Param("id") String id);

    /**
     * 查询所有渠道。
     *
     * @return 渠道列表
     */
    List<PromotionChannel> findAll();

    /**
     * 按状态筛选渠道。
     *
     * @param status 渠道状态
     * @return 符合条件的渠道列表
     */
    List<PromotionChannel> findByStatus(@Param("status") String status);

    /**
     * 检查是否有关联的推广记录。
     *
     * @param channelId 渠道 ID
     * @return 关联记录数量
     */
    long countPromotionRecordsByChannelId(@Param("channelId") String channelId);

    /**
     * 插入新渠道。
     *
     * @param channel 渠道对象
     */
    void insert(PromotionChannel channel);

    /**
     * 更新渠道信息。
     *
     * @param channel 渠道对象
     */
    void update(PromotionChannel channel);

    /**
     * 删除指定渠道。
     *
     * @param id 渠道 ID
     */
    void deleteById(@Param("id") String id);
}
