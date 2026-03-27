package com.grace.platform.shared.domain.id;

import java.util.UUID;

public record PublishRecordId(String value) {
    public PublishRecordId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("PublishRecordId value must not be blank");
        }
    }

    public static PublishRecordId generate() {
        return new PublishRecordId("pub_" + UUID.randomUUID().toString().replace("-", ""));
    }

    @Override
    public String toString() {
        return value;
    }
}
