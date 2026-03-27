package com.grace.platform.shared.infrastructure.encryption;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BcryptApiKeyHashService implements ApiKeyHashService {
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public String hash(String rawKey) {
        return encoder.encode(rawKey);
    }

    @Override
    public boolean verify(String rawKey, String hashedKey) {
        return encoder.matches(rawKey, hashedKey);
    }
}
