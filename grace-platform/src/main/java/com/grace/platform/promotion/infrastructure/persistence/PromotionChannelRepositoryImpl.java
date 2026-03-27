package com.grace.platform.promotion.infrastructure.persistence;

import com.grace.platform.promotion.domain.ChannelStatus;
import com.grace.platform.promotion.domain.PromotionChannel;
import com.grace.platform.promotion.domain.PromotionChannelRepository;
import com.grace.platform.shared.domain.id.ChannelId;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * PromotionChannel 仓储实现类。
 * <p>
 * 基于 MyBatis 实现 PromotionChannel 聚合根的持久化操作。
 * MyBatis 直接映射到领域对象，无需 Entity ↔ Domain 转换。
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
@Repository
public class PromotionChannelRepositoryImpl implements PromotionChannelRepository {

    private final PromotionChannelMapper promotionChannelMapper;

    public PromotionChannelRepositoryImpl(PromotionChannelMapper promotionChannelMapper) {
        this.promotionChannelMapper = promotionChannelMapper;
    }

    @Override
    public PromotionChannel save(PromotionChannel channel) {
        if (channel.getId() == null) {
            // 新增
            promotionChannelMapper.insert(channel);
        } else {
            // 更新
            promotionChannelMapper.update(channel);
        }
        return channel;
    }

    @Override
    public Optional<PromotionChannel> findById(ChannelId id) {
        PromotionChannel channel = promotionChannelMapper.findById(id.value());
        return Optional.ofNullable(channel);
    }

    @Override
    public List<PromotionChannel> findAll() {
        return promotionChannelMapper.findAll();
    }

    @Override
    public List<PromotionChannel> findByStatus(ChannelStatus status) {
        return promotionChannelMapper.findByStatus(status.name());
    }

    @Override
    public void deleteById(ChannelId id) {
        promotionChannelMapper.deleteById(id.value());
    }

    @Override
    public boolean existsPromotionRecordByChannelId(ChannelId channelId) {
        long count = promotionChannelMapper.countPromotionRecordsByChannelId(channelId.value());
        return count > 0;
    }
}
