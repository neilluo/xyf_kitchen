package com.grace.platform.shared.domain.id;

import java.util.UUID;

public record OAuthTokenId(String value) {
    public OAuthTokenId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("OAuthTokenId value must not be blank");
        }
    }

    public static OAuthTokenId generate() {
        return new OAuthTokenId("oauth_" + UUID.randomUUID().toString().replace("-", ""));
    }

    @Override
    public String toString() {
        return value;
    }
}
