package com.grace.platform.video;

import com.grace.platform.GracePlatformApplication;
import com.grace.platform.testutil.AbstractIntegrationTest;
import com.grace.platform.testutil.TestFixtures;
import com.grace.platform.video.domain.Video;
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
    private VideoRepository videoRepository;

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
