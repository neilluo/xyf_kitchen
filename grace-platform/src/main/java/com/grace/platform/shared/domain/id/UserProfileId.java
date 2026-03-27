package com.grace.platform.shared.domain.id;

import java.util.UUID;

public record UserProfileId(String value) {
    public UserProfileId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("UserProfileId value must not be blank");
        }
    }

    public static UserProfileId generate() {
        return new UserProfileId("user_" + UUID.randomUUID().toString().replace("-", ""));
    }

    @Override
    public String toString() {
        return value;
    }
}
