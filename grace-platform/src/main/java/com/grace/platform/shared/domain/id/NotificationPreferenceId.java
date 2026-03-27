package com.grace.platform.shared.domain.id;

import java.util.UUID;

public record NotificationPreferenceId(String value) {
    public NotificationPreferenceId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("NotificationPreferenceId value must not be blank");
        }
    }

    public static NotificationPreferenceId generate() {
        return new NotificationPreferenceId("notif_" + UUID.randomUUID().toString().replace("-", ""));
    }

    @Override
    public String toString() {
        return value;
    }
}
