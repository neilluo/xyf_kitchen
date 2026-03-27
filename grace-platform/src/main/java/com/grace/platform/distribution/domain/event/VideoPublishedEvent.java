package com.grace.platform.distribution.domain.event;

import com.grace.platform.shared.domain.DomainEvent;
import com.grace.platform.shared.domain.id.VideoId;

/**
 * 视频已发布领域事件
 * <p>
 * 当视频成功发布到目标平台后发布，通知 Promotion 上下文进行社交媒体推广。
 * 由 DistributionApplicationService 在发布确认成功后发布。
 * </p>
 */
public class VideoPublishedEvent extends DomainEvent {

    private final VideoId videoId;
    private final String platform;
    private final String videoUrl;

    /**
     * 创建视频已发布事件
     *
     * @param videoId  视频 ID
     * @param platform 平台标识（如 "youtube"）
     * @param videoUrl 发布后的视频链接
     */
    public VideoPublishedEvent(VideoId videoId, String platform, String videoUrl) {
        super();
        if (videoId == null) {
            throw new IllegalArgumentException("VideoId must not be null");
        }
        if (platform == null || platform.isBlank()) {
            throw new IllegalArgumentException("Platform must not be blank");
        }
        if (videoUrl == null || videoUrl.isBlank()) {
            throw new IllegalArgumentException("VideoUrl must not be blank");
        }
        this.videoId = videoId;
        this.platform = platform;
        this.videoUrl = videoUrl;
    }

    /**
     * 获取视频 ID
     *
     * @return 视频 ID
     */
    public VideoId getVideoId() {
        return videoId;
    }

    /**
     * 获取平台标识
     *
     * @return 平台标识
     */
    public String getPlatform() {
        return platform;
    }

    /**
     * 获取发布后视频链接
     *
     * @return 视频链接
     */
    public String getVideoUrl() {
        return videoUrl;
    }

    @Override
    public String toString() {
        return String.format("VideoPublishedEvent[videoId=%s, platform=%s, videoUrl=%s]",
            videoId.value(), platform, videoUrl);
    }
}
