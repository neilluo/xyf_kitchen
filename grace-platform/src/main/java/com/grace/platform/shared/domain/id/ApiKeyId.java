package com.grace.platform.shared.domain.id;

import java.util.UUID;

public record ApiKeyId(String value) {
    public ApiKeyId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ApiKeyId value must not be blank");
        }
    }

    public static ApiKeyId generate() {
        return new ApiKeyId("key_" + UUID.randomUUID().toString().replace("-", ""));
    }

    @Override
    public String toString() {
        return value;
    }
}
