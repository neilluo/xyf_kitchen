package com.grace.platform.video.interfaces.rest.dto.response;

public record ServerChunkUploadResponse(
    String uploadId,
    int chunkIndex,
    int uploadedChunks,
    int totalChunks
) {
}