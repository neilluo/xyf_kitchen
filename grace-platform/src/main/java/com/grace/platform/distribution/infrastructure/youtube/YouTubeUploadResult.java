package com.grace.platform.distribution.infrastructure.youtube;

/**
 * YouTube 视频上传结果
 */
public class YouTubeUploadResult {
    
    private String videoId;            // YouTube 视频 ID
    private String videoUrl;           // YouTube 视频链接
    private String title;              // 视频标题
    private String uploadStatus;       // 上传状态
    
    public YouTubeUploadResult() {
    }
    
    public YouTubeUploadResult(String videoId, String videoUrl, String title, String uploadStatus) {
        this.videoId = videoId;
        this.videoUrl = videoUrl;
        this.title = title;
        this.uploadStatus = uploadStatus;
    }
    
    // Getters and Setters
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
    
    public String getUploadStatus() {
        return uploadStatus;
    }
    
    public void setUploadStatus(String uploadStatus) {
        this.uploadStatus = uploadStatus;
    }
    
    /**
     * 创建已完成的上传结果
     */
    public static YouTubeUploadResult completed(String videoId, String videoUrl) {
        YouTubeUploadResult result = new YouTubeUploadResult();
        result.setVideoId(videoId);
        result.setVideoUrl(videoUrl);
        result.setUploadStatus("completed");
        return result;
    }
}
