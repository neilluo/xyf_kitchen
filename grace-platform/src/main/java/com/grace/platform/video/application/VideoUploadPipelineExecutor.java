package com.grace.platform.video.application;

import com.grace.platform.metadata.application.MetadataApplicationService;
import com.grace.platform.metadata.domain.VideoMetadataRepository;
import com.grace.platform.shared.domain.id.VideoId;
import com.grace.platform.storage.domain.OssStorageService;
import com.grace.platform.storage.domain.StorageProvider;
import com.grace.platform.video.application.event.TaskReadyEvent;
import com.grace.platform.video.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 视频上传流水线执行器。
 * <p>
 * 每次调用只推进一步，通过 {@link TaskReadyEvent} 实现链式推进。
 * 5 个大步骤：MERGE → INSPECT → OSS → METADATA → CLEANUP。
 * </p>
 */
@Service
public class VideoUploadPipelineExecutor {

    private static final Logger logger = LoggerFactory.getLogger(VideoUploadPipelineExecutor.class);

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final UploadTaskRepository taskRepository;
    private final VideoRepository videoRepository;
    private final UploadSessionRepository uploadSessionRepository;
    private final VideoMetadataRepository videoMetadataRepository;
    private final ChunkMergeService chunkMergeService;
    private final VideoFileInspector videoFileInspector;
    private final OssStorageService ossStorageService;
    private final MetadataApplicationService metadataApplicationService;
    private final ApplicationEventPublisher eventPublisher;
    private final String instanceId;

    public VideoUploadPipelineExecutor(
            UploadTaskRepository taskRepository,
            VideoRepository videoRepository,
            UploadSessionRepository uploadSessionRepository,
            VideoMetadataRepository videoMetadataRepository,
            ChunkMergeService chunkMergeService,
            VideoFileInspector videoFileInspector,
            OssStorageService ossStorageService,
            MetadataApplicationService metadataApplicationService,
            ApplicationEventPublisher eventPublisher,
            @Value("${grace.upload-task.instance-id}") String instanceId) {
        this.taskRepository = taskRepository;
        this.videoRepository = videoRepository;
        this.uploadSessionRepository = uploadSessionRepository;
        this.videoMetadataRepository = videoMetadataRepository;
        this.chunkMergeService = chunkMergeService;
        this.videoFileInspector = videoFileInspector;
        this.ossStorageService = ossStorageService;
        this.metadataApplicationService = metadataApplicationService;
        this.eventPublisher = eventPublisher;
        this.instanceId = instanceId;
    }

    /**
     * 每次调用只推进一步。
     * <p>流程：claim → markRunning → 执行对应步骤 → 成功后若还有下一步则发布 TaskReadyEvent。</p>
     *
     * @param taskId 任务 ID
     */
    @Transactional(propagation = Propagation.NEVER)
    public void runOnce(String taskId) {
        int claimed = taskRepository.claim(taskId, instanceId);
        if (claimed == 0) {
            logger.debug("Task {} not claimed by instance {}", taskId, instanceId);
            return;
        }

        UploadTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalStateException("Task not found after claim: " + taskId));

        task.markRunning(instanceId);
        taskRepository.save(task);

