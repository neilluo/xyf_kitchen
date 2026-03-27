package com.grace.platform.distribution.domain;

import java.util.List;
import java.util.Objects;

/**
 * 视频元数据值对象
 * <p>
 * 表示视频发布所需的元数据信息，包含标题、描述、标签等。
 * </p>
 *
 * @param title 视频标题
 * @param description 视频描述
 * @param tags 标签列表
 */
public record VideoMetadata(
    String title,
    String description,
    List<String> tags
) {
    /**
     * 创建视频元数据值对象
     *
     * @param title 视频标题，不能为空
     * @param description 视频描述
     * @param tags 标签列表
     */
    public VideoMetadata {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        // description 可以为 null 或空
        // tags 可以为 null，防御性复制
        if (tags != null) {
            tags = List.copyOf(tags);
        }
    }

    /**
     * 获取标签列表（防御性复制）
     *
     * @return 标签列表，不会返回 null
     */
    public List<String> tags() {
        return tags != null ? tags : List.of();
    }
}
