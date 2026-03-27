package com.grace.platform.distribution.domain;

import java.nio.file.Path;
import java.util.Objects;

/**
 * 视频文件值对象
 * <p>
 * 表示待发布的视频文件信息，包含文件路径和基本信息。
 * </p>
 *
 * @param filePath 视频文件路径
 * @param fileName 原始文件名
 * @param fileSize 文件大小（字节）
 */
public record VideoFile(
    Path filePath,
    String fileName,
    long fileSize
) {
    /**
     * 创建视频文件值对象
     *
     * @param filePath 视频文件路径，不能为 null
     * @param fileName 原始文件名，不能为空
     * @param fileSize 文件大小，必须大于 0
     */
    public VideoFile {
        Objects.requireNonNull(filePath, "filePath must not be null");
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("fileName must not be blank");
        }
        if (fileSize <= 0) {
            throw new IllegalArgumentException("fileSize must be greater than 0");
        }
    }
}
