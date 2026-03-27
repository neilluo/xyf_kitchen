package com.grace.platform.metadata.domain;

import com.grace.platform.shared.domain.id.MetadataId;
import com.grace.platform.shared.domain.id.VideoId;
import java.util.List;
import java.util.Optional;

/**
 * 视频元数据仓储接口
 * <p>
 * 定义 VideoMetadata 聚合根的持久化操作。
 * </p>
 */
public interface VideoMetadataRepository {

    /**
     * 保存元数据（新增或更新）
     *
     * @param metadata 元数据实体
     * @return 保存后的元数据实体
     */
    VideoMetadata save(VideoMetadata metadata);

    /**
     * 按 ID 查询元数据
     *
     * @param id 元数据 ID
     * @return 可选的元数据实体
     */
    Optional<VideoMetadata> findById(MetadataId id);

    /**
     * 按视频 ID 查询最新元数据
     *
     * @param videoId 视频 ID
     * @return 可选的最新元数据实体
     */
    Optional<VideoMetadata> findLatestByVideoId(VideoId videoId);

    /**
     * 按视频 ID 查询全部历史元数据
     * <p>
     * 用于 LLM 生成时参考用户历史风格
     * </p>
     *
     * @param videoId 视频 ID
     * @return 元数据列表
     */
    List<VideoMetadata> findByVideoId(VideoId videoId);
}
