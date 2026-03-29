package com.grace.platform.distribution.infrastructure.youtube;

import com.grace.platform.distribution.domain.PublishResult;
import com.grace.platform.distribution.domain.PublishStatus;

/**
 * YouTube 视频上传结果
 */
public class YouTubeUploadResult {
    
    private String videoId;
    private String videoUrl;
    private String title;
    private YouTubeUploadStatus uploadStatus;
    
    public YouTubeUploadResult() {
    }
    
    public YouTubeUploadResult(String videoId, String videoUrl, String title, YouTubeUploadStatus uploadStatus) {
        this.videoId = videoId;
        this.videoUrl = videoUrl;
        this.title = title;
        this.uploadStatus = uploadStatus;
    }
    
    public String getVideoId() {
        return videoId;
    }
    
    public void setVideoId(String videoId) {
        this.videoId = videoId;
    }
    
    public String getVideoUrl() {
        return videoUrl;
    }
    
    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public YouTubeUploadStatus getUploadStatus() {
        return uploadStatus;
    }
    
    public void setUploadStatus(YouTubeUploadStatus uploadStatus) {
        this.uploadStatus = uploadStatus;
    }
    
    public static YouTubeUploadResult completed(String videoId, String videoUrl) {
        YouTubeUploadResult result = new YouTubeUploadResult();
        result.setVideoId(videoId);
        result.setVideoUrl(videoUrl);
        result.setUploadStatus(YouTubeUploadStatus.COMPLETED);
        return result;
    }
    
    public PublishResult toPublishResult() {
        if (uploadStatus == null) {
            return new PublishResult(videoId, PublishStatus.PENDING, videoUrl);
        }
        PublishStatus status = switch (uploadStatus) {
            case UPLOADING -> PublishStatus.UPLOADING;
            case COMPLETED -> PublishStatus.COMPLETED;
            case FAILED -> PublishStatus.FAILED;
            case QUOTA_EXCEEDED -> PublishStatus.QUOTA_EXCEEDED;
        };
        return new PublishResult(videoId, status, videoUrl);
    }
}
