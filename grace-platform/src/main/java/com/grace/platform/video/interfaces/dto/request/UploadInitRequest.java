package com.grace.platform.video.interfaces.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 初始化上传请求 DTO。
 * <p>
 * 对应 API B1 请求体。
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
public record UploadInitRequest(
    @NotBlank(message = "文件名不能为空")
    @Size(max = 500, message = "文件名长度不能超过500字符")
    String fileName,

    @NotNull(message = "文件大小不能为空")
    @Positive(message = "文件大小必须大于0")
    long fileSize,

    @NotBlank(message = "文件格式不能为空")
    @Size(max = 10, message = "格式长度不能超过10字符")
    String format
) {
}
