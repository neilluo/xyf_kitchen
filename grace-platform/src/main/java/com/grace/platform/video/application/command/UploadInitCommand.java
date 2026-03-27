package com.grace.platform.video.application.command;

import com.grace.platform.video.domain.VideoFormat;

/**
 * 初始化分片上传命令。
 * <p>
 * 用于 {@code initUpload} 方法的参数封装。
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
public record UploadInitCommand(
    String fileName,
    long fileSize,
    VideoFormat format
) {
}
