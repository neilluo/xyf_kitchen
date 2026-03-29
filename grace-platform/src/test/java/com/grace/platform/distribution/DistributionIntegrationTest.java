package com.grace.platform.distribution;

import com.grace.platform.distribution.application.DistributionApplicationService;
import com.grace.platform.distribution.application.command.PublishCommand;
import com.grace.platform.distribution.application.dto.PublishResultDTO;
import com.grace.platform.distribution.application.dto.UploadStatusDTO;
import com.grace.platform.distribution.application.dto.PublishRecordDTO;
import com.grace.platform.distribution.domain.OAuthToken;
import com.grace.platform.distribution.domain.OAuthTokenRepository;
import com.grace.platform.distribution.domain.PublishRecord;
import com.grace.platform.distribution.domain.PublishRecordRepository;
import com.grace.platform.distribution.domain.PublishStatus;
import com.grace.platform.distribution.infrastructure.youtube.YouTubeApiAdapter;
import com.grace.platform.distribution.infrastructure.youtube.YouTubeUploadResult;
import com.grace.platform.distribution.infrastructure.youtube.YouTubeUploadProgress;
import com.grace.platform.distribution.infrastructure.youtube.YouTubeUploadStatus;
import com.grace.platform.metadata.domain.VideoMetadata;
import com.grace.platform.metadata.domain.VideoMetadataRepository;
import com.grace.platform.GracePlatformApplication;
import com.grace.platform.shared.domain.id.MetadataId;
import com.grace.platform.shared.domain.id.VideoId;
import com.grace.platform.testutil.AbstractIntegrationTest;
import com.grace.platform.testutil.TestFixtures;
import com.grace.platform.video.domain.Video;
import com.grace.platform.video.domain.VideoRepository;
import com.grace.platform.video.domain.VideoStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Distribution 上下文集成测试。
 * <p>
 * 测试发布流程：publish（Mock YouTube API）→ 查询状态 → 查询记录。
 * 继承 AbstractIntegrationTest 使用 Testcontainers MySQL 8.0。
 * 使用 @MockBean 模拟 YouTubeApiAdapter。
 * </p>
 */
