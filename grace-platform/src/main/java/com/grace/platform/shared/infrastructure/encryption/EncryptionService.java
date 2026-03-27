package com.grace.platform.shared.infrastructure.encryption;

public interface EncryptionService {
    String encrypt(String plaintext);
    String decrypt(String ciphertext);
}
