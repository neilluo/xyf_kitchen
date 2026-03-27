package com.grace.platform.metadata.application;

import com.grace.platform.metadata.application.command.UpdateMetadataCommand;
import com.grace.platform.metadata.application.dto.VideoMetadataDTO;
import com.grace.platform.metadata.domain.MetadataGenerationService;
import com.grace.platform.metadata.domain.MetadataSource;
import com.grace.platform.metadata.domain.VideoMetadata;
import com.grace.platform.metadata.domain.VideoMetadataRepository;
import com.grace.platform.metadata.domain.event.MetadataConfirmedEvent;
import com.grace.platform.shared.ErrorCode;
import com.grace.platform.shared.domain.DomainEventPublisher;
import com.grace.platform.shared.domain.id.MetadataId;
import com.grace.platform.shared.domain.id.VideoId;
import com.grace.platform.shared.infrastructure.exception.EntityNotFoundException;
import com.grace.platform.video.domain.Video;
import com.grace.platform.video.domain.VideoFileInfo;
import com.grace.platform.video.domain.VideoFormat;
import com.grace.platform.video.domain.VideoRepository;
import com.grace.platform.video.domain.VideoStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

/**
 * 元数据应用服务
 * <p>
 * 负责编排元数据相关的用例流程，包括：
 * <ul>
 *   <li>自动生成元数据（响应 VideoUploadedEvent）</li>
 *   <li>用户编辑元数据</li>
 *   <li>重新生成元数据</li>
 *   <li>确认元数据（触发状态流转和领域事件）</li>
 *   <li>查询元数据</li>
 * </ul>
 * </p>
 */
@Service
@Transactional
public class MetadataApplicationService {

    private final VideoMetadataRepository videoMetadataRepository;
    private final VideoRepository videoRepository;
    private final MetadataGenerationService metadataGenerationService;
    private final DomainEventPublisher eventPublisher;

    /**
     * 创建元数据应用服务
     *
     * @param videoMetadataRepository    元数据仓储
     * @param videoRepository            视频仓储
     * @param metadataGenerationService  元数据生成服务
     * @param eventPublisher             领域事件发布器
     */
    public MetadataApplicationService(
            VideoMetadataRepository videoMetadataRepository,
            VideoRepository videoRepository,
            MetadataGenerationService metadataGenerationService,
            DomainEventPublisher eventPublisher) {
        this.videoMetadataRepository = videoMetadataRepository;
        this.videoRepository = videoRepository;
        this.metadataGenerationService = metadataGenerationService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 为视频自动生成元数据
     * <p>
     * 流程：
     * 1. 查询视频信息
     * 2. 查询该视频的历史元数据（用于风格参考）
     * 3. 调用 LLM 生成元数据
     * 4. 保存元数据
     * 5. 更新视频状态为 METADATA_GENERATED
     * </p>
     *
     * @param videoId 视频 ID
     * @return 生成的元数据 DTO
     * @throws EntityNotFoundException 当视频不存在时
     */
    public VideoMetadataDTO generateMetadata(VideoId videoId) {
        // 1. 查询视频
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new EntityNotFoundException(
                        ErrorCode.VIDEO_NOT_FOUND,
                        "Video",
                        videoId.value()
                ));

        // 2. 查询历史元数据
        List<VideoMetadata> historicalMetadata = videoMetadataRepository.findByVideoId(videoId);

        // 3. 构建 VideoFileInfo
        VideoFileInfo videoFileInfo = new VideoFileInfo(
                video.getFileName(),
                video.getFileSize(),
                video.getFormat(),
                video.getDuration()
        );

        // 4. 调用 LLM 生成元数据
        VideoMetadata metadata = metadataGenerationService.generate(videoFileInfo, historicalMetadata);

        // 5. 保存元数据
        metadata = videoMetadataRepository.save(metadata);

        // 6. 更新视频状态
        video.transitionTo(VideoStatus.METADATA_GENERATED);
        videoRepository.save(video);

        return toDto(metadata);
    }

    /**
     * 更新元数据
     * <p>
     * 用户编辑元数据字段。已确认的元数据不可编辑。
     * </p>
     *
     * @param metadataId 元数据 ID
     * @param command    更新命令
     * @return 更新后的元数据 DTO
     * @throws EntityNotFoundException 当元数据不存在时
     */
    public VideoMetadataDTO updateMetadata(MetadataId metadataId, UpdateMetadataCommand command) {
        VideoMetadata metadata = videoMetadataRepository.findById(metadataId)
                .orElseThrow(() -> new EntityNotFoundException(
                        ErrorCode.METADATA_NOT_FOUND,
                        "VideoMetadata",
                        metadataId.value()
                ));

        metadata.update(command.title(), command.description(), command.tags());
        metadata = videoMetadataRepository.save(metadata);

        return toDto(metadata);
    }

