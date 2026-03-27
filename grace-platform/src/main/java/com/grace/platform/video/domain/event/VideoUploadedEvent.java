package com.grace.platform.video.domain.event;

import com.grace.platform.shared.domain.DomainEvent;
import com.grace.platform.shared.domain.id.VideoId;

public class VideoUploadedEvent extends DomainEvent {
    private final VideoId videoId;
    private final String fileName;
    private final long fileSize;
    private final String format;

    public VideoUploadedEvent(VideoId videoId, String fileName, long fileSize, String format) {
        super();
        this.videoId = videoId;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.format = format;
    }

    public VideoId getVideoId() {
        return videoId;
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getFormat() {
        return format;
    }
}
