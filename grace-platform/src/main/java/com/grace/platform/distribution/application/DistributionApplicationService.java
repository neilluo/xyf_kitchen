package com.grace.platform.distribution.application;

import com.grace.platform.distribution.application.command.PublishCommand;
import com.grace.platform.distribution.application.dto.*;
import com.grace.platform.distribution.domain.*;
import com.grace.platform.distribution.domain.event.VideoPublishedEvent;
import com.grace.platform.distribution.domain.AuthorizationUrl;
import com.grace.platform.metadata.domain.VideoMetadataRepository;
import com.grace.platform.shared.ErrorCode;
import com.grace.platform.shared.domain.DomainEventPublisher;
import com.grace.platform.shared.domain.id.MetadataId;
import com.grace.platform.shared.domain.id.PublishRecordId;
import com.grace.platform.shared.domain.id.VideoId;
import com.grace.platform.shared.infrastructure.exception.BusinessRuleViolationException;
import com.grace.platform.shared.infrastructure.exception.EntityNotFoundException;
import com.grace.platform.video.domain.Video;
import com.grace.platform.video.domain.VideoRepository;
import com.grace.platform.video.domain.VideoStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Distribution 应用服务
 * <p>
 * 负责视频分发到各平台的编排，包括：
 * <ul>
 *   <li>发布视频到指定平台</li>
 *   <li>查询上传状态</li>
 *   <li>管理 OAuth 授权流程</li>
 *   <li>列出可用平台</li>
 *   <li>查询发布记录</li>
 * </ul>
 * </p>
 * <p>
 * <strong>存储适配：</strong>
 * 支持从本地文件路径或 OSS URL 发布视频。
 * Video 实体的 storageUrl 字段优先使用，filePath 作为向后兼容。
 * </p>
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
@Service
@Transactional
public class DistributionApplicationService {

    private final VideoDistributorRegistry distributorRegistry;
    private final OAuthService oAuthService;
    private final PublishRecordRepository publishRecordRepository;
    private final OAuthTokenRepository oAuthTokenRepository;
    private final VideoRepository videoRepository;
    private final VideoMetadataRepository videoMetadataRepository;
    private final DomainEventPublisher eventPublisher;

    public DistributionApplicationService(
            VideoDistributorRegistry distributorRegistry,
            OAuthService oAuthService,
            PublishRecordRepository publishRecordRepository,
            OAuthTokenRepository oAuthTokenRepository,
            VideoRepository videoRepository,
            VideoMetadataRepository videoMetadataRepository,
            DomainEventPublisher eventPublisher) {
        this.distributorRegistry = distributorRegistry;
        this.oAuthService = oAuthService;
        this.publishRecordRepository = publishRecordRepository;
        this.oAuthTokenRepository = oAuthTokenRepository;
        this.videoRepository = videoRepository;
        this.videoMetadataRepository = videoMetadataRepository;
        this.eventPublisher = eventPublisher;
    }

    public PublishResultDTO publish(PublishCommand command) {
        Video video = videoRepository.findById(command.videoId())
                .orElseThrow(() -> new EntityNotFoundException(
                        ErrorCode.VIDEO_NOT_FOUND,
                        "Video",
                        command.videoId().value()
                ));

        if (video.getStatus() != VideoStatus.READY_TO_PUBLISH) {
            throw new BusinessRuleViolationException(
                    ErrorCode.VIDEO_NOT_READY,
                    "Video status must be READY_TO_PUBLISH, current: " + video.getStatus()
            );
        }

        var metadata = videoMetadataRepository.findById(command.metadataId())
                .orElseThrow(() -> new EntityNotFoundException(
                        ErrorCode.METADATA_NOT_FOUND,
                        "VideoMetadata",
                        command.metadataId().value()
                ));

        VideoDistributor distributor = distributorRegistry.getDistributor(command.platform());

        OAuthToken token = oAuthTokenRepository.findByPlatform(command.platform())
                .orElseThrow(() -> new BusinessRuleViolationException(
                        ErrorCode.PLATFORM_NOT_AUTHORIZED,
                        "Platform not authorized: " + command.platform()
                ));

        PublishRecord record = PublishRecord.create(
                command.videoId(),
                command.metadataId(),
                command.platform()
        );
        record = publishRecordRepository.save(record);

        video.transitionTo(VideoStatus.PUBLISHING);
        videoRepository.save(video);

        String storageUrl = getStorageUrl(video);

        VideoMetadata metadataVo = new VideoMetadata(
                metadata.getTitle(),
                metadata.getDescription(),
                metadata.getTags()
        );

        PublishResult result = distributor.publish(storageUrl, metadataVo);

        record.markAsUploading(result.taskId());
        record = publishRecordRepository.save(record);

        return new PublishResultDTO(
                record.getId().value(),
                command.videoId().value(),
                command.platform(),
                result.taskId(),
                result.status(),
                record.getCreatedAt()
        );
    }

