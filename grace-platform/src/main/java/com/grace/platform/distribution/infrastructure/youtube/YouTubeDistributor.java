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
 * <strong>存储 URL 支持：</strong>
 * <ul>
 *   <li>本地文件路径：如 "/path/to/video.mp4"</li>
 *   <li>OSS URL：如 "https://bucket.oss.aliyuncs.com/video.mp4"</li>
 * </ul>
 * YouTube API 支持从 URL 流式上传视频。
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

    public String displayName() {
        return "YouTube";
    }

    public boolean isEnabled() {
        return true;
    }

    @Override
    public PublishResult publish(String storageUrl, VideoMetadata metadata) {
        logger.info("Publishing video to YouTube: storageUrl={}, title={}",
            storageUrl, metadata.title());

        try {
            OAuthToken token = oAuthService.getValidToken(platform());
            String accessToken = token.getAccessToken();

            logger.debug("Using OAuth token for platform: {}, expired={}",
                platform(), token.isExpired());

            String privacyStatus = "public";

            YouTubeUploadResult uploadResult = youTubeApiAdapter.uploadVideo(
                accessToken,
                storageUrl,
                metadata.title(),
                metadata.description(),
                metadata.tags(),
                privacyStatus
            );

            PublishStatus status = mapYouTubeStatus(uploadResult.getUploadStatus());
            String videoUrl = uploadResult.getVideoUrl();

            PublishResult result = new PublishResult(
                uploadResult.getVideoId(),
                status,
                videoUrl
            );

            logger.info("YouTube publish initiated: videoId={}, status={}",
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
            OAuthToken token = oAuthService.getValidToken(platform());
            String accessToken = token.getAccessToken();

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
    public PublishResult resumeUpload(String taskId, String storageUrl) {
        logger.info("Resuming YouTube upload: taskId={}, storageUrl={}", taskId, storageUrl);

        try {
            OAuthToken token = oAuthService.getValidToken(platform());
            String accessToken = token.getAccessToken();

            PublishResult result = youTubeApiAdapter.resumeUpload(accessToken, taskId, storageUrl)
                .toPublishResult();

            logger.info("YouTube upload resumed: taskId={}, status={}",
                taskId, result.status());

            return result;

        } catch (Exception e) {
            logger.error("Failed to resume YouTube upload: {}", e.getMessage(), e);
            throw new ExternalServiceException(
                ErrorCode.PLATFORM_API_ERROR,
                "YouTube",
                "Failed to resume upload: " + e.getMessage()
            );
        }
    }

    private PublishStatus mapYouTubeStatus(YouTubeUploadStatus youtubeStatus) {
        if (youtubeStatus == null) {
            return PublishStatus.PENDING;
        }
        return switch (youtubeStatus) {
            case UPLOADING -> PublishStatus.UPLOADING;
            case COMPLETED -> PublishStatus.COMPLETED;
            case FAILED -> PublishStatus.FAILED;
            case QUOTA_EXCEEDED -> PublishStatus.QUOTA_EXCEEDED;
        };
    }
    
    private PublishStatus mapYouTubeStatus(String status) {
        if (status == null) {
            return PublishStatus.PENDING;
        }
        return switch (status.toLowerCase()) {
            case "uploading" -> PublishStatus.UPLOADING;
            case "completed", "processed" -> PublishStatus.COMPLETED;
            case "failed" -> PublishStatus.FAILED;
            default -> PublishStatus.PENDING;
        };
    }
}
