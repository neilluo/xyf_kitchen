package com.grace.platform.metadata.infrastructure.persistence;

import com.grace.platform.metadata.domain.VideoMetadata;
import com.grace.platform.metadata.domain.VideoMetadataRepository;
import com.grace.platform.shared.domain.id.MetadataId;
import com.grace.platform.shared.domain.id.VideoId;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * VideoMetadata 仓储实现类。
 * <p>
 * 基于 MyBatis 实现 VideoMetadata 聚合根的持久化操作。
 * MyBatis 直接映射到领域对象，无需 Entity ↔ Domain 转换。
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
@Repository
public class VideoMetadataRepositoryImpl implements VideoMetadataRepository {

    private final VideoMetadataMapper videoMetadataMapper;

    public VideoMetadataRepositoryImpl(VideoMetadataMapper videoMetadataMapper) {
        this.videoMetadataMapper = videoMetadataMapper;
    }

    @Override
    public VideoMetadata save(VideoMetadata metadata) {
        if (metadata.getId() == null) {
            // 新增
            videoMetadataMapper.insert(metadata);
        } else {
            // 更新
            videoMetadataMapper.update(metadata);
        }
        return metadata;
    }

    @Override
    public Optional<VideoMetadata> findById(MetadataId id) {
        VideoMetadata metadata = videoMetadataMapper.findById(id.value());
        return Optional.ofNullable(metadata);
    }

    @Override
    public Optional<VideoMetadata> findLatestByVideoId(VideoId videoId) {
        VideoMetadata metadata = videoMetadataMapper.findByVideoId(videoId.value());
        return Optional.ofNullable(metadata);
    }

    @Override
    public List<VideoMetadata> findByVideoId(VideoId videoId) {
        return videoMetadataMapper.findByVideoIdOrdered(videoId.value());
    }
}