    private String getStorageUrl(Video video) {
        String storageUrl = video.getStorageUrl();
        if (storageUrl != null && !storageUrl.isBlank()) {
            return storageUrl;
        }
        String filePath = video.getFilePath();
        if (filePath != null && !filePath.isBlank()) {
            return filePath;
        }
        throw new BusinessRuleViolationException(
                ErrorCode.INTERNAL_SERVER_ERROR,
                "Video has no valid storage URL or file path"
        );
    }

    // ==================== D2: 查询上传状态 ====================

    /**
     * 查询上传状态
     * <p>
     * 流程：
     * 1. 根据 taskId 查询 PublishRecord
     * 2. 获取对应平台的 Distributor
     * 3. 调用 distributor.getUploadStatus() 查询最新状态
     * 4. 更新 PublishRecord 的进度和状态
     * 5. 如果状态为 COMPLETED，更新视频状态为 PUBLISHED 并发布 VideoPublishedEvent
     * </p>
     *
     * @param taskId 上传任务 ID
     * @return 上传状态 DTO
     * @throws EntityNotFoundException 当发布任务不存在时
     */
    @Transactional(readOnly = true)
    public UploadStatusDTO getUploadStatus(String taskId) {
        // 1. 查询发布记录
        PublishRecord record = publishRecordRepository.findByUploadTaskId(taskId)
                .orElseThrow(() -> new EntityNotFoundException(
                        ErrorCode.PUBLISH_TASK_NOT_FOUND,
                        "PublishRecord",
                        taskId
                ));

        // 2. 获取分发器
        VideoDistributor distributor = distributorRegistry.getDistributor(record.getPlatform());

        // 3. 查询平台上传状态
        UploadStatus status = distributor.getUploadStatus(taskId);

        // 4. 更新记录进度
        record.updateProgress(status.progressPercent());

        // 5. 根据状态更新记录
        if (status.status() == PublishStatus.COMPLETED) {
            record.markAsCompleted(status.videoUrl());
            publishRecordRepository.save(record);

            // 更新视频状态为 PUBLISHED
            Video video = videoRepository.findById(record.getVideoId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            ErrorCode.VIDEO_NOT_FOUND,
                            "Video",
                            record.getVideoId().value()
                    ));
            video.transitionTo(VideoStatus.PUBLISHED);
            videoRepository.save(video);

            // 发布 VideoPublishedEvent
            VideoPublishedEvent event = new VideoPublishedEvent(
                    record.getVideoId(),
                    record.getPlatform(),
                    status.videoUrl()
            );
            eventPublisher.publish(event);
        } else if (status.status() == PublishStatus.FAILED) {
            record.markAsFailed(status.errorMessage());
            publishRecordRepository.save(record);
        } else if (status.status() == PublishStatus.QUOTA_EXCEEDED) {
            record.markAsQuotaExceeded();
            publishRecordRepository.save(record);
        }

