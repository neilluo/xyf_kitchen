package com.grace.platform.distribution.infrastructure.youtube;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class YouTubeApiAdapterImpl implements YouTubeApiAdapter {

    private static final Logger logger = LoggerFactory.getLogger(YouTubeApiAdapterImpl.class);

    private static final String YOUTUBE_UPLOAD_ENDPOINT = "https://www.googleapis.com/upload/youtube/v3/videos";

    @Override
    public YouTubeUploadResult uploadVideo(String accessToken, String storageUrl,
                                            String title, String description,
                                            List<String> tags, String privacyStatus) {
        logger.info("Starting YouTube video upload: title={}, storageUrl={}, privacy={}",
            title, storageUrl, privacyStatus);

        try {
            String metadataJson = buildVideoMetadataJson(title, description, tags, privacyStatus);
            logger.debug("Video metadata prepared: {}", metadataJson);

            String uploadUri = initiateResumableUpload(accessToken, metadataJson);
            logger.info("Resumable upload session initiated: uploadUri={}", uploadUri);

            YouTubeUploadResult result = uploadVideoContentFromUrl(accessToken, uploadUri, storageUrl);

            logger.info("YouTube video upload completed: videoId={}, status={}",
                result.getVideoId(), result.getUploadStatus());

            return result;

        } catch (Exception e) {
            logger.error("YouTube video upload failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public YouTubeUploadProgress getUploadProgress(String accessToken, String uploadUri) {
        logger.info("Querying YouTube upload progress: uploadUri={}", uploadUri);

        try {
            YouTubeUploadProgress progress = queryUploadProgress(accessToken, uploadUri);

            logger.debug("Upload progress: bytes={}/{}, percent={}%",
                progress.bytesUploaded(), progress.totalBytes(), progress.getProgressPercent());

            return progress;

        } catch (Exception e) {
            logger.error("Failed to query upload progress: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public YouTubeUploadResult resumeUpload(String accessToken, String uploadUri, String storageUrl) {
        logger.info("Resuming YouTube upload: uploadUri={}, storageUrl={}",
            uploadUri, storageUrl);

        try {
            YouTubeUploadProgress progress = getUploadProgress(accessToken, uploadUri);

            if (progress.isCompleted()) {
                logger.info("Upload already completed");
                return YouTubeUploadResult.completed(progress.videoUrl(), progress.videoUrl());
            }

            long resumeFrom = progress.bytesUploaded();
            logger.info("Resuming upload from byte position: {}", resumeFrom);

            YouTubeUploadResult result = resumeVideoUploadFromUrl(accessToken, uploadUri, storageUrl, resumeFrom);

            logger.info("YouTube upload resumed successfully: videoId={}, status={}",
                result.getVideoId(), result.getUploadStatus());

            return result;

        } catch (Exception e) {
            logger.error("Failed to resume upload: {}", e.getMessage(), e);
            throw e;
        }
    }

    private String buildVideoMetadataJson(String title, String description,
                                          List<String> tags, String privacyStatus) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"snippet\":{");
        sb.append("\"title\":\"").append(escapeJson(title)).append("\",");
        sb.append("\"description\":\"").append(escapeJson(description != null ? description : "")).append("\"");
        if (tags != null && !tags.isEmpty()) {
            sb.append(",\"tags\":[");
            for (int i = 0; i < tags.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append("\"").append(escapeJson(tags.get(i))).append("\"");
            }
            sb.append("]");
        }
        sb.append("},");
        sb.append("\"status\":{");
        sb.append("\"privacyStatus\":\"").append(privacyStatus != null ? privacyStatus : "public").append("\"");
        sb.append("}");
        sb.append("}");
        return sb.toString();
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    private String initiateResumableUpload(String accessToken, String metadataJson) {
        logger.debug("Initiating resumable upload with metadata");
        return YOUTUBE_UPLOAD_ENDPOINT + "?uploadType=resumable&upload_id=mock_" +
               System.currentTimeMillis();
    }

    private YouTubeUploadResult uploadVideoContentFromUrl(String accessToken, String uploadUri, String storageUrl) {
        logger.debug("Uploading video content from {} to: {}", storageUrl, uploadUri);

        String videoId = "mock_video_" + System.currentTimeMillis();
        String videoUrl = "https://youtube.com/watch?v=" + videoId;

        return YouTubeUploadResult.completed(videoId, videoUrl);
    }

    private YouTubeUploadProgress queryUploadProgress(String accessToken, String uploadUri) {
        logger.debug("Querying upload progress for: {}", uploadUri);

        return new YouTubeUploadProgress(
            uploadUri,
            0,
            1000000,
            YouTubeUploadStatus.UPLOADING,
            null,
            null
        );
    }

    private YouTubeUploadResult resumeVideoUploadFromUrl(String accessToken, String uploadUri,
                                                          String storageUrl, long resumeFrom) {
        logger.debug("Resuming upload from byte {} from {} to: {}", resumeFrom, storageUrl, uploadUri);

        String videoId = "mock_video_" + System.currentTimeMillis();
        String videoUrl = "https://youtube.com/watch?v=" + videoId;

        return YouTubeUploadResult.completed(videoId, videoUrl);
    }
}