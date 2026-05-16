package com.grace.platform.video.domain;

import java.util.Set;

/**
 * 视频状态枚举
 * <p>
 * 状态流转：
 * UPLOADED → METADATA_GENERATED → READY_TO_PUBLISH → PUBLISHING → PUBLISHED → PROMOTION_DONE
 * PUBLISHING → PUBLISH_FAILED (可重试) → PUBLISHING
 * </p>
 * <p>
 * Effective Java Item 34: 使用枚举类型替代整型常量 —— 将状态机转换规则内聚到枚举自身，
 * 避免外部维护散落的 Map&lt;VideoStatus, Set&lt;VideoStatus&gt;&gt;。
 * </p>
 */
public enum VideoStatus {
    UPLOADED(Set.of()),               // allowedTransitions 延迟初始化（见 static 块）
    METADATA_FAILED(Set.of()),
    METADATA_GENERATED(Set.of()),
    READY_TO_PUBLISH(Set.of()),
    PUBLISHING(Set.of()),
    PUBLISHED(Set.of()),
    PUBLISH_FAILED(Set.of()),
    PROMOTION_DONE(Set.of()),
    DELETED(Set.of());

    private Set<VideoStatus> allowedTransitions;

    VideoStatus(Set<VideoStatus> allowedTransitions) {
        this.allowedTransitions = allowedTransitions;
    }

    // 枚举常量不能前向引用，所以在 static 块中赋值
    static {
        UPLOADED.allowedTransitions = Set.of(METADATA_GENERATED, METADATA_FAILED);
        METADATA_FAILED.allowedTransitions = Set.of(METADATA_GENERATED);
        METADATA_GENERATED.allowedTransitions = Set.of(READY_TO_PUBLISH);
        READY_TO_PUBLISH.allowedTransitions = Set.of(PUBLISHING);
        PUBLISHING.allowedTransitions = Set.of(PUBLISHED, PUBLISH_FAILED);
        PUBLISHED.allowedTransitions = Set.of(PROMOTION_DONE);
        PUBLISH_FAILED.allowedTransitions = Set.of(PUBLISHING);
    }

    /**
     * 判断是否可以转换到目标状态
     */
    public boolean canTransitionTo(VideoStatus target) {
        return allowedTransitions.contains(target);
    }

    /**
     * 获取允许的目标状态集合（不可变）
     */
    public Set<VideoStatus> getAllowedTransitions() {
        return allowedTransitions;
    }
}
