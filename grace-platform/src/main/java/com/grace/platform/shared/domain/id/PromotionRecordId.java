package com.grace.platform.shared.domain.id;

import java.util.UUID;

public record PromotionRecordId(String value) {
    public PromotionRecordId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("PromotionRecordId value must not be blank");
        }
    }

    public static PromotionRecordId generate() {
        return new PromotionRecordId("promo_" + UUID.randomUUID().toString().replace("-", ""));
    }

    @Override
    public String toString() {
        return value;
    }
}
