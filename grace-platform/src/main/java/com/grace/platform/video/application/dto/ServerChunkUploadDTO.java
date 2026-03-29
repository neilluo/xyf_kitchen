package com.grace.platform.video.application.dto;

public record ServerChunkUploadDTO(
    String uploadId,
    int chunkIndex,
    int uploadedChunks,
    int totalChunks
) {
}