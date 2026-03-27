package com.grace.platform.video;

import com.grace.platform.GracePlatformApplication;
import com.grace.platform.testutil.AbstractIntegrationTest;
import com.grace.platform.testutil.TestFixtures;
import com.grace.platform.video.application.VideoApplicationService;
import com.grace.platform.video.application.command.UploadInitCommand;
import com.grace.platform.video.application.dto.UploadInitDTO;
import com.grace.platform.video.application.dto.ChunkUploadDTO;
import com.grace.platform.video.application.dto.VideoInfoDTO;
import com.grace.platform.video.application.dto.UploadProgressDTO;
import com.grace.platform.video.domain.UploadSession;
import com.grace.platform.video.domain.UploadSessionRepository;
import com.grace.platform.video.domain.UploadSessionStatus;
import com.grace.platform.video.domain.Video;
import com.grace.platform.video.domain.VideoFormat;
import com.grace.platform.video.domain.VideoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.ByteArrayInputStream;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Video 上下文集成测试。
 * <p>
 * 测试完整分片上传流程：初始化 → 上传分片 → 完成 → 验证。
 * 继承 AbstractIntegrationTest 使用 Testcontainers MySQL 8.0。
 * </p>
 */
@SpringBootTest(classes = GracePlatformApplication.class)
class VideoIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private VideoApplicationService videoService;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private UploadSessionRepository uploadSessionRepository;

    @Test
    @DisplayName("完整分片上传流程：初始化 → 上传分片 → 完成 → 验证")
    void fullChunkedUploadFlow() {
        // 1. Init - 初始化上传
        String fileName = "test.mp4";
        long fileSize = 3 * 1024 * 1024L; // 3MB
        int totalChunks = 3;
        UploadInitCommand initCommand = new UploadInitCommand(fileName, fileSize, VideoFormat.MP4);
        
        UploadInitDTO init = videoService.initUpload(initCommand);
        
        assertThat(init).isNotNull();
        assertThat(init.uploadId()).isNotBlank();
        assertThat(init.totalChunks()).isEqualTo(totalChunks);
        assertThat(init.chunkSize()).isGreaterThan(0);
        String uploadId = init.uploadId();

        // 2. Upload chunks - 上传分片（模拟数据）
        for (int i = 0; i < totalChunks; i++) {
            byte[] chunkData = new byte[1024 * 1024]; // 1MB per chunk
            ByteArrayInputStream chunkInput = new ByteArrayInputStream(chunkData);
            
            ChunkUploadDTO chunkResult = videoService.uploadChunk(uploadId, i, chunkInput);
            
            assertThat(chunkResult).isNotNull();
            assertThat(chunkResult.uploadId()).isEqualTo(uploadId);
            assertThat(chunkResult.chunkIndex()).isEqualTo(i);
            assertThat(chunkResult.uploadedChunks()).isEqualTo(i + 1);
        }

        // 3. Complete - 完成上传
        VideoInfoDTO complete = videoService.completeUpload(uploadId);
        
        assertThat(complete).isNotNull();
        assertThat(complete.videoId()).isNotBlank();
        assertThat(complete.fileName()).isEqualTo(fileName);
        assertThat(complete.format()).isEqualTo("MP4");
        assertThat(complete.fileSize()).isEqualTo(fileSize);
        String videoId = complete.videoId();

        // 4. Verify - 验证视频已保存
        Optional<Video> video = videoRepository.findById(new com.grace.platform.shared.domain.id.VideoId(videoId));
        assertThat(video).isPresent();
        assertThat(video.get().getFileName()).isEqualTo(fileName);
        assertThat(video.get().getFormat()).isEqualTo(VideoFormat.MP4);
        assertThat(video.get().getStatus()).isNotNull();

        // 5. Verify session status - 验证上传会话已标记为完成
        Optional<UploadSession> session = uploadSessionRepository.findById(uploadId);
        assertThat(session).isPresent();
        assertThat(session.get().getStatus()).isEqualTo(UploadSessionStatus.COMPLETED);
    }

    @Test
    @DisplayName("查询上传进度：初始化后查询应返回正确进度")
    void queryUploadProgress() {
        // Init
        UploadInitCommand initCommand = new UploadInitCommand("progress_test.mp4", 1024 * 1024L, VideoFormat.MP4);
        UploadInitDTO init = videoService.initUpload(initCommand);
        String uploadId = init.uploadId();

        // Query progress
        UploadProgressDTO progress = videoService.getUploadProgress(uploadId);
        
        assertThat(progress).isNotNull();
        assertThat(progress.uploadId()).isEqualTo(uploadId);
        assertThat(progress.uploadedChunks()).isEqualTo(0);
        assertThat(progress.totalChunks()).isEqualTo(init.totalChunks());
        assertThat(progress.progressPercent()).isEqualTo(0);
        assertThat(progress.status()).isEqualTo(UploadSessionStatus.ACTIVE);
    }

    @Test
    @DisplayName("视频持久化往返：保存后应能通过 ID 正确查询")
    void videoRoundTrip() {
        // Create and save a video directly
        Video video = TestFixtures.createVideo();
        Video saved = videoRepository.save(video);
        
        // Query by ID
        Optional<Video> found = videoRepository.findById(saved.getId());
        
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getFileName()).isEqualTo(saved.getFileName());
        assertThat(found.get().getFileSize()).isEqualTo(saved.getFileSize());
        assertThat(found.get().getFormat()).isEqualTo(saved.getFormat());
    }
}
