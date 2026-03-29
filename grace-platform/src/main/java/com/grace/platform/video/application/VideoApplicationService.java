package com.grace.platform.video.application;

import com.grace.platform.shared.ErrorCode;
import com.grace.platform.shared.application.dto.PageResponse;
import com.grace.platform.shared.domain.DomainEventPublisher;
import com.grace.platform.shared.domain.PageRequest;
import com.grace.platform.shared.domain.PaginatedResult;
import com.grace.platform.shared.domain.id.VideoId;
import com.grace.platform.shared.infrastructure.exception.BusinessRuleViolationException;
import com.grace.platform.shared.infrastructure.exception.EntityNotFoundException;
import com.grace.platform.storage.domain.OssStorageService;
import com.grace.platform.storage.domain.StsCredentials;
import com.grace.platform.storage.domain.StorageProvider;
import com.grace.platform.storage.domain.UploadCallback;
import com.grace.platform.video.application.command.UploadInitCommand;
import com.grace.platform.video.application.command.VideoQueryCommand;
import com.grace.platform.video.application.dto.*;
import com.grace.platform.video.domain.*;
import com.grace.platform.video.domain.event.VideoUploadedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Video 应用服务。
 * <p>
 * 负责视频上传流程的编排，包括：
 * <ul>
 *   <li>初始化 OSS 上传会话，返回 STS 凭证</li>
 *   <li>接收 OSS 上传完成回调</li>
 *   <li>查询上传进度或回调状态</li>
 *   <li>视频列表查询</li>
 *   <li>视频详情查询</li>
 * </ul>
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
@Service
@Transactional
public class VideoApplicationService {

    private final VideoRepository videoRepository;
    private final UploadSessionRepository uploadSessionRepository;
    private final VideoFileInspector videoFileInspector;
    private final OssStorageService ossStorageService;
    private final DomainEventPublisher eventPublisher;

    private final long defaultChunkSize;
    private final long stsDurationSeconds;

    /**
     * 构造器注入依赖。
     *
     * @param videoRepository          视频仓储
     * @param uploadSessionRepository  上传会话仓储
     * @param videoFileInspector       视频文件检查器
     * @param ossStorageService        OSS 存储服务
     * @param eventPublisher           领域事件发布器
     * @param defaultChunkSize         默认分片大小（从配置注入）
     * @param stsDurationSeconds       STS 凭证有效期（从配置注入）
     */
    public VideoApplicationService(
            VideoRepository videoRepository,
            UploadSessionRepository uploadSessionRepository,
            VideoFileInspector videoFileInspector,
            OssStorageService ossStorageService,
            DomainEventPublisher eventPublisher,
            @Value("${grace.upload.chunk-size:16777216}") long defaultChunkSize,
            @Value("${grace.oss.sts.duration-seconds:3600}") long stsDurationSeconds) {
        this.videoRepository = videoRepository;
        this.uploadSessionRepository = uploadSessionRepository;
        this.videoFileInspector = videoFileInspector;
        this.ossStorageService = ossStorageService;
        this.eventPublisher = eventPublisher;
        this.defaultChunkSize = defaultChunkSize;
        this.stsDurationSeconds = stsDurationSeconds;
    }

    // ==================== B1: 初始化上传 ====================

    /**
     * 初始化 OSS 直传上传。
     * <p>
     * 流程：校验格式+大小 → 生成 storageKey → 创建 UploadSession → 生成 STS 凭证
     *
     * @param command 初始化命令
     * @return 上传初始化 DTO（包含 STS 凭证）
     * @throws BusinessRuleViolationException 当格式不支持或文件大小超过限制时
     */
    public UploadInitDTO initUpload(UploadInitCommand command) {
        if (!Video.SUPPORTED_FORMATS.contains(command.format())) {
            throw new BusinessRuleViolationException(
                ErrorCode.UNSUPPORTED_VIDEO_FORMAT,
                String.format("Unsupported video format: %s. Supported formats: MP4, MOV, AVI, MKV", command.format())
            );
        }

        if (command.fileSize() > Video.MAX_FILE_SIZE) {
            throw new BusinessRuleViolationException(
                ErrorCode.VIDEO_FILE_SIZE_EXCEEDED,
                String.format("File size %d exceeds maximum allowed size %d bytes (5GB)", 
                    command.fileSize(), Video.MAX_FILE_SIZE)
            );
        }

        String uploadId = generateUploadId();
        String storageKey = generateStorageKey(uploadId, command.fileName());
        String bucket = ossStorageService.getBucketName();

        UploadSession session = UploadSession.createWithId(
            uploadId,
            command.fileName(),
            command.fileSize(),
            command.format(),
            storageKey,
            bucket,
            defaultChunkSize
        );

        uploadSessionRepository.save(session);

        StsCredentials stsCredentials = ossStorageService.generateStsCredentials(
            bucket,
            storageKey,
            stsDurationSeconds
        );

        return new UploadInitDTO(
            session.getUploadId(),
            session.getTotalChunks(),
            defaultChunkSize,
            session.getExpiresAt(),
            storageKey,
            bucket,
            stsCredentials
        );
    }

