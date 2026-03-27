package com.grace.platform.promotion.infrastructure.persistence;

import com.grace.platform.promotion.domain.PromotionRecord;
import com.grace.platform.promotion.domain.PromotionRecordRepository;
import com.grace.platform.promotion.domain.PromotionStatus;
import com.grace.platform.shared.application.dto.PageResponse;
import com.grace.platform.shared.domain.PageRequest;
import com.grace.platform.shared.domain.id.ChannelId;
import com.grace.platform.shared.domain.id.PromotionRecordId;
import com.grace.platform.shared.domain.id.VideoId;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * PromotionRecord 仓储实现类。
 * <p>
 * 基于 MyBatis 实现 PromotionRecord 聚合根的持久化操作。
 * MyBatis 直接映射到领域对象，无需 Entity ↔ Domain 转换。
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
@Repository
public class PromotionRecordRepositoryImpl implements PromotionRecordRepository {

    private final PromotionRecordMapper promotionRecordMapper;

    public PromotionRecordRepositoryImpl(PromotionRecordMapper promotionRecordMapper) {
        this.promotionRecordMapper = promotionRecordMapper;
    }

    @Override
    public PromotionRecord save(PromotionRecord record) {
        if (record.getId() == null) {
            // 新增
            promotionRecordMapper.insert(record);
        } else {
            // 更新
            promotionRecordMapper.update(record);
        }
        return record;
    }

    @Override
    public Optional<PromotionRecord> findById(PromotionRecordId id) {
        PromotionRecord record = promotionRecordMapper.findById(id.value());
        return Optional.ofNullable(record);
    }

    @Override
    public PageResponse<PromotionRecord> findByVideoId(VideoId videoId, PageRequest pageRequest, PromotionStatus status) {
        // 计算分页参数
        int offset = pageRequest.page() * pageRequest.pageSize();
        int limit = pageRequest.pageSize();

        // 查询列表
        String statusStr = status != null ? status.name() : null;
        List<PromotionRecord> records = promotionRecordMapper.findByVideoId(
            videoId.value(), statusStr, offset, limit);

        // 查询总数
        long total = promotionRecordMapper.countByVideoId(videoId.value(), statusStr);

        return PageResponse.of(records, total, pageRequest.page(), pageRequest.pageSize());
    }

    @Override
    public List<PromotionRecord> findByVideoIdForReport(VideoId videoId) {
        return promotionRecordMapper.findByVideoIdForReport(videoId.value());
    }

    @Override
    public List<ChannelSuccessRate> getChannelSuccessRates(LocalDateTime startDate, LocalDateTime endDate) {
        List<PromotionRecordMapper.ChannelSuccessRateProjection> projections =
            promotionRecordMapper.getChannelSuccessRates(startDate, endDate);

        return projections.stream()
            .map(p -> {
                double successRate = p.getTotalCount() > 0
                    ? (double) p.getSuccessCount() / p.getTotalCount() * 100
                    : 0.0;
                return new ChannelSuccessRate(
                    new ChannelId(p.getChannelId()),
                    p.getChannelName(),
                    p.getTotalCount(),
                    p.getSuccessCount(),
                    successRate
                );
            })
            .collect(Collectors.toList());
    }

    @Override
    public long countDistinctVideosByStatus(PromotionStatus status) {
        return promotionRecordMapper.countDistinctVideosByStatus(status.name());
    }
}
