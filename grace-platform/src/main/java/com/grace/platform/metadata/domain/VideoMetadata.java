package com.grace.platform.metadata.domain;

import com.grace.platform.shared.ErrorCode;
import com.grace.platform.shared.domain.id.MetadataId;
import com.grace.platform.shared.domain.id.VideoId;
import com.grace.platform.shared.infrastructure.exception.BusinessRuleViolationException;

import java.time.LocalDateTime;
import java.util.List;

public class VideoMetadata {

    private final MetadataId id;
    private final VideoId videoId;
    private String title;
    private String description;
    private List<String> tags;
    private MetadataSource source;
    private boolean confirmed;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public VideoMetadata(
            MetadataId id,
            VideoId videoId,
            String title,
            String description,
            List<String> tags,
            MetadataSource source,
            boolean confirmed,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.videoId = videoId;
        this.title = title;
        this.description = description;
        this.tags = tags;
        this.source = source;
        this.confirmed = confirmed;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static VideoMetadata create(
            VideoId videoId,
            String title,
            String description,
            List<String> tags,
            MetadataSource source
    ) {
        MetadataId id = MetadataId.generate();
        LocalDateTime now = LocalDateTime.now();
        VideoMetadata metadata = new VideoMetadata(
                id,
                videoId,
                title,
                description,
                tags,
                source,
                false,
                now,
                now
        );
        metadata.validate();
        return metadata;
    }

    public void validate() {
        if (title == null || title.isBlank() || title.length() > 100) {
            throw new BusinessRuleViolationException(ErrorCode.INVALID_METADATA, "标题不能为空且不超过100字符");
        }
        if (description != null && description.length() > 5000) {
            throw new BusinessRuleViolationException(ErrorCode.INVALID_METADATA, "描述不超过5000字符");
        }
        if (tags == null || tags.size() < 5 || tags.size() > 15) {
            throw new BusinessRuleViolationException(ErrorCode.INVALID_METADATA, "标签数量需在5-15之间");
        }
    }

    public void update(String title, String description, List<String> tags) {
        if (this.confirmed) {
            throw new BusinessRuleViolationException(ErrorCode.METADATA_ALREADY_CONFIRMED, "已确认的元数据不可再编辑");
        }
        if (title != null) {
            this.title = title;
        }
        if (description != null) {
            this.description = description;
        }
        if (tags != null) {
            this.tags = tags;
        }
        this.source = (this.source == MetadataSource.AI_GENERATED) ? MetadataSource.AI_EDITED : this.source;
        this.updatedAt = LocalDateTime.now();
        validate();
    }

    public void confirm() {
        if (this.confirmed) {
            throw new BusinessRuleViolationException(ErrorCode.METADATA_ALREADY_CONFIRMED, "元数据已被确认");
        }
        validate();
        this.confirmed = true;
        this.updatedAt = LocalDateTime.now();
    }

    public MetadataId getId() {
        return id;
    }

    public VideoId getVideoId() {
        return videoId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getTags() {
        return tags;
    }

    public MetadataSource getSource() {
        return source;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
