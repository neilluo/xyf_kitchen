package com.grace.platform.video;

import com.grace.platform.GracePlatformApplication;
import com.grace.platform.testutil.AbstractIntegrationTest;
import com.grace.platform.testutil.TestFixtures;
import com.grace.platform.storage.domain.StorageProvider;
import com.grace.platform.storage.domain.UploadCallback;
import com.grace.platform.video.application.VideoApplicationService;
import com.grace.platform.video.application.command.UploadInitCommand;
import com.grace.platform.video.application.dto.UploadInitDTO;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = GracePlatformApplication.class)
class VideoIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private VideoApplicationService videoService;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private UploadSessionRepository uploadSessionRepository;

    @Test
    @DisplayName("OSS 直传上传流程：初始化 → OSS 回调 → 验证")
    void ossDirectUploadFlow() {
        String fileName = "test.mp4";
        long fileSize = 3 * 1024 * 1024L;
        UploadInitCommand initCommand = new UploadInitCommand(fileName, fileSize, VideoFormat.MP4);
        
        UploadInitDTO init = videoService.initUpload(initCommand);
        
        assertThat(init).isNotNull();
        assertThat(init.uploadId()).isNotBlank();
        assertThat(init.totalChunks()).isGreaterThan(0);
        assertThat(init.chunkSize()).isGreaterThan(0);
        assertThat(init.stsCredentials()).isNotNull();
        assertThat(init.storageKey()).isNotBlank();
        assertThat(init.ossBucket()).isNotBlank();
        String uploadId = init.uploadId();

        UploadCallback callback = new UploadCallback(
            init.ossBucket(),
            init.storageKey(),
            "test-etag-12345",
            fileSize,
            "video/mp4",
            uploadId
        );
        VideoInfoDTO complete = videoService.completeUpload(callback);
        
        assertThat(complete).isNotNull();
        assertThat(complete.videoId()).isNotBlank();
        assertThat(complete.fileName()).isEqualTo(fileName);
        assertThat(complete.format()).isEqualTo("MP4");
        assertThat(complete.fileSize()).isEqualTo(fileSize);
        String videoId = complete.videoId();

        Optional<Video> video = videoRepository.findById(new com.grace.platform.shared.domain.id.VideoId(videoId));
        assertThat(video).isPresent();
        assertThat(video.get().getFileName()).isEqualTo(fileName);
        assertThat(video.get().getFormat()).isEqualTo(VideoFormat.MP4);
        assertThat(video.get().getStatus()).isNotNull();
        assertThat(video.get().getStorageProvider()).isEqualTo(StorageProvider.OSS);
        assertThat(video.get().getStorageUrl()).isNotNull();

        Optional<UploadSession> session = uploadSessionRepository.findById(uploadId);
        assertThat(session).isPresent();
        assertThat(session.get().getStatus()).isEqualTo(UploadSessionStatus.COMPLETED);
    }

    @Test
    @DisplayName("查询上传进度：初始化后查询应返回正确进度")
    void queryUploadProgress() {
        UploadInitCommand initCommand = new UploadInitCommand("progress_test.mp4", 1024 * 1024L, VideoFormat.MP4);
        UploadInitDTO init = videoService.initUpload(initCommand);
        String uploadId = init.uploadId();

        UploadProgressDTO progress = videoService.getUploadProgress(uploadId);
        
        assertThat(progress).isNotNull();
        assertThat(progress.uploadId()).isEqualTo(uploadId);
        assertThat(progress.uploadedChunks()).isEqualTo(0);
        assertThat(progress.totalChunks()).isEqualTo(init.totalChunks());
        assertThat(progress.status()).isEqualTo(UploadSessionStatus.ACTIVE);
    }

    @Test
    @DisplayName("视频持久化往返：保存后应能通过 ID 正确查询")
    void videoRoundTrip() {
        Video video = TestFixtures.createVideo();
        Video saved = videoRepository.save(video);
        
        Optional<Video> found = videoRepository.findById(saved.getId());
        
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getFileName()).isEqualTo(saved.getFileName());
        assertThat(found.get().getFileSize()).isEqualTo(saved.getFileSize());
        assertThat(found.get().getFormat()).isEqualTo(saved.getFormat());
    }
}