package com.grace.platform.video.application.command;

import com.grace.platform.video.domain.VideoFormat;

public record ServerUploadInitCommand(
    String fileName,
    long fileSize,
    VideoFormat format
) {
}