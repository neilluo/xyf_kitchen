package com.grace.platform.promotion.domain;

import com.grace.platform.metadata.domain.VideoMetadata;
import com.grace.platform.promotion.domain.vo.PromotionCopy;

/**
 * 推广文案生成服务接口
 * <p>
 * 领域服务接口，定义在 domain 层，由 infrastructure 层实现。
 * 内部调用 Metadata 上下文的 LlmService 来生成推广文案。
 * </p>
 */
public interface PromotionCopyGenerationService {

    /**
     * 为指定渠道生成推广文案
     *
     * @param metadata 视频元数据
     * @param channel  推广渠道
     * @param videoUrl 视频 URL
     * @return 生成的推广文案值对象
     */
    PromotionCopy generate(VideoMetadata metadata, PromotionChannel channel, String videoUrl);
}
