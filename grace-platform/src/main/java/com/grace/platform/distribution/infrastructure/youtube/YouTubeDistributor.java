package com.grace.platform.distribution.infrastructure.youtube;

import com.grace.platform.distribution.domain.*;
import com.grace.platform.shared.ErrorCode;
import com.grace.platform.shared.infrastructure.exception.ExternalServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * YouTube 视频分发器实现
 * <p>
 * 实现 {@link ResumableVideoDistributor} 接口，提供 YouTube 平台特定的视频发布功能。
 * 支持断点续传、OAuth 认证、配额管理。
 * </p>
 * <p>
 * <strong>YouTube API 配额：</strong>每日 10,000 units，单次上传消耗 1,600 units。
 * </p>
 *
 * @author Grace Platform Team
 * @see VideoDistributor
 * @see ResumableVideoDistributor
 */
@Component
public class YouTubeDistributor implements ResumableVideoDistributor {

    private static final Logger logger = LoggerFactory.getLogger(YouTubeDistributor.class);

    private final YouTubeApiAdapter youTubeApiAdapter;
    private final OAuthService oAuthService;

    public YouTubeDistributor(YouTubeApiAdapter youTubeApiAdapter, OAuthService oAuthService) {
        this.youTubeApiAdapter = youTubeApiAdapter;
        this.oAuthService = oAuthService;
    }

    @Override
    public String platform() {
        return "youtube";
    }

    /**
     * 获取显示名称（用于平台列表展示）
     */
    public String displayName() {
        return "YouTube";
    }

    /**
     * 检查平台是否可用
     */
    public boolean isEnabled() {
        return true;
    }

    @Override
    public PublishResult publish(VideoFile videoFile, VideoMetadata metadata) {
        logger.info("Publishing video to YouTube: file={}, title={}",
            videoFile.fileName(), metadata.title());

        try {
            // 1. 获取有效 Token（自动刷新过期 Token）
            OAuthToken token = oAuthService.getValidToken(platform());
            String accessToken = token.getAccessToken();

            logger.debug("Using OAuth token for platform: {}, expired={}",
                platform(), token.isExpired());

            // 2. 调用 YouTube API 上传视频
            // 默认隐私状态为 public
            String privacyStatus = "public";

            YouTubeUploadResult uploadResult = youTubeApiAdapter.uploadVideo(
                accessToken,
                videoFile.filePath(),
                metadata.title(),
                metadata.description(),
                metadata.tags(),
                privacyStatus
            );

            // 3. 根据上传结果构建 PublishResult
            PublishStatus status = mapYouTubeStatus(uploadResult.status());
            String videoUrl = uploadResult.videoUrl();

            PublishResult result = new PublishResult(
                uploadResult.taskId(),
                status,
                videoUrl
            );

            logger.info("YouTube publish initiated: taskId={}, status={}",
                result.taskId(), result.status());

            return result;

        } catch (ExternalServiceException e) {
            logger.error("YouTube API error during publish: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during YouTube publish: {}", e.getMessage(), e);
            throw new ExternalServiceException(
                ErrorCode.PLATFORM_API_ERROR,
                "YouTube",
                "Upload failed: " + e.getMessage()
            );
        }
    }

    @Override
    public UploadStatus getUploadStatus(String taskId) {
        logger.info("Querying YouTube upload status: taskId={}", taskId);

        try {
            // 获取有效 Token
            OAuthToken token = oAuthService.getValidToken(platform());
            String accessToken = token.getAccessToken();

            // 注意：taskId 在 YouTube 中通常是 uploadUri
            // 这里简化处理，假设 taskId 就是 uploadUri
            YouTubeUploadProgress progress = youTubeApiAdapter.getUploadProgress(accessToken, taskId);

            PublishStatus status = mapYouTubeStatus(progress.status());
            int progressPercent = progress.getProgressPercent();

            UploadStatus uploadStatus = new UploadStatus(
                taskId,
                status,
                progressPercent,
                progress.videoUrl(),
                progress.errorMessage()
            );

            logger.debug("YouTube upload status: taskId={}, progress={}%, status={}",
                taskId, progressPercent, status);

            return uploadStatus;

        } catch (Exception e) {
            logger.error("Failed to query YouTube upload status: {}", e.getMessage(), e);
            throw new ExternalServiceException(
                ErrorCode.PLATFORM_API_ERROR,
                "YouTube",
                "Failed to query upload status: " + e.getMessage()
            );
        }
    }

    @Override
    public PublishResult resumeUpload(String taskId) {
        logger.info("Resuming YouTube upload: taskId={}", taskId);

        try {
            // 获取有效 Token
            OAuthToken token = oAuthService.getValidToken(platform());
            String accessToken = token.getAccessToken();

            // taskId 实际上是 uploadUri
            // 需要获取原视频文件路径 - 这需要从 PublishRecord 中获取
            // 这里简化处理，实际实现中需要从 Repository 查询

            // 注意：这里缺少 videoFile 参数，实际实现需要调整
            // 暂时返回模拟结果
            logger.warn("Resume upload requires video file path, returning mock result");

            return new PublishResult(taskId, PublishStatus.UPLOADING);

        } catch (Exception e) {
            logger.error("Failed to resume YouTube upload: {}", e.getMessage(), e);
            throw new ExternalServiceException(
                ErrorCode.PLATFORM_API_ERROR,
                "YouTube",
                "Failed to resume upload: " + e.getMessage()
            );
        }
    }

    /**
     * 将 YouTube 上传状态映射为领域 PublishStatus
     */
    private PublishStatus mapYouTubeStatus(YouTubeUploadStatus youtubeStatus) {
        return switch (youtubeStatus) {
            case UPLOADING -> PublishStatus.UPLOADING;
            case COMPLETED -> PublishStatus.COMPLETED;
            case FAILED -> PublishStatus.FAILED;
            case QUOTA_EXCEEDED -> PublishStatus.QUOTA_EXCEEDED;
        };
    }
}