        try {
            switch (task.getCurrentStep()) {
                case RUNNING_MERGE -> doMerge(task);
                case RUNNING_INSPECT -> doInspect(task);
                case RUNNING_OSS -> doOss(task);
                case RUNNING_METADATA -> doMetadata(task);
                case RUNNING_CLEANUP -> doCleanup(task);
                default -> throw new IllegalStateException("Unexpected step: " + task.getCurrentStep());
            }

            UploadTask refreshed = taskRepository.findById(taskId).orElseThrow();
            if (refreshed.getStatus() == UploadTaskStatus.PENDING) {
                eventPublisher.publishEvent(new TaskReadyEvent(taskId));
            }
        } catch (Exception e) {
            failAndPersist(task, e);
        }
    }

    private void doMerge(UploadTask task) {
        String uploadId = (String) task.getPayload().get("uploadId");
        if (uploadId == null) {
            throw new IllegalStateException("uploadId not found in task payload");
        }

        UploadSession session = uploadSessionRepository.findById(uploadId)
                .orElseThrow(() -> new IllegalStateException("UploadSession not found: " + uploadId));

        Path tempDir = Paths.get(session.getTempDirectory());
        Path mergedFile = tempDir.resolve(session.getFileName());

        // 幂等检查：已存在且大小匹配则跳过合并
        if (Files.exists(mergedFile) && Files.isRegularFile(mergedFile)) {
            try {
                long existingSize = Files.size(mergedFile);
                if (existingSize == session.getFileSize()) {
                    logger.info("Merged file already exists with matching size, skipping merge: {}", mergedFile);
                    task.getPayload().put("mergedFilePath", mergedFile.toString());
                    task.advanceToNextPending();
                    taskRepository.save(task);
                    return;
                }
            } catch (Exception e) {
                logger.warn("Failed to check existing merged file size, will re-merge: {}", mergedFile, e);
            }
        }

        Path result = chunkMergeService.mergeChunks(tempDir, session.getTotalChunks(), session.getFileName());
        task.getPayload().put("mergedFilePath", result.toString());
        task.advanceToNextPending();
        taskRepository.save(task);
        logger.info("Task {} merge completed: {}", task.getId(), result);
    }

    private void doInspect(UploadTask task) {
        String mergedFilePath = (String) task.getPayload().get("mergedFilePath");
        if (mergedFilePath == null) {
            throw new IllegalStateException("mergedFilePath not found in task payload");
        }

        VideoFileInfo fileInfo = videoFileInspector.inspect(Paths.get(mergedFilePath));

        VideoId videoId = new VideoId(task.getVideoId());
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalStateException("Video not found: " + task.getVideoId()));

        video.updateInfo(null, fileInfo.duration());
        videoRepository.save(video);

        task.advanceToNextPending();
        taskRepository.save(task);
        logger.info("Task {} inspect completed, duration={}", task.getId(), fileInfo.duration());
    }

    private void doOss(UploadTask task) {
        VideoId videoId = new VideoId(task.getVideoId());
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new IllegalStateException("Video not found: " + task.getVideoId()));

        // 幂等：已存在 storageUrl 则跳过
        if (video.getStorageUrl() != null && !video.getStorageUrl().isBlank()) {
            logger.info("Video {} already has storageUrl, skipping OSS upload", videoId);
            task.advanceToNextPending();
            taskRepository.save(task);
            return;
        }

        String mergedFilePath = (String) task.getPayload().get("mergedFilePath");
        if (mergedFilePath == null) {
            throw new IllegalStateException("mergedFilePath not found in task payload");
        }

        String uploadId = (String) task.getPayload().get("uploadId");
        UploadSession session = uploadSessionRepository.findById(uploadId)
                .orElseThrow(() -> new IllegalStateException("UploadSession not found: " + uploadId));

        String storageKey = generateStorageKey(session);
        ossStorageService.uploadFile(Paths.get(mergedFilePath), storageKey);
        String storageUrl = ossStorageService.buildObjectUrl(storageKey);

        video.attachStorage(storageUrl, StorageProvider.OSS);
        videoRepository.save(video);

        task.advanceToNextPending();
        taskRepository.save(task);
        logger.info("Task {} OSS upload completed: {}", task.getId(), storageUrl);
    }

    private void doMetadata(UploadTask task) {
        VideoId videoId = new VideoId(task.getVideoId());

        // 幂等：已存在元数据则跳过
        if (videoMetadataRepository.findLatestByVideoId(videoId).isPresent()) {
            logger.info("Video {} already has metadata, skipping generation", videoId);
            task.advanceToNextPending();
            taskRepository.save(task);
            return;
        }

        ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "task-heartbeat-" + task.getId());
            t.setDaemon(true);
            return t;
        });

        try {
            heartbeatExecutor.scheduleAtFixedRate(
                    () -> taskRepository.touchHeartbeat(task.getId()),
                    30, 30, TimeUnit.SECONDS);

            metadataApplicationService.generateMetadata(videoId);

            task.advanceToNextPending();
            taskRepository.save(task);
            logger.info("Task {} metadata generation completed", task.getId());
        } finally {
            heartbeatExecutor.shutdown();
            try {
                if (!heartbeatExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    heartbeatExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                heartbeatExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private void doCleanup(UploadTask task) {
        String mergedFilePath = (String) task.getPayload().get("mergedFilePath");
        String uploadId = (String) task.getPayload().get("uploadId");

        if (mergedFilePath != null) {
            try {
                Path mergedFile = Paths.get(mergedFilePath);
                Files.deleteIfExists(mergedFile);
                logger.debug("Deleted merged file: {}", mergedFile);
            } catch (Exception e) {
                logger.warn("Failed to delete merged file: {}", mergedFilePath, e);
            }
        }

        if (uploadId != null) {
            uploadSessionRepository.findById(uploadId).ifPresent(session -> {
                String tempDirStr = session.getTempDirectory();
                if (tempDirStr != null) {
                    try {
                        Path tempDir = Paths.get(tempDirStr);
                        if (Files.exists(tempDir)) {
                            Files.walk(tempDir)
                                    .sorted((a, b) -> b.compareTo(a))
                                    .forEach(path -> {
                                        try {
                                            Files.deleteIfExists(path);
                                        } catch (Exception e) {
                                            logger.warn("Failed to delete temp path: {}", path, e);
                                        }
                                    });
                            logger.debug("Cleaned up temp directory: {}", tempDir);
                        }
                    } catch (Exception e) {
                        logger.warn("Failed to cleanup temp directory: {}", tempDirStr, e);
                    }
                }
            });
        }

        task.advanceToNextPending();
        taskRepository.save(task);
        logger.info("Task {} cleanup completed", task.getId());
    }

    private void failAndPersist(UploadTask task, Exception e) {
        logger.error("Task {} failed at step {}", task.getId(), task.getCurrentStep(), e);
        task.markFailed(e);
        taskRepository.save(task);

        if (task.getStatus() == UploadTaskStatus.FAILED && task.getCurrentStep() == UploadTaskStep.RUNNING_METADATA) {
            try {
                VideoId videoId = new VideoId(task.getVideoId());
                Video video = videoRepository.findById(videoId).orElse(null);
                if (video != null) {
                    video.transitionTo(VideoStatus.METADATA_FAILED);
                    videoRepository.save(video);
                }
            } catch (Exception ex) {
                logger.error("Failed to transition video to METADATA_FAILED: {}", task.getVideoId(), ex);
            }
        }
    }

    private String generateStorageKey(UploadSession session) {
        String datePrefix = LocalDateTime.now().format(DATE_FORMATTER);
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String extension = getFileExtension(session.getFileName());
        return String.format("videos/%s/%s_%s.%s", datePrefix, session.getUploadId(), uuid, extension);
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "mp4";
        }
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot + 1).toLowerCase();
        }
        return "mp4";
    }
}
