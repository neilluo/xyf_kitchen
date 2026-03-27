package com.grace.platform.video.domain;

import java.time.Duration;
import java.util.Objects;

/**
 * 视频文件信息值对象
 * <p>
 * 用于封装视频文件的基本元数据信息，不包含持久化标识。
 * </p>
 *
 * @param fileName 原始文件名
 * @param fileSize 文件字节数（必须 > 0）
 * @param format   视频格式
 * @param duration 视频时长（必须 >= 0）
 */
public record VideoFileInfo(
        String fileName,
        long fileSize,
        VideoFormat format,
        Duration duration
) {

    /**
     * 创建视频文件信息值对象
     *
     * @param fileName 原始文件名，不能为空
     * @param fileSize 文件字节数，必须大于 0
     * @param format   视频格式，不能为 null
     * @param duration 视频时长，不能为 null 且必须大于等于 0
     * @throws NullPointerException     如果 fileName、format 或 duration 为 null
     * @throws IllegalArgumentException 如果 fileSize <= 0 或 duration 为负数
     */
    public VideoFileInfo {
        Objects.requireNonNull(fileName, "fileName must not be null");
        Objects.requireNonNull(format, "format must not be null");
        Objects.requireNonNull(duration, "duration must not be null");

        if (fileSize <= 0) {
            throw new IllegalArgumentException("fileSize must be greater than 0");
        }
        if (duration.isNegative()) {
            throw new IllegalArgumentException("duration must not be negative");
        }
    }

    /**
     * 获取视频时长（秒数）
     *
     * @return 时长秒数
     */
    public long durationSeconds() {
        return duration.getSeconds();
    }
}
