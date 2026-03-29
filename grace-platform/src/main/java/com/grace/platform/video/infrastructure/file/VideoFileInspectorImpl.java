package com.grace.platform.video.infrastructure.file;

import com.grace.platform.shared.ErrorCode;
import com.grace.platform.shared.infrastructure.exception.BusinessRuleViolationException;
import com.grace.platform.shared.infrastructure.exception.FileOperationException;
import com.grace.platform.video.domain.VideoFileInfo;
import com.grace.platform.video.domain.VideoFileInspector;
import com.grace.platform.video.domain.VideoFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 视频文件检查器实现。
 * <p>
 * 使用 ProcessBuilder 调用 ffprobe 提取视频文件的元信息（时长、格式等）。
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
@Component
public class VideoFileInspectorImpl implements VideoFileInspector {

    private static final Logger logger = LoggerFactory.getLogger(VideoFileInspectorImpl.class);

    private static final String FFPROBE_COMMAND = "ffprobe";
    private static final long FFPROBE_TIMEOUT_SECONDS = 30;
    private static final long DOWNLOAD_TIMEOUT_SECONDS = 60;
    private static final long DOWNLOAD_MAX_SIZE = 50 * 1024 * 1024;

    private final Path tempDir;

    public VideoFileInspectorImpl() {
        this.tempDir = Path.of(System.getProperty("java.io.tmpdir"), "grace-video-inspect");
        try {
            Files.createDirectories(tempDir);
        } catch (IOException e) {
            logger.warn("Failed to create temp directory for video inspection: {}", tempDir, e);
        }
    }

    @Override
    public VideoFileInfo inspect(Path filePath) {
        if (!Files.exists(filePath)) {
            throw new FileOperationException("Video file does not exist: " + filePath);
        }

        String fileName = filePath.getFileName().toString();
        long fileSize;
        try {
            fileSize = Files.size(filePath);
        } catch (IOException e) {
            logger.error("Failed to get file size: {}", filePath, e);
            throw new FileOperationException("Failed to get file size: " + filePath, e);
        }

        VideoFormat format = extractFormat(fileName);
        Duration duration = extractDuration(filePath);

        logger.debug("Video file inspected: {} (size: {}, format: {}, duration: {}s)",
                fileName, fileSize, format, duration.getSeconds());

        return new VideoFileInfo(fileName, fileSize, format, duration);
    }

    @Override
    public VideoFileInfo inspectFromUrl(String url) {
        logger.debug("Inspecting video from URL: {}", url);

        Path tempFile = downloadToTempFile(url);

        try {
            VideoFileInfo info = inspect(tempFile);
            String fileName = extractFileNameFromUrl(url);
            return new VideoFileInfo(fileName, info.fileSize(), info.format(), info.duration());
        } finally {
            deleteTempFile(tempFile);
        }
    }

    private Path downloadToTempFile(String url) {
        try {
            URL videoUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) videoUrl.openConnection();
            connection.setConnectTimeout((int) DOWNLOAD_TIMEOUT_SECONDS * 1000);
            connection.setReadTimeout((int) DOWNLOAD_TIMEOUT_SECONDS * 1000);
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new FileOperationException("Failed to download video from URL: HTTP " + responseCode);
            }

            String fileName = extractFileNameFromUrl(url);
            Path tempFile = tempDir.resolve("inspect_" + System.currentTimeMillis() + "_" + fileName);

            try (InputStream inputStream = connection.getInputStream()) {
                long copied = Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
                if (copied > DOWNLOAD_MAX_SIZE) {
                    Files.deleteIfExists(tempFile);
                    throw new FileOperationException("Video file too large for inspection: " + copied + " bytes");
                }
            }

            logger.debug("Video downloaded to temp file: {} ({} bytes)", tempFile, Files.size(tempFile));
            return tempFile;

        } catch (IOException e) {
            logger.error("Failed to download video from URL: {}", url, e);
            throw new FileOperationException("Failed to download video from URL: " + url, e);
        }
    }

    private void deleteTempFile(Path tempFile) {
        try {
            Files.deleteIfExists(tempFile);
            logger.debug("Deleted temp file: {}", tempFile);
        } catch (IOException e) {
            logger.warn("Failed to delete temp file: {}", tempFile, e);
        }
    }

    private String extractFileNameFromUrl(String url) {
        int lastSlash = url.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < url.length() - 1) {
            return url.substring(lastSlash + 1);
        }
        return "video_" + System.currentTimeMillis();
    }

    private VideoFormat extractFormat(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toUpperCase();
        try {
            return VideoFormat.valueOf(extension);
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleViolationException(ErrorCode.UNSUPPORTED_VIDEO_FORMAT, extension);
        }
    }

    private Duration extractDuration(Path filePath) {
        ProcessBuilder pb = new ProcessBuilder(
                FFPROBE_COMMAND,
                "-v", "error",
                "-show_entries", "format=duration",
                "-of", "csv=p=0",
                filePath.toString()
        );

        pb.redirectErrorStream(true);

        Process process = null;
        try {
            process = pb.start();

            String output = readProcessOutput(process);

            boolean finished = process.waitFor(FFPROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                logger.error("ffprobe timeout after {} seconds for file: {}",
                        FFPROBE_TIMEOUT_SECONDS, filePath);
                throw new FileOperationException("ffprobe timeout after " + FFPROBE_TIMEOUT_SECONDS + " seconds");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                logger.error("ffprobe failed with exit code {}: {}", exitCode, output);
                throw new FileOperationException("ffprobe failed: " + output);
            }

            double durationSeconds = parseDuration(output);
            return Duration.ofMillis((long) (durationSeconds * 1000));

        } catch (IOException e) {
            logger.error("Failed to execute ffprobe for file: {}", filePath, e);
            throw new FileOperationException("Failed to execute ffprobe. Make sure ffmpeg is installed.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("ffprobe interrupted for file: {}", filePath, e);
            throw new FileOperationException("ffprobe interrupted", e);
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    private String readProcessOutput(Process process) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
        }
        return output.toString().trim();
    }

    private double parseDuration(String output) {
        try {
            return Double.parseDouble(output);
        } catch (NumberFormatException e) {
            logger.warn("Failed to parse duration: '{}', defaulting to 0", output);
            return 0.0;
        }
    }
}
