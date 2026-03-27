package com.grace.platform.shared.domain.id;

import java.util.UUID;

public record VideoId(String value) {
    public VideoId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("VideoId value must not be blank");
        }
    }

    public static VideoId generate() {
        return new VideoId("vid_" + UUID.randomUUID().toString().replace("-", ""));
    }

    @Override
    public String toString() {
        return value;
    }
}
