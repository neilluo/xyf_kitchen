package com.grace.platform.shared.infrastructure.encryption;

public interface ApiKeyHashService {
    String hash(String rawKey);
    boolean verify(String rawKey, String hashedKey);
}
