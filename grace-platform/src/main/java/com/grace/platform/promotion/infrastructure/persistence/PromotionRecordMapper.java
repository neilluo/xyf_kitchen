package com.grace.platform.promotion.infrastructure.persistence;

import com.grace.platform.promotion.domain.PromotionRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.grace.platform.dashboard.application.dto.ChannelPromotionStats;
import java.time.LocalDateTime;
import java.util.List;

/**
 * PromotionRecord 数据访问 Mapper 接口。
 * <p>
 * 定义 PromotionRecord 聚合根的数据库操作，由 MyBatis 实现。
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
@Mapper
public interface PromotionRecordMapper {

    /**
     * 根据 ID 查询推广记录。
     *
     * @param id 推广记录 ID
     * @return 推广记录对象，未找到返回 null
     */
    PromotionRecord findById(@Param("id") String id);

    /**
     * 按视频 ID 分页查询推广记录。
     *
     * @param videoId 视频 ID
     * @param status  状态筛选（可为 null 表示不限）
     * @param offset  偏移量
     * @param limit   限制数量
     * @return 推广记录列表
     */
    List<PromotionRecord> findByVideoId(@Param("videoId") String videoId,
                                        @Param("status") String status,
                                        @Param("offset") int offset,
                                        @Param("limit") int limit);

    /**
     * 按视频 ID 统计推广记录数量。
     *
     * @param videoId 视频 ID
     * @param status  状态筛选（可为 null 表示不限）
     * @return 记录数量
     */
    long countByVideoId(@Param("videoId") String videoId,
                        @Param("status") String status);

    /**
     * 按视频 ID 查询所有推广记录（用于报告汇总）。
     *
     * @param videoId 视频 ID
     * @return 推广记录列表
     */
    List<PromotionRecord> findByVideoIdForReport(@Param("videoId") String videoId);

    /**
     * 获取渠道成功率统计（用于 Dashboard）。
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 各渠道成功率统计列表
     */
    List<ChannelSuccessRateProjection> getChannelSuccessRates(@Param("startDate") LocalDateTime startDate,
                                                              @Param("endDate") LocalDateTime endDate);

    /**
     * 统计指定状态的推广所涉及的不同视频数量（用于 Dashboard）。
     *
     * @param status 推广状态
     * @return 不同视频的数量
     */
    long countDistinctVideosByStatus(@Param("status") String status);

    /**
     * 统计指定状态的推广所涉及的不同视频数量（用于 Dashboard）。
     * <p>此方法为 {@link #countDistinctVideosByStatus} 的别名，用于保持与文档规范一致。
     *
     * @param status 推广状态
     * @return 不同视频的数量
     */
    default long countDistinctVideoIdByStatus(@Param("status") String status) {
        return countDistinctVideosByStatus(status);
    }

    /**
     * 统计指定时间之后创建的推广记录数量（用于 Dashboard）。
     *
     * @param since 起始时间
     * @return 推广记录数量
     */
    long countByCreatedAtAfter(@Param("since") LocalDateTime since);

    /**
     * 统计指定状态和指定时间之后创建的推广记录数量（用于 Dashboard）。
     *
     * @param status 推广状态
     * @param since  起始时间
     * @return 推广记录数量
     */
    long countByStatusAndCreatedAtAfter(@Param("status") String status,
                                        @Param("since") LocalDateTime since);

    /**
     * 按渠道 ID 分组统计推广记录（用于 Dashboard）。
     *
     * @param since 起始时间
     * @return 各渠道的推广统计
     */
    List<ChannelPromotionStats> countGroupByChannelId(@Param("since") LocalDateTime since);

    /**
     * 插入新推广记录。
     *
     * @param record 推广记录对象
     */
    void insert(PromotionRecord record);

    /**
     * 更新推广记录。
     *
     * @param record 推广记录对象
     */
    void update(PromotionRecord record);

    /**
     * 渠道成功率投影（用于 MyBatis 结果映射）。
     */
    class ChannelSuccessRateProjection {
        private String channelId;
        private String channelName;
        private long totalCount;
        private long successCount;

        public String getChannelId() { return channelId; }
        public void setChannelId(String channelId) { this.channelId = channelId; }
        public String getChannelName() { return channelName; }
        public void setChannelName(String channelName) { this.channelName = channelName; }
        public long getTotalCount() { return totalCount; }
        public void setTotalCount(long totalCount) { this.totalCount = totalCount; }
        public long getSuccessCount() { return successCount; }
        public void setSuccessCount(long successCount) { this.successCount = successCount; }
    }
}
