package com.grace.platform.distribution.application.command;

import com.grace.platform.shared.domain.id.MetadataId;
import com.grace.platform.shared.domain.id.VideoId;

/**
 * 发布视频命令
 * <p>
 * 用于 {@code publish} 方法的参数封装，包含视频发布所需的信息。
 * </p>
 *
 * @param videoId       视频 ID
 * @param metadataId    元数据 ID
 * @param platform      目标平台标识（如 "youtube"）
 * @param privacyStatus 视频隐私状态（可选，默认 public）
 */
public record PublishCommand(
    VideoId videoId,
    MetadataId metadataId,
    String platform,
    String privacyStatus
) {
    /**
     * 创建发布命令（带默认隐私状态）
     *
     * @param videoId    视频 ID
     * @param metadataId 元数据 ID
     * @param platform   目标平台标识
     */
    public PublishCommand(VideoId videoId, MetadataId metadataId, String platform) {
        this(videoId, metadataId, platform, "public");
    }

    /**
     * 验证构造参数
     */
    public PublishCommand {
        if (videoId == null) {
            throw new IllegalArgumentException("VideoId must not be null");
        }
        if (metadataId == null) {
            throw new IllegalArgumentException("MetadataId must not be null");
        }
        if (platform == null || platform.isBlank()) {
            throw new IllegalArgumentException("Platform must not be blank");
        }
    }
}