        return new UploadStatusDTO(
                record.getId().value(),
                taskId,
                record.getPlatform(),
                record.getStatus(),
                record.getProgressPercent(),
                record.getVideoUrl(),
                record.getErrorMessage(),
                record.getPublishedAt()
        );
    }

    // ==================== D3: 发起 OAuth 授权 ====================

    /**
     * 发起平台 OAuth 授权
     * <p>
     * 委托 OAuthService 生成授权 URL。
     * </p>
     *
     * @param platform    平台标识
     * @param redirectUri 回调地址
     * @return 授权 URL DTO
     * @throws BusinessRuleViolationException 当平台不支持时
     */
    public AuthUrlDTO initiateAuth(String platform, String redirectUri) {
        // 验证平台是否支持
        distributorRegistry.getDistributor(platform);

        AuthorizationUrl authUrl = oAuthService.initiateAuth(platform, redirectUri);
        return new AuthUrlDTO(authUrl.authUrl(), authUrl.state());
    }

    // ==================== D4: 处理 OAuth 回调 ====================

    /**
     * 处理 OAuth 授权回调
     * <p>
     * 委托 OAuthService 处理回调，交换 Token 并加密存储。
     * </p>
     *
     * @param platform 平台标识
     * @param code     授权码
     * @param state    状态参数
     * @throws BusinessRuleViolationException 当平台不支持或授权码交换失败时
     */
    public void handleAuthCallback(String platform, String code, String state) {
        // 验证平台是否支持
        distributorRegistry.getDistributor(platform);

        oAuthService.handleCallback(platform, code, state);
    }

    // ==================== D5: 列出可用平台 ====================

    /**
     * 列出所有可用的分发平台及其 OAuth 授权状态
     * <p>
     * 流程：
     * 1. 从 Registry 获取所有平台信息
     * 2. 从 OAuthTokenRepository 查询授权状态
     * 3. 合并信息返回平台信息列表
     * </p>
     *
     * @return 平台信息列表
     */
    @Transactional(readOnly = true)
    public List<PlatformInfoDTO> listPlatforms() {
        List<PlatformInfo> platforms = distributorRegistry.listPlatforms();

        return platforms.stream()
                .map(platformInfo -> {
                    // 查询 OAuth 授权状态
                    OAuthToken token = oAuthTokenRepository.findByPlatform(platformInfo.platform())
                            .orElse(null);

                    boolean authorized = token != null;
                    boolean authExpired = token != null && token.isExpired();

                    return new PlatformInfoDTO(
                            platformInfo.platform(),
                            platformInfo.displayName(),
                            authorized,
                            authExpired,
                            platformInfo.enabled()
                    );
                })
                .toList();
    }

    // ==================== D6: 查询发布记录 ====================

    /**
     * 获取指定视频的所有发布记录
     * <p>
     * 委托 PublishRecordRepository 查询。
     * </p>
     *
     * @param videoId 视频 ID
     * @return 发布记录列表
     */
    @Transactional(readOnly = true)
    public List<PublishRecordDTO> getPublishRecords(VideoId videoId) {
        List<PublishRecord> records = publishRecordRepository.findByVideoId(videoId);

        return records.stream()
                .map(this::toPublishRecordDTO)
                .toList();
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 将 PublishRecord 转换为 PublishRecordDTO
     *
     * @param record 发布记录
     * @return 发布记录 DTO
     */
    private PublishRecordDTO toPublishRecordDTO(PublishRecord record) {
        return new PublishRecordDTO(
                record.getId().value(),
                record.getPlatform(),
                record.getStatus(),
                record.getVideoUrl(),
                record.getProgressPercent(),
                record.getErrorMessage(),
                record.getPublishedAt(),
                record.getCreatedAt()
        );
    }

    // ==================== AuthUrlDTO 内部类 ====================

    /**
     * OAuth 授权 URL DTO
     *
     * @param authUrl 授权 URL
     * @param state   状态参数
     */
    public record AuthUrlDTO(String authUrl, String state) {
    }
}
