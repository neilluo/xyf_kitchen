package com.grace.platform.distribution.infrastructure.youtube;

import java.util.List;

/**
 * YouTube 视频上传请求
 */
public class YouTubeUploadRequest {
    
    private String videoFilePath;      // 本地视频文件路径
    private String title;              // 视频标题
    private String description;        // 视频描述
    private List<String> tags;         // 标签
    private String categoryId;         // YouTube 分类 ID (默认 22 = People & Blogs)
    private String privacyStatus;      // public, unlisted, private (默认 private)
    
    public YouTubeUploadRequest() {
        this.categoryId = "22";        // People & Blogs
        this.privacyStatus = "private"; // 默认私有
    }
    
    // Getters and Setters
    public String getVideoFilePath() {
        return videoFilePath;
    }
    
    public void setVideoFilePath(String videoFilePath) {
        this.videoFilePath = videoFilePath;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public List<String> getTags() {
        return tags;
    }
    
    public void setTags(List<String> tags) {
        this.tags = tags;
    }
    
    public String getCategoryId() {
        return categoryId;
    }
    
    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }
    
    public String getPrivacyStatus() {
        return privacyStatus;
    }
    
    public void setPrivacyStatus(String privacyStatus) {
        this.privacyStatus = privacyStatus;
    }
}
