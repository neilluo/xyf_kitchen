package com.grace.platform.metadata.domain.event;

import com.grace.platform.shared.domain.DomainEvent;
import com.grace.platform.shared.domain.id.MetadataId;
import com.grace.platform.shared.domain.id.VideoId;

/**
 * 元数据确认领域事件
 * <p>
 * 当元数据被用户确认后发布，通知 Distribution 上下文视频已就绪可发布。
 * 由 MetadataApplicationService.confirmMetadata() 成功后发布。
 * </p>
 */
public class MetadataConfirmedEvent extends DomainEvent {

    private final VideoId videoId;
    private final MetadataId metadataId;

    /**
     * 创建元数据确认事件
     *
     * @param videoId    视频 ID
     * @param metadataId 元数据 ID
     */
    public MetadataConfirmedEvent(VideoId videoId, MetadataId metadataId) {
        super();
        this.videoId = videoId;
        this.metadataId = metadataId;
    }

    /**
     * 获取视频 ID
     *
     * @return 视频 ID
     */
    public VideoId getVideoId() {
        return videoId;
    }

    /**
     * 获取元数据 ID
     *
     * @return 元数据 ID
     */
    public MetadataId getMetadataId() {
        return metadataId;
    }
}
