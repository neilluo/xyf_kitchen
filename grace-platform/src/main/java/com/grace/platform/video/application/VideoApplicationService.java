package com.grace.platform.video.application;

import com.grace.platform.shared.ErrorCode;
import com.grace.platform.shared.application.dto.PageResponse;
import com.grace.platform.shared.domain.DomainEventPublisher;
import com.grace.platform.shared.domain.PageRequest;
import com.grace.platform.shared.domain.PaginatedResult;
import com.grace.platform.shared.domain.id.VideoId;
import com.grace.platform.shared.infrastructure.exception.BusinessRuleViolationException;
import com.grace.platform.shared.infrastructure.exception.EntityNotFoundException;
import com.grace.platform.shared.infrastructure.exception.FileOperationException;
import com.grace.platform.video.application.command.UploadInitCommand;
import com.grace.platform.video.application.command.VideoQueryCommand;
import com.grace.platform.video.application.dto.*;
import com.grace.platform.video.domain.*;
import com.grace.platform.video.domain.event.VideoUploadedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Video 应用服务。
 * <p>
 * 负责视频上传流程的编排，包括：
 * <ul>
 *   <li>初始化分片上传会话</li>
 *   <li>接收并存储分片</li>
 *   <li>完成上传并合并分片</li>
 *   <li>查询上传进度</li>
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
    private final ChunkMergeService chunkMergeService;
    private final DomainEventPublisher eventPublisher;

    private final String tempDirBase;
    private final String videoDirBase;
    private final long chunkSize;

    // Track uploaded chunks to detect duplicates
    private final Set<String> uploadedChunkKeys = new HashSet<>();

    /**
     * 构造器注入依赖。
     *
     * @param videoRepository          视频仓储
     * @param uploadSessionRepository  上传会话仓储
     * @param videoFileInspector       视频文件检查器
     * @param chunkMergeService        分片合并服务
     * @param eventPublisher           领域事件发布器
     * @param tempDirBase              临时目录基路径（从配置注入）
     * @param videoDirBase             视频目录基路径（从配置注入）
     * @param chunkSize                分片大小（从配置注入）
     */
    public VideoApplicationService(
            VideoRepository videoRepository,
            UploadSessionRepository uploadSessionRepository,
            VideoFileInspector videoFileInspector,
            ChunkMergeService chunkMergeService,
            DomainEventPublisher eventPublisher,
            @Value("${grace.storage.temp-dir:./data/temp}") String tempDirBase,
            @Value("${grace.storage.video-dir:./data/videos}") String videoDirBase,
            @Value("${grace.upload.chunk-size:16777216}") long chunkSize) {
        this.videoRepository = videoRepository;
        this.uploadSessionRepository = uploadSessionRepository;
        this.videoFileInspector = videoFileInspector;
        this.chunkMergeService = chunkMergeService;
        this.eventPublisher = eventPublisher;
        this.tempDirBase = tempDirBase;
        this.videoDirBase = videoDirBase;
        this.chunkSize = chunkSize;
    }

    // ==================== B1: 初始化上传 ====================

    /**
     * 初始化分片上传。
     * <p>
     * 流程：校验格式+大小 → 计算分片数 → 创建临时目录 → 创建 UploadSession
     *
     * @param command 初始化命令
     * @return 上传初始化 DTO
     * @throws BusinessRuleViolationException 当格式不支持或文件大小超过限制时
     */
    public UploadInitDTO initUpload(UploadInitCommand command) {
        // 验证格式
        if (!Video.SUPPORTED_FORMATS.contains(command.format())) {
            throw new BusinessRuleViolationException(
                ErrorCode.UNSUPPORTED_VIDEO_FORMAT,
                String.format("Unsupported video format: %s. Supported formats: MP4, MOV, AVI, MKV", command.format())
            );
        }

        // 验证文件大小
        if (command.fileSize() > Video.MAX_FILE_SIZE) {
            throw new BusinessRuleViolationException(
                ErrorCode.VIDEO_FILE_SIZE_EXCEEDED,
                String.format("File size %d exceeds maximum allowed size %d bytes (5GB)", 
                    command.fileSize(), Video.MAX_FILE_SIZE)
            );
        }

        // 创建临时目录
        String uploadId = generateUploadId();
        Path tempDir = Paths.get(tempDirBase, uploadId);
        try {
            Files.createDirectories(tempDir);
        } catch (IOException e) {
            throw new FileOperationException("Failed to create temp directory: " + tempDir, e);
        }

        // 创建上传会话
        UploadSession session = UploadSession.createWithId(
            uploadId,
            command.fileName(),
            command.fileSize(),
            command.format(),
            tempDir.toString(),
            chunkSize
        );

        // 保存会话
        uploadSessionRepository.save(session);

        return new UploadInitDTO(
            session.getUploadId(),
            session.getTotalChunks(),
            chunkSize,
            session.getExpiresAt()
        );
    }

    // ==================== B2: 上传分片 ====================

    /**
     * 上传单个分片。
     * <p>
     * 流程：查 Session → 校验索引+重复 → 存分片 → 更新 uploadedChunks
     *
     * @param uploadId    上传会话 ID
     * @param chunkIndex  分片索引
     * @param chunkInput  分片数据流
     * @return 分片上传 DTO
     * @throws EntityNotFoundException        当会话不存在时
     * @throws BusinessRuleViolationException 当会话过期、索引越界或重复上传时
     */
    public ChunkUploadDTO uploadChunk(String uploadId, int chunkIndex, InputStream chunkInput) {
        UploadSession session = uploadSessionRepository.findById(uploadId)
            .orElseThrow(() -> new EntityNotFoundException(
                ErrorCode.UPLOAD_SESSION_NOT_FOUND, "UploadSession", uploadId));

        // 检查会话是否过期
        if (session.isExpired()) {
            throw new BusinessRuleViolationException(
                ErrorCode.UPLOAD_SESSION_EXPIRED,
                "Upload session has expired"
            );
        }

        // 检查会话状态
        if (session.getStatus() != UploadSessionStatus.ACTIVE) {
            throw new BusinessRuleViolationException(
                ErrorCode.UPLOAD_NOT_COMPLETE,
                "Upload session is not active: " + session.getStatus()
            );
        }

        // 检查分片索引范围
        if (!session.isValidChunkIndex(chunkIndex)) {
            throw new BusinessRuleViolationException(
                ErrorCode.CHUNK_INDEX_OUT_OF_RANGE,
                String.format("Chunk index %d is out of range [0, %d)", chunkIndex, session.getTotalChunks())
            );
        }

        // 检查重复分片
        String chunkKey = uploadId + "_" + chunkIndex;
        if (uploadedChunkKeys.contains(chunkKey)) {
            throw new BusinessRuleViolationException(
                ErrorCode.DUPLICATE_CHUNK,
                String.format("Chunk %d has already been uploaded", chunkIndex)
            );
        }

        // 存储分片文件
        Path chunkPath = Paths.get(session.getTempDirectory(), "chunk_" + chunkIndex);
        try {
            Files.copy(chunkInput, chunkPath);
        } catch (IOException e) {
            throw new FileOperationException("Failed to save chunk file: " + chunkPath, e);
        }

        // 更新已上传分片数
        session.incrementUploadedChunks();
        uploadSessionRepository.save(session);

        // 记录已上传
        uploadedChunkKeys.add(chunkKey);

        return new ChunkUploadDTO(
            uploadId,
            chunkIndex,
            session.getUploadedChunks(),
            session.getTotalChunks()
        );
    }

    // ==================== B3: 完成上传 ====================

    /**
     * 完成上传。
     * <p>
     * 流程：查 Session → 校验完整 → 合并分片 → 提取信息 → 保存 Video → 发布 VideoUploadedEvent → 更新 Session 状态
     *
     * @param uploadId 上传会话 ID
     * @return 视频信息 DTO
     * @throws EntityNotFoundException        当会话不存在时
     * @throws BusinessRuleViolationException 当上传未完成时
     */
    public VideoInfoDTO completeUpload(String uploadId) {
        UploadSession session = uploadSessionRepository.findById(uploadId)
            .orElseThrow(() -> new EntityNotFoundException(
                ErrorCode.UPLOAD_SESSION_NOT_FOUND, "UploadSession", uploadId));

        // 检查会话是否过期
        if (session.isExpired()) {
            throw new BusinessRuleViolationException(
                ErrorCode.UPLOAD_SESSION_EXPIRED,
                "Upload session has expired"
            );
        }

        // 校验是否全部上传完成
        if (!session.isUploadComplete()) {
            throw new BusinessRuleViolationException(
                ErrorCode.UPLOAD_NOT_COMPLETE,
                String.format("Upload incomplete: %d of %d chunks uploaded", 
                    session.getUploadedChunks(), session.getTotalChunks())
            );
        }

        // 合并分片
        Path tempDir = Paths.get(session.getTempDirectory());
        String targetFileName = session.getUploadId() + "." + session.getFormat().name().toLowerCase();
        Path mergedFilePath = chunkMergeService.mergeChunks(tempDir, session.getTotalChunks(), targetFileName);

        // 移动最终文件到视频目录
        Path videoDir = Paths.get(videoDirBase);
        Path finalFilePath;
        try {
            Files.createDirectories(videoDir);
            String finalFileName = generateVideoFileName(session.getUploadId(), session.getFormat());
            finalFilePath = videoDir.resolve(finalFileName);
            Files.move(mergedFilePath, finalFilePath);
        } catch (IOException e) {
            throw new FileOperationException("Failed to move video file to final location", e);
        }

        // 提取视频信息
        VideoFileInfo fileInfo = videoFileInspector.inspect(finalFilePath);

        // 创建 Video 聚合根
        Video video = Video.create(
            session.getFileName(),
            session.getFileSize(),
            session.getFormat(),
            fileInfo.duration(),
            finalFilePath.toString()
        );

        // 保存 Video
        videoRepository.save(video);

        // 标记会话为已完成
        session.markAsCompleted();
        uploadSessionRepository.save(session);

        // 发布 VideoUploadedEvent
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
     * 流程：查 Session → 计算进度百分比
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

        return new UploadProgressDTO(
            uploadId,
            session.getUploadedChunks(),
            session.getTotalChunks(),
            session.getProgressPercentage(),
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

        // TODO: 从 Metadata 上下文获取元数据（通过查询或防腐层）
        VideoDetailDTO.MetadataDTO metadata = null;

        // TODO: 从 Distribution 上下文获取发布记录
        List<VideoDetailDTO.PublishRecordDTO> publishRecords = List.of();

        return new VideoDetailDTO(
            video.getId().value(),
            video.getFileName(),
            video.getFormat().name(),
            video.getFileSize(),
            video.getDuration(),
            video.getFilePath(),
            video.getStatus(),
            null, // thumbnailUrl - TODO
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
            null, // thumbnailUrl - TODO
            false, // hasMetadata - TODO
            video.getCreatedAt(),
            video.getUpdatedAt()
        );
    }

    private String generateUploadId() {
        return "upl_" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private String generateVideoFileName(String uploadId, VideoFormat format) {
        String videoId = "vid_" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        return videoId + "." + format.name().toLowerCase();
    }
}
