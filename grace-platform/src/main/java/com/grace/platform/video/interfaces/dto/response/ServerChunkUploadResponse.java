package com.grace.platform.video.interfaces.dto.response;

public record ServerChunkUploadResponse(
    String uploadId,
    int chunkIndex,
    int uploadedChunks,
    int totalChunks
) {
}