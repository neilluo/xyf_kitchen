package com.grace.platform.video.infrastructure.file;

import com.grace.platform.shared.infrastructure.exception.FileOperationException;
import com.grace.platform.video.domain.ImageFrame;
import com.grace.platform.video.domain.VideoFileInfo;
import com.grace.platform.video.domain.VideoFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VideoFrameExtractorTest {

    @Mock
    private VideoFileInspectorImpl videoFileInspector;

    private VideoFrameExtractorImpl videoFrameExtractor;

    @BeforeEach
    void setUp() {
        videoFrameExtractor = new VideoFrameExtractorImpl(videoFileInspector);
    }

    @Test
    @DisplayName("ImageFrame 应正确创建并验证参数")
    void imageFrameShouldValidateParameters() {
        ImageFrame frame = ImageFrame.jpeg("base64data", 0.5);

        assertThat(frame.base64Data()).isEqualTo("base64data");
        assertThat(frame.mimeType()).isEqualTo("image/jpeg");
        assertThat(frame.position()).isEqualTo(0.5);
        assertThat(frame.toDataUri()).isEqualTo("data:image/jpeg;base64,base64data");
        assertThat(frame.isMiddleFrame()).isTrue();
        assertThat(frame.isStartFrame()).isFalse();
        assertThat(frame.isEndFrame()).isFalse();
    }

    @Test
    @DisplayName("ImageFrame 应正确识别开头帧")
    void imageFrameShouldIdentifyStartFrame() {
        ImageFrame frame = ImageFrame.jpeg("base64data", 0.0);

        assertThat(frame.isStartFrame()).isTrue();
        assertThat(frame.isMiddleFrame()).isFalse();
        assertThat(frame.isEndFrame()).isFalse();
    }

    @Test
    @DisplayName("ImageFrame 应正确识别结尾帧")
    void imageFrameShouldIdentifyEndFrame() {
        ImageFrame frame = ImageFrame.jpeg("base64data", 1.0);

        assertThat(frame.isEndFrame()).isTrue();
        assertThat(frame.isStartFrame()).isFalse();
        assertThat(frame.isMiddleFrame()).isFalse();
    }

    @Test
    @DisplayName("ImageFrame 应拒绝无效位置")
    void imageFrameShouldRejectInvalidPosition() {
        assertThatThrownBy(() -> ImageFrame.jpeg("base64data", -0.1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("position must be between 0.0 and 1.0");

        assertThatThrownBy(() -> ImageFrame.jpeg("base64data", 1.1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("position must be between 0.0 and 1.0");
    }

    @Test
    @DisplayName("ImageFrame 应拒绝 null 参数")
    void imageFrameShouldRejectNullParameters() {
        assertThatThrownBy(() -> new ImageFrame(null, "image/jpeg", 0.5))
            .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> new ImageFrame("base64data", null, 0.5))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("VideoFrameExtractor 应拒绝不存在的视频文件")
    void shouldRejectNonExistentVideoFile() {
        Path nonExistentPath = Path.of("/non/existent/video.mp4");

        assertThatThrownBy(() -> videoFrameExtractor.extractFrames(nonExistentPath))
            .isInstanceOf(FileOperationException.class)
            .hasMessageContaining("Video file does not exist");
    }

    @Test
    @DisplayName("VideoFrameExtractor 应拒绝无效的帧数量")
    void shouldRejectInvalidFrameCount() {
        assertThatThrownBy(() -> videoFrameExtractor.extractFrames(Path.of("/tmp/test.mp4"), 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("frameCount must be between 1 and 10");

        assertThatThrownBy(() -> videoFrameExtractor.extractFrames(Path.of("/tmp/test.mp4"), 11))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("frameCount must be between 1 and 10");
    }
}
