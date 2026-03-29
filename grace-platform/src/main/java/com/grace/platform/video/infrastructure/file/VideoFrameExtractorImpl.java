package com.grace.platform.video.infrastructure.file;

import com.grace.platform.shared.infrastructure.exception.FileOperationException;
import com.grace.platform.video.domain.ImageFrame;
import com.grace.platform.video.domain.VideoFrameExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * 视频帧提取器实现。
 * <p>
 * 使用 FFmpeg 从视频中提取关键帧，转换为 Base64 编码的图像。
 * 默认提取 3 帧：开头(0%)、中间(50%)、结尾(100%)。
 * <p>
 * 支持两种输入方式：
 * - 本地文件路径：直接从本地文件提取
 * - URL（OSS/HTTP）：下载到临时文件后提取，完成后自动删除临时文件
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
    private static final long DOWNLOAD_TIMEOUT_SECONDS = 120;
    private static final String TEMP_FILE_PREFIX = "grace-frame-";
    private static final String TEMP_FILE_SUFFIX = ".jpg";
    private static final String VIDEO_TEMP_PREFIX = "grace-video-";
    private static final int MAX_VIDEO_DOWNLOAD_SIZE_MB = 500;

    private final VideoFileInspectorImpl videoFileInspector;
    private final Path tempDir;

    public VideoFrameExtractorImpl(VideoFileInspectorImpl videoFileInspector) {
        this.videoFileInspector = videoFileInspector;
        this.tempDir = Path.of(System.getProperty("java.io.tmpdir"), "grace-frame-extract");
        try {
            Files.createDirectories(tempDir);
        } catch (IOException e) {
            logger.warn("Failed to create temp directory for frame extraction: {}", tempDir, e);
        }
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
     * 从 URL（OSS 或 HTTP）提取视频关键帧。
     * <p>
     * 先将视频下载到临时文件，然后使用 FFmpeg 提取关键帧，
     * 提取完成后自动删除所有临时文件。
     *
     * @param videoUrl   视频 URL（OSS 或 HTTP）
     * @param frameCount 要提取的帧数
     * @return 提取的图像帧列表，按位置排序
     */
    @Override
    public List<ImageFrame> extractFramesFromUrl(String videoUrl, int frameCount) {
        validateUrl(videoUrl, frameCount);

        logger.debug("Extracting {} frames from URL: {}", frameCount, videoUrl);

        Path videoTempFile = downloadVideoToTempFile(videoUrl);
        List<Path> frameTempFiles = new ArrayList<>();

        try {
            Duration duration = getVideoDuration(videoTempFile);
            List<Double> positions = calculateFramePositions(frameCount);

            List<ImageFrame> frames = new ArrayList<>();
            for (double position : positions) {
                Path tempFile = createTempFile();
                frameTempFiles.add(tempFile);

                extractFrameAtPosition(videoTempFile, duration, position, tempFile);
                ImageFrame frame = encodeFrame(tempFile, position);
                frames.add(frame);

                logger.debug("Extracted frame at position {}% from URL: {}",
                        (int) (position * 100), videoUrl);
            }

            return frames;
        } finally {
            cleanupTempFiles(frameTempFiles);
            deleteTempFile(videoTempFile);
        }
    }

    /**
     * 验证 URL 输入参数。
     */
    void validateUrl(String videoUrl, int frameCount) {
        if (videoUrl == null || videoUrl.isBlank()) {
            throw new IllegalArgumentException("videoUrl must not be blank");
        }
        if (!videoUrl.startsWith("http://") && !videoUrl.startsWith("https://")) {
            throw new IllegalArgumentException("videoUrl must be a valid HTTP/HTTPS URL");
        }
        if (frameCount < 1 || frameCount > 10) {
            throw new IllegalArgumentException("frameCount must be between 1 and 10");
        }
    }

    /**
     * 从 URL 下载视频到临时文件。
     */
    private Path downloadVideoToTempFile(String videoUrl) {
        try {
            URL url = new URL(videoUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout((int) DOWNLOAD_TIMEOUT_SECONDS * 1000);
            connection.setReadTimeout((int) DOWNLOAD_TIMEOUT_SECONDS * 1000);
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new FileOperationException("Failed to download video from URL: HTTP " + responseCode);
            }

            String fileName = extractFileNameFromUrl(videoUrl);
            Path tempFile = tempDir.resolve(VIDEO_TEMP_PREFIX + System.currentTimeMillis() + "_" + fileName);

            try (InputStream inputStream = connection.getInputStream()) {
                long copied = Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
                logger.debug("Video downloaded to temp file: {} ({} bytes)", tempFile, copied);

                if (copied > MAX_VIDEO_DOWNLOAD_SIZE_MB * 1024 * 1024) {
                    Files.deleteIfExists(tempFile);
                    throw new FileOperationException("Video file too large for frame extraction: " 
                        + (copied / 1024 / 1024) + " MB exceeds limit of " + MAX_VIDEO_DOWNLOAD_SIZE_MB + " MB");
                }
            }

            return tempFile;

        } catch (IOException e) {
            logger.error("Failed to download video from URL: {}", videoUrl, e);
            throw new FileOperationException("Failed to download video from URL: " + videoUrl, e);
        }
    }

    /**
     * 从 URL 提取文件名。
     */
    private String extractFileNameFromUrl(String url) {
        int lastSlash = url.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < url.length() - 1) {
            String fileName = url.substring(lastSlash + 1);
            int queryIndex = fileName.indexOf('?');
            if (queryIndex > 0) {
                fileName = fileName.substring(0, queryIndex);
            }
            return fileName;
        }
        return "video_" + System.currentTimeMillis();
    }

    /**
     * 删除临时文件。
     */
    private void deleteTempFile(Path tempFile) {
        try {
            if (Files.exists(tempFile)) {
                Files.delete(tempFile);
                logger.trace("Deleted temp file: {}", tempFile);
            }
        } catch (IOException e) {
            logger.warn("Failed to delete temp file: {}", tempFile, e);
        }
    }

    /**
     * 验证输入参数。
     */
    void validateInput(Path videoPath, int frameCount) {
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
    List<Double> calculateFramePositions(int frameCount) {
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
     * 获取临时文件前缀。
     */
    String getTempFilePrefix() {
        return TEMP_FILE_PREFIX;
    }

    /**
     * 获取临时文件后缀。
     */
    String getTempFileSuffix() {
        return TEMP_FILE_SUFFIX;
    }

    /**
     * 构建 FFmpeg 命令参数列表。
     *
     * @param videoPath  视频文件路径
     * @param duration   视频时长
     * @param position   帧位置（0.0-1.0）
     * @param outputPath 输出文件路径
     * @return FFmpeg 命令参数列表
     */
    List<String> buildFfmpegCommand(Path videoPath, Duration duration, double position, Path outputPath) {
        double timestampSeconds = duration.toMillis() / 1000.0 * position;
        return List.of(
                FFMPEG_COMMAND,
                "-ss", String.format("%.3f", timestampSeconds),
                "-i", videoPath.toString(),
                "-vframes", "1",
                "-q:v", "2",
                "-y",
                outputPath.toString()
        );
    }

    /**
     * 创建临时文件。
     */
    private Path createTempFile() {
        try {
            return Files.createTempFile(tempDir, TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX);
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