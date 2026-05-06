package com.grace.platform.video.application;

import com.grace.platform.shared.ErrorCode;
import com.grace.platform.shared.infrastructure.exception.BusinessRuleViolationException;
import com.grace.platform.shared.infrastructure.exception.EntityNotFoundException;
import com.grace.platform.shared.infrastructure.log.TraceContext;
import com.grace.platform.video.application.command.ServerUploadInitCommand;
import com.grace.platform.video.application.dto.ServerChunkUploadDTO;
import com.grace.platform.video.application.dto.ServerUploadCompleteDTO;
import com.grace.platform.video.application.dto.ServerUploadInitDTO;
import com.grace.platform.video.application.event.TaskReadyEvent;
import com.grace.platform.video.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class ServerUploadApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(ServerUploadApplicationService.class);

    private final VideoRepository videoRepository;
    private final UploadSessionRepository uploadSessionRepository;
    private final UploadTaskRepository uploadTaskRepository;
    private final ApplicationEventPublisher eventPublisher;

    private final long defaultChunkSize;
    private final String tempBaseDir;

    public ServerUploadApplicationService(
            VideoRepository videoRepository,
            UploadSessionRepository uploadSessionRepository,
            UploadTaskRepository uploadTaskRepository,
            ApplicationEventPublisher eventPublisher,
            @Value("${grace.upload.chunk-size:16777216}") long defaultChunkSize,
            @Value("${grace.upload.temp-dir:./data/temp}") String tempBaseDir) {
        this.videoRepository = videoRepository;
        this.uploadSessionRepository = uploadSessionRepository;
        this.uploadTaskRepository = uploadTaskRepository;
        this.eventPublisher = eventPublisher;
        this.defaultChunkSize = defaultChunkSize;
        this.tempBaseDir = tempBaseDir;
    }

    public ServerUploadInitDTO initUpload(ServerUploadInitCommand command) {
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
        Path tempDir = Paths.get(tempBaseDir, uploadId);
        
        try {
            Files.createDirectories(tempDir);
            logger.info("Created temp directory for server upload: {}", tempDir);
        } catch (IOException e) {
            logger.error("Failed to create temp directory: {}", tempDir, e);
            throw new BusinessRuleViolationException(
                ErrorCode.INTERNAL_SERVER_ERROR,
                "Failed to create temp directory: " + e.getMessage()
            );
        }

        UploadSession session = UploadSession.createWithId(
            uploadId,
            command.fileName(),
            command.fileSize(),
            command.format(),
            tempDir.toString(),
            defaultChunkSize
        );

        uploadSessionRepository.save(session);

        return new ServerUploadInitDTO(
            session.getUploadId(),
            session.getTotalChunks(),
            defaultChunkSize,
            tempDir.toString(),
            session.getExpiresAt()
        );
    }

    public ServerChunkUploadDTO uploadChunk(String uploadId, int chunkIndex, InputStream chunkData) {
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

        if (!session.isValidChunkIndex(chunkIndex)) {
            throw new BusinessRuleViolationException(
                ErrorCode.CHUNK_INDEX_OUT_OF_RANGE,
                String.format("Chunk index %d is out of range. Valid range: 0-%d", 
                    chunkIndex, session.getTotalChunks() - 1)
            );
        }

        Path tempDir = Paths.get(session.getTempDirectory());
        Path chunkFile = tempDir.resolve("chunk_" + chunkIndex);

        if (Files.exists(chunkFile)) {
            throw new BusinessRuleViolationException(
                ErrorCode.DUPLICATE_CHUNK,
                String.format("Chunk %d has already been uploaded", chunkIndex)
            );
        }

        try {
            Files.copy(chunkData, chunkFile, StandardCopyOption.REPLACE_EXISTING);
            logger.debug("Saved chunk {} to {}", chunkIndex, chunkFile);
        } catch (IOException e) {
            logger.error("Failed to save chunk {} for upload {}", chunkIndex, uploadId, e);
            throw new BusinessRuleViolationException(
                ErrorCode.INTERNAL_SERVER_ERROR,
                "Failed to save chunk: " + e.getMessage()
            );
        }

        session.incrementUploadedChunks();
        uploadSessionRepository.save(session);

        return new ServerChunkUploadDTO(
            session.getUploadId(),
            chunkIndex,
            session.getUploadedChunks(),
            session.getTotalChunks()
        );
    }

    @Transactional
    public ServerUploadCompleteDTO completeUpload(String uploadId) {
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

        if (!session.isUploadComplete()) {
            throw new BusinessRuleViolationException(
                ErrorCode.UPLOAD_NOT_COMPLETE,
                String.format("Upload incomplete: %d of %d chunks uploaded", 
                    session.getUploadedChunks(), session.getTotalChunks())
            );
        }

        // 创建“空壳” video（无 duration / storageUrl，后续 pipeline 步骤补充）
        Video video = Video.createPendingProcessing(session);
        videoRepository.save(video);

        // 构建 payload
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("uploadId", uploadId);
        payload.put("fileName", session.getFileName());
        payload.put("fileSize", session.getFileSize());
        payload.put("format", session.getFormat().name());
        payload.put("tempDirectory", session.getTempDirectory());

        // 构建 extInfo（traceId 便于全链路关联）
        Map<String, Object> ext = new LinkedHashMap<>();
        ext.put("traceId", TraceContext.currentTraceId());
        ext.put("createdTraceId", TraceContext.currentTraceId());

        UploadTask task = UploadTask.createInitial(video.getId().value(), payload, ext);
        uploadTaskRepository.save(task);

        session.markAsCompleted();
        uploadSessionRepository.save(session);

        // 事务提交后触发 push
        eventPublisher.publishEvent(new TaskReadyEvent(task.getId()));

        logger.info("Server upload accepted for video: {}, task: {}", video.getId(), task.getId());

        return new ServerUploadCompleteDTO(
            video.getId().value(),
            task.getId(),
            video.getFileName(),
            video.getFileSize(),
            video.getFormat().name(),
            video.getDuration(),
            video.getStatus(),
            video.getStorageUrl(),
            video.getCreatedAt()
        );
    }

    private String generateUploadId() {
        return "upl_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}