@SpringBootTest(classes = GracePlatformApplication.class)
class DistributionIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private DistributionApplicationService distributionService;

    @Autowired
    private PublishRecordRepository publishRecordRepository;

    @Autowired
    private OAuthTokenRepository oAuthTokenRepository;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private VideoMetadataRepository metadataRepository;

    @MockBean
    private YouTubeApiAdapter youTubeApiAdapter;

    @BeforeEach
    void setupMocks() {
        // Mock YouTube API 返回预定义的上传结果
        String taskId = "youtube_task_123";
        String videoUrl = "https://www.youtube.com/watch?v=test123";
        YouTubeUploadResult mockResult = YouTubeUploadResult.completed(taskId, videoUrl);
        
        when(youTubeApiAdapter.uploadVideo(
            anyString(), 
            anyString(), 
            anyString(), 
            anyString(), 
            anyList(), 
            anyString()
        )).thenReturn(mockResult);

        // Mock 上传进度查询
        YouTubeUploadProgress mockProgress = new YouTubeUploadProgress(
            "upload_uri_123", 
            1000000, 
            1000000, 
            YouTubeUploadStatus.COMPLETED, 
            videoUrl, 
            null
        );
        when(youTubeApiAdapter.getUploadProgress(anyString(), anyString()))
            .thenReturn(mockProgress);
    }

    @Test
    @DisplayName("视频发布完整流程：publish → 查询状态 → 查询记录")
    void publishFlow() {
        // Given: 准备视频和元数据
        VideoId videoId = TestFixtures.randomVideoId();
        MetadataId metadataId = TestFixtures.randomMetadataId();

        // 创建视频（状态为 READY_TO_PUBLISH）
        Video video = Video.create(
            "test_video.mp4",
            1024 * 1024L,
            com.grace.platform.video.domain.VideoFormat.MP4,
            java.time.Duration.ofSeconds(120),
            "/storage/videos/test.mp4"
        );
        // 手动将视频状态设置为 READY_TO_PUBLISH
        video.transitionTo(VideoStatus.READY_TO_PUBLISH);
        videoRepository.save(video);
        videoId = video.getId();

        // 创建元数据
        VideoMetadata metadata = VideoMetadata.create(
            videoId,
            "测试视频标题",
            "测试视频描述",
            List.of("测试", "视频", "美食", "教程", "烹饪"),
            com.grace.platform.metadata.domain.MetadataSource.AI_GENERATED
        );
        metadata.confirm(); // 确认元数据
        metadataRepository.save(metadata);
        metadataId = metadata.getId();

        // 创建 OAuth Token（模拟已授权）
        OAuthToken token = OAuthToken.create(
            "youtube",
            "encrypted_access_token",
            "encrypted_refresh_token",
            LocalDateTime.now().plusHours(1)
        );
        oAuthTokenRepository.save(token);

        // 1. Publish - 发布视频
        PublishCommand publishCommand = new PublishCommand(videoId, metadataId, "youtube", "public");
        PublishResultDTO result = distributionService.publish(publishCommand);
        
        assertThat(result).isNotNull();
        assertThat(result.publishRecordId()).isNotBlank();
        assertThat(result.videoId()).isEqualTo(videoId.value());
        assertThat(result.platform()).isEqualTo("youtube");
        assertThat(result.uploadTaskId()).isNotBlank();
        
        String taskId = result.uploadTaskId();
        String recordId = result.publishRecordId();

        // 2. Query upload status - 查询上传状态
        UploadStatusDTO status = distributionService.getUploadStatus(taskId);
        
        assertThat(status).isNotNull();
        assertThat(status.publishRecordId()).isEqualTo(recordId);
        assertThat(status.platform()).isEqualTo("youtube");

        // 3. Query publish records - 查询发布记录
        List<PublishRecordDTO> records = distributionService.getPublishRecords(videoId);
        
        assertThat(records).isNotEmpty();
        assertThat(records.get(0).platform()).isEqualTo("youtube");
    }

    @Test
    @DisplayName("发布记录持久化往返：保存后应能通过视频 ID 正确查询")
    void publishRecordRoundTrip() {
        // Create and save a publish record
        VideoId videoId = TestFixtures.randomVideoId();
        PublishRecord record = TestFixtures.createPublishRecord(videoId);
        PublishRecord saved = publishRecordRepository.save(record);
        
        // Query by video ID
        List<PublishRecord> found = publishRecordRepository.findByVideoId(videoId);
        
        assertThat(found).isNotEmpty();
        assertThat(found.get(0).getVideoId()).isEqualTo(saved.getVideoId());
        assertThat(found.get(0).getPlatform()).isEqualTo(saved.getPlatform());
    }

    @Test
    @DisplayName("OAuth Token 持久化往返：保存后应能通过平台标识正确查询")
    void oauthTokenRoundTrip() {
        // Create and save OAuth token
        OAuthToken token = OAuthToken.create(
            "youtube",
            "test_access_token",
            "test_refresh_token",
            LocalDateTime.now().plusHours(1)
        );
        OAuthToken saved = oAuthTokenRepository.save(token);
        
        // Query by platform
        java.util.Optional<OAuthToken> found = oAuthTokenRepository.findByPlatform("youtube");
        
        assertThat(found).isPresent();
        assertThat(found.get().getPlatform()).isEqualTo("youtube");
        assertThat(found.get().getId()).isEqualTo(saved.getId());
    }

    @Test
    @DisplayName("列出可用平台及其授权状态")
    void listPlatforms() {
        // Create OAuth token
        OAuthToken token = OAuthToken.create(
            "youtube",
            "test_access_token",
            "test_refresh_token",
            LocalDateTime.now().plusHours(1)
        );
        oAuthTokenRepository.save(token);

        // List platforms
        var platforms = distributionService.listPlatforms();
        
        assertThat(platforms).isNotEmpty();
        // YouTube should be in the list (from YouTubeDistributor @Component)
        assertThat(platforms).anyMatch(p -> p.platform().equals("youtube"));
    }
}
