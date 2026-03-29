package com.grace.platform.video.infrastructure.file;

import com.grace.platform.shared.infrastructure.exception.FileOperationException;
import com.grace.platform.video.domain.ImageFrame;
import com.grace.platform.video.domain.VideoFrameExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * 视频帧提取器实现。
 * <p>
 * 使用 FFmpeg 从视频中提取关键帧，转换为 Base64 编码的图像。
 * 默认提取 3 帧：开头(0%)、中间(50%)、结尾(100%)。
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
@Component
public class VideoFrameExtractorImpl implements VideoFrameExtractor {

    private static final Logger logger = LoggerFactory.getLogger(VideoFrameExtractorImpl.class);

    private static final String FFMPEG_COMMAND = "ffmpeg";
    private static final String FFPROBE_COMMAND = "ffprobe";
    private static final long FFMPEG_TIMEOUT_SECONDS = 60;
    private static final String TEMP_FILE_PREFIX = "grace-frame-";
    private static final String TEMP_FILE_SUFFIX = ".jpg";

    private final VideoFileInspectorImpl videoFileInspector;

    public VideoFrameExtractorImpl(VideoFileInspectorImpl videoFileInspector) {
        this.videoFileInspector = videoFileInspector;
    }

    /**
     * 从视频中提取关键帧。
     * <p>
     * 使用 FFmpeg select 滤镜按时间点提取帧，转换为 JPEG 格式后 Base64 编码。
     * 临时文件在提取完成后自动清理。
     *
     * @param videoPath  视频文件路径
     * @param frameCount 要提取的帧数
     * @return 提取的图像帧列表，按位置排序
     */
    @Override
    public List<ImageFrame> extractFrames(Path videoPath, int frameCount) {
        validateInput(videoPath, frameCount);

        Duration duration = getVideoDuration(videoPath);
        List<Double> positions = calculateFramePositions(frameCount);

        List<ImageFrame> frames = new ArrayList<>();
        List<Path> tempFiles = new ArrayList<>();

        try {
            for (double position : positions) {
                Path tempFile = createTempFile();
                tempFiles.add(tempFile);

                extractFrameAtPosition(videoPath, duration, position, tempFile);
                ImageFrame frame = encodeFrame(tempFile, position);
                frames.add(frame);

                logger.debug("Extracted frame at position {}% from video: {}",
                        (int) (position * 100), videoPath.getFileName());
            }

            return frames;
        } finally {
            cleanupTempFiles(tempFiles);
        }
    }

    /**
     * 验证输入参数。
     */
    private void validateInput(Path videoPath, int frameCount) {
        if (!Files.exists(videoPath)) {
            throw new FileOperationException("Video file does not exist: " + videoPath);
        }
        if (frameCount < 1 || frameCount > 10) {
            throw new IllegalArgumentException("frameCount must be between 1 and 10");
        }
    }

    /**
     * 获取视频时长。
     */
    private Duration getVideoDuration(Path videoPath) {
        return videoFileInspector.inspect(videoPath).duration();
    }

    /**
     * 计算帧提取位置。
     * <p>
     * 均匀分布在 [0.0, 1.0] 范围内，包括两端点。
     */
    private List<Double> calculateFramePositions(int frameCount) {
        List<Double> positions = new ArrayList<>();
        if (frameCount == 1) {
            positions.add(0.5);
        } else {
            for (int i = 0; i < frameCount; i++) {
                positions.add((double) i / (frameCount - 1));
            }
        }
        return positions;
    }

    /**
     * 创建临时文件。
     */
    private Path createTempFile() {
        try {
            return Files.createTempFile(TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX);
        } catch (IOException e) {
            throw new FileOperationException("Failed to create temp file for frame extraction", e);
        }
    }

    /**
     * 使用 FFmpeg 提取指定位置的帧。
     * <p>
     * 命令格式: ffmpeg -ss {timestamp} -i {input} -vframes 1 -q:v 2 {output}
     * 使用 -ss 在 -i 之前实现快速定位（keyframe seeking）。
     */
    private void extractFrameAtPosition(Path videoPath, Duration duration, double position, Path outputPath) {
        double timestampSeconds = duration.toMillis() / 1000.0 * position;

        ProcessBuilder pb = new ProcessBuilder(
                FFMPEG_COMMAND,
                "-ss", String.format("%.3f", timestampSeconds),
                "-i", videoPath.toString(),
                "-vframes", "1",
                "-q:v", "2",
                "-y",
                outputPath.toString()
        );

        pb.redirectErrorStream(true);

        Process process = null;
        try {
            process = pb.start();
            boolean finished = process.waitFor(FFMPEG_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                throw new FileOperationException("FFmpeg timeout after " + FFMPEG_TIMEOUT_SECONDS + " seconds");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                String output = readProcessOutput(process);
                logger.error("FFmpeg failed with exit code {}: {}", exitCode, output);
                throw new FileOperationException("FFmpeg failed to extract frame: " + output);
            }

            if (!Files.exists(outputPath) || Files.size(outputPath) == 0) {
                throw new FileOperationException("FFmpeg produced no output file");
            }

        } catch (IOException e) {
            logger.error("Failed to execute FFmpeg for video: {}", videoPath, e);
            throw new FileOperationException("Failed to execute FFmpeg. Make sure ffmpeg is installed.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FileOperationException("FFmpeg interrupted", e);
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    /**
     * 将图像文件编码为 Base64。
     */
    private ImageFrame encodeFrame(Path imagePath, double position) {
        try {
            byte[] imageBytes = Files.readAllBytes(imagePath);
            String base64Data = Base64.getEncoder().encodeToString(imageBytes);
            return ImageFrame.jpeg(base64Data, position);
        } catch (IOException e) {
            logger.error("Failed to read frame image: {}", imagePath, e);
            throw new FileOperationException("Failed to encode frame image", e);
        }
    }

    /**
     * 清理临时文件。
     */
    private void cleanupTempFiles(List<Path> tempFiles) {
        for (Path tempFile : tempFiles) {
            try {
                if (Files.exists(tempFile)) {
                    Files.delete(tempFile);
                    logger.trace("Deleted temp file: {}", tempFile);
                }
            } catch (IOException e) {
                logger.warn("Failed to delete temp file: {}", tempFile, e);
            }
        }
    }

    /**
     * 读取进程输出。
     */
    private String readProcessOutput(Process process) throws IOException {
        StringBuilder output = new StringBuilder();
        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        return output.toString().trim();
    }
}