    /**
     * 重新生成元数据
     * <p>
     * 对已存在的元数据重新调用 LLM 生成，覆盖原有内容。
     * 已确认的元数据不可重新生成。
     * </p>
     *
     * @param metadataId 元数据 ID
     * @return 重新生成的元数据 DTO
     * @throws EntityNotFoundException 当元数据不存在时
     */
    public VideoMetadataDTO regenerateMetadata(MetadataId metadataId) {
        // 1. 查询现有元数据
        VideoMetadata existingMetadata = videoMetadataRepository.findById(metadataId)
                .orElseThrow(() -> new EntityNotFoundException(
                        ErrorCode.METADATA_NOT_FOUND,
                        "VideoMetadata",
                        metadataId.value()
                ));

        // 2. 获取关联的视频
        VideoId videoId = existingMetadata.getVideoId();
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new EntityNotFoundException(
                        ErrorCode.VIDEO_NOT_FOUND,
                        "Video",
                        videoId.value()
                ));

        // 3. 查询历史元数据
        List<VideoMetadata> historicalMetadata = videoMetadataRepository.findByVideoId(videoId);

        // 4. 构建 VideoFileInfo
        VideoFileInfo videoFileInfo = new VideoFileInfo(
                video.getFileName(),
                video.getFileSize(),
                video.getFormat(),
                video.getDuration()
        );

        // 5. 调用 LLM 重新生成
        VideoMetadata newMetadata = metadataGenerationService.generate(videoFileInfo, historicalMetadata);

        // 6. 创建新的元数据实体（替换原有）
        VideoMetadata regeneratedMetadata = VideoMetadata.create(
                videoId,
                newMetadata.getTitle(),
                newMetadata.getDescription(),
                newMetadata.getTags(),
                MetadataSource.AI_GENERATED
        );

        // 7. 保存新元数据
        regeneratedMetadata = videoMetadataRepository.save(regeneratedMetadata);

        return toDto(regeneratedMetadata);
    }

    /**
     * 确认元数据
     * <p>
     * 流程：
     * 1. 确认元数据（设 confirmed=true）
     * 2. 更新视频状态为 READY_TO_PUBLISH
     * 3. 发布 MetadataConfirmedEvent
     * </p>
     *
     * @param metadataId 元数据 ID
     * @return 确认后的元数据 DTO
     * @throws EntityNotFoundException 当元数据不存在时
     */
    public VideoMetadataDTO confirmMetadata(MetadataId metadataId) {
        // 1. 查询元数据
        VideoMetadata metadata = videoMetadataRepository.findById(metadataId)
                .orElseThrow(() -> new EntityNotFoundException(
                        ErrorCode.METADATA_NOT_FOUND,
                        "VideoMetadata",
                        metadataId.value()
                ));

        // 2. 确认元数据
        metadata.confirm();
        metadata = videoMetadataRepository.save(metadata);

        // 3. 更新视频状态
        VideoId videoId = metadata.getVideoId();
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new EntityNotFoundException(
                        ErrorCode.VIDEO_NOT_FOUND,
                        "Video",
                        videoId.value()
                ));
        video.transitionTo(VideoStatus.READY_TO_PUBLISH);
        videoRepository.save(video);

        // 4. 发布领域事件
        MetadataConfirmedEvent event = new MetadataConfirmedEvent(videoId, metadataId);
        eventPublisher.publish(event);

        return toDto(metadata);
    }

    /**
     * 根据视频 ID 获取最新元数据
     *
     * @param videoId 视频 ID
     * @return 最新元数据 DTO，如果不存在则返回 null
     */
    @Transactional(readOnly = true)
    public VideoMetadataDTO getMetadataByVideoId(VideoId videoId) {
        return videoMetadataRepository.findLatestByVideoId(videoId)
                .map(this::toDto)
                .orElse(null);
    }

    /**
     * 将领域实体转换为 DTO
     *
     * @param metadata 元数据实体
     * @return 元数据 DTO
     */
    private VideoMetadataDTO toDto(VideoMetadata metadata) {
        return new VideoMetadataDTO(
                metadata.getId().value(),
                metadata.getVideoId().value(),
                metadata.getTitle(),
                metadata.getDescription(),
                metadata.getTags(),
                metadata.getSource(),
                metadata.isConfirmed(),
                metadata.getCreatedAt(),
                metadata.getUpdatedAt()
        );
    }
}
