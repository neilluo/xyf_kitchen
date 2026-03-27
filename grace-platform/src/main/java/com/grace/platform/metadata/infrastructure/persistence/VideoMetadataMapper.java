package com.grace.platform.metadata.infrastructure.persistence;

import com.grace.platform.metadata.domain.VideoMetadata;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface VideoMetadataMapper {

    VideoMetadata findById(@Param("id") String id);

    VideoMetadata findByVideoId(@Param("videoId") String videoId);

    List<VideoMetadata> findByVideoIdOrdered(@Param("videoId") String videoId);

    void insert(VideoMetadata metadata);

    void update(VideoMetadata metadata);
}
