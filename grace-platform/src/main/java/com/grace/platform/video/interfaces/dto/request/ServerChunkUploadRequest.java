package com.grace.platform.video.interfaces.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ServerChunkUploadRequest(
    @NotNull(message = "分片索引不能为空")
    @Min(value = 0, message = "分片索引最小为0")
    Integer chunkIndex
) {
}