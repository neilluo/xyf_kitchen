package com.grace.platform.video.application;

import com.grace.platform.shared.ErrorCode;
import com.grace.platform.shared.domain.DomainEventPublisher;
import com.grace.platform.shared.domain.id.VideoId;
import com.grace.platform.shared.infrastructure.exception.BusinessRuleViolationException;
import com.grace.platform.shared.infrastructure.exception.EntityNotFoundException;
import com.grace.platform.storage.domain.StorageProvider;
import com.grace.platform.video.application.command.ServerUploadInitCommand;
import com.grace.platform.video.application.dto.ServerChunkUploadDTO;
import com.grace.platform.video.application.dto.ServerUploadCompleteDTO;
import com.grace.platform.video.application.dto.ServerUploadInitDTO;
import com.grace.platform.video.domain.*;
import com.grace.platform.video.domain.event.VideoUploadedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
public class ServerUploadApplicationService {

    private static final Logger logger = LoggerFactory.getLogger(ServerUploadApplicationService.class);

    private final VideoRepository videoRepository;
    private final UploadSessionRepository uploadSessionRepository;
    private final ChunkMergeService chunkMergeService;
    private final VideoFileInspector videoFileInspector;
    private final DomainEventPublisher eventPublisher;

    private final long defaultChunkSize;
    private final String tempBaseDir;

    public ServerUploadApplicationService(
            VideoRepository videoRepository,
            UploadSessionRepository uploadSessionRepository,
            ChunkMergeService chunkMergeService,
            VideoFileInspector videoFileInspector,
            DomainEventPublisher eventPublisher,
            @Value("${grace.upload.chunk-size:16777216}") long defaultChunkSize,
            @Value("${grace.upload.temp-dir:./data/temp}") String tempBaseDir) {
        this.videoRepository = videoRepository;
        this.uploadSessionRepository = uploadSessionRepository;
        this.chunkMergeService = chunkMergeService;
        this.videoFileInspector = videoFileInspector;
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

        Path tempDir = Paths.get(session.getTempDirectory());
        Path mergedFile = chunkMergeService.mergeChunks(
            tempDir, 
            session.getTotalChunks(), 
            session.getFileName()
        );

        VideoFileInfo fileInfo = videoFileInspector.inspect(mergedFile);

        Video video = Video.createWithStorageUrl(
            session.getFileName(),
            session.getFileSize(),
            session.getFormat(),
            fileInfo.duration(),
            null,
            StorageProvider.LOCAL
        );

        videoRepository.save(video);

        session.markAsCompleted();
        uploadSessionRepository.save(session);

        cleanupTempDirectory(tempDir, mergedFile);

        VideoUploadedEvent event = new VideoUploadedEvent(
            video.getId(),
            video.getFileName(),
            video.getFileSize(),
            video.getFormat().name()
        );
        eventPublisher.publish(event);

        logger.info("Server upload completed for video: {}", video.getId());

        return new ServerUploadCompleteDTO(
            video.getId().value(),
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

    private void cleanupTempDirectory(Path tempDir, Path mergedFile) {
        try {
            if (Files.exists(mergedFile)) {
                Files.deleteIfExists(mergedFile);
                logger.debug("Deleted merged file: {}", mergedFile);
            }
            
            if (Files.exists(tempDir)) {
                Files.walk(tempDir)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            logger.warn("Failed to delete temp path: {}", path, e);
                        }
                    });
                logger.debug("Cleaned up temp directory: {}", tempDir);
            }
        } catch (IOException e) {
            logger.warn("Failed to cleanup temp directory: {}", tempDir, e);
        }
    }
}