    // ==================== B3: 完成上传（OSS 回调） ====================

    /**
     * 完成 OSS 上传。
     * <p>
     * 流程：验证回调 → 查 Session → 提取信息 → 保存 Video → 发布 VideoUploadedEvent
     * 由 OSS 上传完成回调触发，不再合并分片。
     *
     * @param callback OSS 上传回调数据
     * @return 视频信息 DTO
     * @throws EntityNotFoundException        当会话不存在时
     * @throws BusinessRuleViolationException 当会话过期或状态异常时
     */
    public VideoInfoDTO completeUpload(UploadCallback callback) {
        String uploadId = extractUploadIdFromStorageKey(callback.objectKey());
        
        UploadSession session = uploadSessionRepository.findById(uploadId)
            .orElseThrow(() -> new EntityNotFoundException(
                ErrorCode.UPLOAD_SESSION_NOT_FOUND, "UploadSession", uploadId));

        if (session.isExpired()) {
            throw new BusinessRuleViolationException(
                ErrorCode.UPLOAD_SESSION_EXPIRED,
                "Upload session has expired"
            );
        }

        if (session.getStatus() != UploadSessionStatus.ACTIVE) {
            throw new BusinessRuleViolationException(
                ErrorCode.UPLOAD_NOT_COMPLETE,
                "Upload session is not active: " + session.getStatus()
            );
        }

        String storageUrl = ossStorageService.buildObjectUrl(callback.objectKey());

        VideoFileInfo fileInfo = videoFileInspector.inspectFromUrl(storageUrl);

        Video video = Video.createWithStorageUrl(
            session.getFileName(),
            session.getFileSize(),
            session.getFormat(),
            fileInfo.duration(),
            storageUrl,
            StorageProvider.OSS
        );

        videoRepository.save(video);

        session.markAsCompletedByOssCallback();
        uploadSessionRepository.save(session);

        VideoUploadedEvent event = new VideoUploadedEvent(
            video.getId(),
            video.getFileName(),
            video.getFileSize(),
            video.getFormat().name()
        );
        eventPublisher.publish(event);

        return new VideoInfoDTO(
            video.getId().value(),
            video.getFileName(),
            video.getFileSize(),
            video.getFormat().name(),
            video.getDuration(),
            video.getStatus(),
            video.getCreatedAt()
        );
    }

    // ==================== B4: 查询上传进度 ====================

    /**
     * 查询上传进度。
     * <p>
     * 查 Session → 返回状态（前端通过 OSS SDK 获取实际上传进度）
     *
     * @param uploadId 上传会话 ID
     * @return 上传进度 DTO
     * @throws EntityNotFoundException 当会话不存在时
     */
    @Transactional(readOnly = true)
    public UploadProgressDTO getUploadProgress(String uploadId) {
        UploadSession session = uploadSessionRepository.findById(uploadId)
            .orElseThrow(() -> new EntityNotFoundException(
                ErrorCode.UPLOAD_SESSION_NOT_FOUND, "UploadSession", uploadId));

        int progressPercent = 0;
        if (session.getStatus() == UploadSessionStatus.COMPLETED) {
            progressPercent = 100;
        } else if (session.getStatus() == UploadSessionStatus.ACTIVE) {
            progressPercent = -1;
        }

        return new UploadProgressDTO(
            uploadId,
            session.getUploadedChunks(),
            session.getTotalChunks(),
            progressPercent,
            session.getStatus()
        );
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

    private String generateUploadId() {
        return "upl_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private String generateStorageKey(String uploadId, String fileName) {
        String sanitizedFileName = fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        return "videos/" + uploadId + "/" + sanitizedFileName;
    }

    private String extractUploadIdFromStorageKey(String storageKey) {
        if (storageKey == null || !storageKey.startsWith("videos/")) {
            throw new BusinessRuleViolationException(
                ErrorCode.INTERNAL_SERVER_ERROR,
                "Invalid storageKey format: " + storageKey
            );
        }
        String[] parts = storageKey.split("/");
        if (parts.length < 3) {
            throw new BusinessRuleViolationException(
                ErrorCode.INTERNAL_SERVER_ERROR,
                "Invalid storageKey format: " + storageKey
            );
        }
        return parts[1];
    }
}