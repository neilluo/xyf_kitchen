package com.grace.platform.promotion.domain;

import com.grace.platform.shared.application.dto.PageResponse;
import com.grace.platform.shared.domain.PageRequest;
import com.grace.platform.shared.domain.id.ChannelId;
import com.grace.platform.shared.domain.id.PromotionRecordId;
import com.grace.platform.shared.domain.id.VideoId;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 推广记录仓储接口
 * <p>
 * 定义 PromotionRecord 聚合根的持久化操作，由基础设施层实现。
 * </p>
 */
public interface PromotionRecordRepository {

    /**
     * 保存推广记录（新增或更新）
     *
     * @param record 推广记录
     * @return 保存后的推广记录
     */
    PromotionRecord save(PromotionRecord record);

    /**
     * 根据 ID 查询推广记录
     *
     * @param id 推广记录 ID
     * @return 推广记录 Optional 包装
     */
    Optional<PromotionRecord> findById(PromotionRecordId id);

    /**
     * 按视频 ID 分页查询推广历史（对应 F3 端点）
     *
     * @param videoId     视频 ID
     * @param pageRequest 分页请求
     * @param status      状态筛选（可选）
     * @return 分页推广记录
     */
    PageResponse<PromotionRecord> findByVideoId(VideoId videoId, PageRequest pageRequest, PromotionStatus status);

    /**
     * 按视频 ID 查询所有推广记录（用于报告汇总，对应 F4 端点）
     *
     * @param videoId 视频 ID
     * @return 推广记录列表
     */
    List<PromotionRecord> findByVideoIdForReport(VideoId videoId);

    /**
     * 获取渠道成功率统计（用于 Dashboard）
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 各渠道成功率列表
     */
    List<ChannelSuccessRate> getChannelSuccessRates(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 统计指定状态的推广所涉及的不同视频数量（用于 Dashboard）
     *
     * @param status 推广状态
     * @return 不同视频的数量
     */
    long countDistinctVideosByStatus(PromotionStatus status);

    /**
     * 渠道成功率记录
     */
    record ChannelSuccessRate(
        ChannelId channelId,
        String channelName,
        long totalCount,
        long successCount,
        double successRate
    ) {}
}
