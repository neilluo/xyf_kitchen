package com.grace.platform.video.application;

import com.grace.platform.shared.ErrorCode;
import com.grace.platform.shared.application.dto.PageResponse;
import com.grace.platform.shared.domain.PageRequest;
import com.grace.platform.shared.domain.PaginatedResult;
import com.grace.platform.shared.domain.id.VideoId;
import com.grace.platform.shared.infrastructure.exception.EntityNotFoundException;
import com.grace.platform.video.application.command.VideoQueryCommand;
import com.grace.platform.video.application.dto.*;
import com.grace.platform.video.domain.Video;
import com.grace.platform.video.domain.VideoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Video 应用服务。
 * <p>
 * 负责视频查询的编排，包括：
 * <ul>
 *   <li>视频列表查询</li>
 *   <li>视频详情查询</li>
 * </ul>
 * <p>
 * 视频上传链路统一由 {@link ServerUploadApplicationService} 处理。
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
@Service
@Transactional
public class VideoApplicationService {

    private final VideoRepository videoRepository;

    /**
     * 构造器注入依赖。
     *
     * @param videoRepository 视频仓储
     */
    public VideoApplicationService(VideoRepository videoRepository) {
        this.videoRepository = videoRepository;
    }

    // ==================== B5: 视频列表查询 ====================

    /**
     * 分页查询视频列表。
     * <p>
     * 支持文件名搜索、状态筛选、日期范围筛选。
     *
     * @param command 查询命令
     * @return 分页视频列表
     */
    @Transactional(readOnly = true)
    public PageResponse<VideoListItemDTO> listVideos(VideoQueryCommand command) {
        PageRequest pageRequest = new PageRequest(command.page(), command.pageSize(), command.sort(), command.order());

        PaginatedResult<Video> result = videoRepository.findByCondition(
            command.keyword(),
            command.statuses(),
            command.startDate(),
            command.endDate(),
            pageRequest
        );

        List<VideoListItemDTO> items = result.items().stream()
            .map(this::toVideoListItemDTO)
            .collect(Collectors.toList());

        return PageResponse.of(items, result.total(), result.page(), result.pageSize());
    }

    // ==================== B6: 视频详情查询 ====================

    /**
     * 获取视频详情。
     * <p>
     * 包含视频基本信息、关联的元数据和发布记录。
     *
     * @param videoId 视频 ID
     * @return 视频详情 DTO
     * @throws EntityNotFoundException 当视频不存在时
     */
    @Transactional(readOnly = true)
    public VideoDetailDTO getVideoDetail(String videoId) {
        VideoId vid = new VideoId(videoId);
        Video video = videoRepository.findById(vid)
            .orElseThrow(() -> new EntityNotFoundException(
                ErrorCode.VIDEO_NOT_FOUND, "Video", videoId));

        VideoDetailDTO.MetadataDTO metadata = null;

        List<VideoDetailDTO.PublishRecordDTO> publishRecords = List.of();

        return new VideoDetailDTO(
            video.getId().value(),
            video.getFileName(),
            video.getFormat().name(),
            video.getFileSize(),
            video.getDuration(),
            video.getStorageUrl() != null ? video.getStorageUrl() : video.getFilePath(),
            video.getStatus(),
            null,
            video.getCreatedAt(),
            video.getUpdatedAt(),
            metadata,
            publishRecords
        );
    }

    // ==================== 软删除视频 ====================

    /**
     * 软删除视频。
     * <p>
     * 将视频状态置为 DELETED，保留数据库记录与 OSS 文件。
     * 已发布（PUBLISHED）的视频不允许删除。
     *
     * @param videoId 视频 ID
     * @throws EntityNotFoundException 当视频不存在时
     */
    public void deleteVideo(String videoId) {
        VideoId vid = new VideoId(videoId);
        Video video = videoRepository.findById(vid)
            .orElseThrow(() -> new EntityNotFoundException(
                ErrorCode.VIDEO_NOT_FOUND, "Video", videoId));

        video.markAsDeleted();
        videoRepository.save(video);
    }

    // ==================== 私有辅助方法 ====================

    private VideoListItemDTO toVideoListItemDTO(Video video) {
        return new VideoListItemDTO(
            video.getId().value(),
            video.getFileName(),
            video.getFormat().name(),
            video.getFileSize(),
            video.getDuration(),
            video.getStatus(),
            null,
            false,
            video.getCreatedAt(),
            video.getUpdatedAt()
        );
    }
}
