package com.grace.platform.shared.domain.id;

import java.util.UUID;

public record MetadataId(String value) {
    public MetadataId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("MetadataId value must not be blank");
        }
    }

    public static MetadataId generate() {
        return new MetadataId("meta_" + UUID.randomUUID().toString().replace("-", ""));
    }

    @Override
    public String toString() {
        return value;
    }
}
