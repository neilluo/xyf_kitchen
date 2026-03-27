package com.grace.platform.shared.domain.id;

import java.util.UUID;

public record ChannelId(String value) {
    public ChannelId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ChannelId value must not be blank");
        }
    }

    public static ChannelId generate() {
        return new ChannelId("ch_" + UUID.randomUUID().toString().replace("-", ""));
    }

    @Override
    public String toString() {
        return value;
    }
}
