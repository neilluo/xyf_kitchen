package com.grace.platform.video.infrastructure.file;

import com.grace.platform.shared.ErrorCode;
import com.grace.platform.shared.infrastructure.exception.BusinessRuleViolationException;
import com.grace.platform.video.domain.VideoFileInfo;
import com.grace.platform.video.domain.VideoFileInspector;
import com.grace.platform.video.domain.VideoFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
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

    /**
     * 检查视频文件，提取元信息。
     * <p>
     * 使用 ffprobe 命令行工具提取视频时长、格式等信息。
     *
     * @param filePath 视频文件路径
     * @return 视频文件信息（包含文件名、文件大小、格式、时长等）
     * @throws InfrastructureException 如果文件不存在或 ffprobe 执行失败
     */
    @Override
    public VideoFileInfo inspect(Path filePath) {
        if (!Files.exists(filePath)) {
            throw new RuntimeException("Video file does not exist: " + filePath);
        }

        String fileName = filePath.getFileName().toString();
        long fileSize;
        try {
            fileSize = Files.size(filePath);
        } catch (IOException e) {
            logger.error("Failed to get file size: {}", filePath, e);
            throw new RuntimeException("Failed to get file size: " + filePath, e);
        }

        VideoFormat format = extractFormat(fileName);
        Duration duration = extractDuration(filePath);

        logger.debug("Video file inspected: {} (size: {}, format: {}, duration: {}s)",
                fileName, fileSize, format, duration.getSeconds());

        return new VideoFileInfo(fileName, fileSize, format, duration);
    }

    /**
     * 从文件名提取视频格式。
     *
     * @param fileName 文件名
     * @return 视频格式枚举
     */
    private VideoFormat extractFormat(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toUpperCase();
        try {
            return VideoFormat.valueOf(extension);
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleViolationException(ErrorCode.UNSUPPORTED_VIDEO_FORMAT, extension);
        }
    }

    /**
     * 使用 ffprobe 提取视频时长。
     * <p>
     * 命令格式: ffprobe -v error -show_entries format=duration -of csv=p=0 <file>
     *
     * @param filePath 视频文件路径
     * @return 视频时长
     */
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
                throw new RuntimeException("ffprobe timeout after " + FFPROBE_TIMEOUT_SECONDS + " seconds");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                logger.error("ffprobe failed with exit code {}: {}", exitCode, output);
                throw new RuntimeException("ffprobe failed: " + output);
            }

            double durationSeconds = parseDuration(output);
            return Duration.ofMillis((long) (durationSeconds * 1000));

        } catch (IOException e) {
            logger.error("Failed to execute ffprobe for file: {}", filePath, e);
            throw new RuntimeException("Failed to execute ffprobe. Make sure ffmpeg is installed.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("ffprobe interrupted for file: {}", filePath, e);
            throw new RuntimeException("ffprobe interrupted", e);
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    /**
     * 读取进程输出。
     *
     * @param process 进程
     * @return 输出字符串
     * @throws IOException 如果读取失败
     */
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

    /**
     * 解析时长字符串。
     *
     * @param output ffprobe 输出
     * @return 时长秒数
     */
    private double parseDuration(String output) {
        try {
            return Double.parseDouble(output);
        } catch (NumberFormatException e) {
            logger.warn("Failed to parse duration: '{}', defaulting to 0", output);
            return 0.0;
        }
    }
}
