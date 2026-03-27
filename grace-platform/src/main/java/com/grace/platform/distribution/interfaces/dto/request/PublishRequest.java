package com.grace.platform.distribution.interfaces.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 发布视频请求 DTO
 * <p>
 * 对应 API D1 的请求体，用于请求将视频发布到指定平台。
 * </p>
 *
 * @param videoId       视频 ID（必填）
 * @param metadataId    元数据 ID（必填）
 * @param platform      平台标识，如 "youtube"（必填）
 * @param privacyStatus 视频隐私状态，可选值：public(默认) / unlisted / private
 */
public record PublishRequest(
    @NotBlank(message = "视频 ID 不能为空")
    String videoId,

    @NotBlank(message = "元数据 ID 不能为空")
    String metadataId,

    @NotBlank(message = "平台标识不能为空")
    String platform,

    String privacyStatus
) {
    /**
     * 获取隐私状态，默认为 "public"
     *
     * @return 隐私状态
     */
    public String privacyStatus() {
        return privacyStatus == null || privacyStatus.isBlank() ? "public" : privacyStatus;
    }
}
