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
     * 新增元数据。
     * <p>
     * 调用方在语义上明确是"新建"时使用。若受影响行数不为 1，抛出异常。
     * </p>
     *
     * @param metadata 元数据实体
     * @return 保存后的元数据实体
     */
    VideoMetadata insert(VideoMetadata metadata);

    /**
     * 更新已存在的元数据。
     * <p>
     * 调用方在语义上明确是"更新"时使用。若受影响行数不为 1（通常意味着 id 不存在），
     * 抛出异常以避免静默失败。
     * </p>
     *
     * @param metadata 元数据实体
     * @return 更新后的元数据实体
     */
    VideoMetadata update(VideoMetadata metadata);

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
