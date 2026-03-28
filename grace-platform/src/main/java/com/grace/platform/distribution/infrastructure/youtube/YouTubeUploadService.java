package com.grace.platform.distribution.infrastructure.youtube;

import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.googleapis.media.MediaHttpUploaderProgressListener;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoSnippet;
import com.google.api.services.youtube.model.VideoStatus;
import com.grace.platform.distribution.domain.OAuthToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * YouTube 视频上传服务
 */
@Service
public class YouTubeUploadService {
    
    private static final Logger logger = LoggerFactory.getLogger(YouTubeUploadService.class);
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String APPLICATION_NAME = "Grace Platform";
    
    private final YouTubeConfig config;
    private final YouTubeOAuthService oAuthService;
    
    public YouTubeUploadService(YouTubeConfig config, YouTubeOAuthService oAuthService) {
        this.config = config;
        this.oAuthService = oAuthService;
    }
    
    /**
     * 上传视频到 YouTube
     */
    public YouTubeUploadResult uploadVideo(YouTubeUploadRequest request) throws Exception {
        // 1. 获取 Token
        OAuthToken token = oAuthService.getStoredToken();
        if (token == null) {
            throw new IllegalStateException("YouTube not authorized. Please authorize first.");
        }
        
        // 2. 创建 Credential
        Credential credential = createCredential(token);
        
        // 3. 创建 YouTube 服务
        YouTube youtube = new YouTube.Builder(
                new NetHttpTransport(),
                JSON_FACTORY,
                credential)
            .setApplicationName(APPLICATION_NAME)
            .build();
        
        // 4. 构建视频元数据
        Video video = new Video();
        
        VideoSnippet snippet = new VideoSnippet();
        snippet.setTitle(request.getTitle());
        snippet.setDescription(request.getDescription());
        snippet.setTags(request.getTags());
        snippet.setCategoryId(request.getCategoryId());
        video.setSnippet(snippet);
        
        VideoStatus status = new VideoStatus();
        status.setPrivacyStatus(request.getPrivacyStatus());
        video.setStatus(status);
        
        // 5. 上传视频
        File videoFile = new File(request.getVideoFilePath());
        InputStreamContent mediaContent = new InputStreamContent(
            "video/*",
            new BufferedInputStream(new FileInputStream(videoFile)));
        mediaContent.setLength(videoFile.length());
        
        YouTube.Videos.Insert videoInsert = youtube.videos()
            .insert("snippet,status", video, mediaContent);
        
        // 6. 设置进度监听器
        videoInsert.getMediaHttpUploader().setProgressListener(
            new MediaHttpUploaderProgressListener() {
                @Override
                public void progressChanged(MediaHttpUploader uploader) throws IOException {
                    switch (uploader.getUploadState()) {
                        case INITIATION_STARTED:
                            logger.info("YouTube upload: Initiation started");
                            break;
                        case INITIATION_COMPLETE:
                            logger.info("YouTube upload: Initiation completed");
                            break;
                        case MEDIA_IN_PROGRESS:
                            double progress = uploader.getProgress() * 100;
                            logger.info("YouTube upload progress: {}%", String.format("%.2f", progress));
                            break;
                        case MEDIA_COMPLETE:
                            logger.info("YouTube upload: Completed");
                            break;
                        case NOT_STARTED:
                            logger.info("YouTube upload: Not started");
                            break;
                    }
                }
            });
        
        // 7. 执行上传
        Video returnedVideo = videoInsert.execute();
        
        logger.info("Video uploaded successfully: {}", returnedVideo.getId());
        
        // 8. 返回结果
        return new YouTubeUploadResult(
            returnedVideo.getId(),
            "https://www.youtube.com/watch?v=" + returnedVideo.getId(),
            returnedVideo.getSnippet().getTitle(),
            returnedVideo.getStatus().getUploadStatus()
        );
    }
    
    private Credential createCredential(OAuthToken token) throws Exception {
        return new Credential.Builder(BearerToken.authorizationHeaderAccessMethod())
            .setTransport(new NetHttpTransport())
            .setJsonFactory(JSON_FACTORY)
            .setClientAuthentication(new ClientParametersAuthentication(
                config.getClientId(), config.getClientSecret()))
            .setTokenServerEncodedUrl("https://oauth2.googleapis.com/token")
            .build()
            .setAccessToken(decrypt(token.getAccessToken()))
            .setRefreshToken(decrypt(token.getRefreshToken()));
    }
    
    private String decrypt(String value) {
        // TODO: 实现解密
        // 暂时直接返回
        return value;
    }
}
