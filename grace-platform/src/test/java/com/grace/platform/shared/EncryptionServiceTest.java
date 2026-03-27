package com.grace.platform.shared;

import com.grace.platform.shared.infrastructure.encryption.AesGcmEncryptionService;
import com.grace.platform.shared.infrastructure.encryption.EncryptionService;
import com.grace.platform.shared.infrastructure.exception.EncryptionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EncryptionServiceTest {

    private EncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        // Generate a 256-bit (32 bytes) key for testing
        // Base64 encoded 32 bytes = 44 characters
        String testKey = Base64.getEncoder().encodeToString(new byte[32]);
        encryptionService = new AesGcmEncryptionService(testKey);
    }

    @Test
    @DisplayName("加密后解密应恢复原文")
    void shouldDecryptToOriginalPlaintext() {
        // Given
        String plaintext = "Hello, World!";

        // When
        String encrypted = encryptionService.encrypt(plaintext);
        String decrypted = encryptionService.decrypt(encrypted);

        // Then
        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    @DisplayName("加密长文本后解密应恢复原文")
    void shouldDecryptLongTextToOriginal() {
        // Given
        String plaintext = "This is a longer text with multiple words and special characters: !@#$%^&*()_+-=[]{}|;':\",./<>?";

        // When
        String encrypted = encryptionService.encrypt(plaintext);
        String decrypted = encryptionService.decrypt(encrypted);

        // Then
        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    @DisplayName("加密Unicode文本后解密应恢复原文")
    void shouldDecryptUnicodeTextToOriginal() {
        // Given
        String plaintext = "你好世界！こんにちは世界！Bonjour le monde! 🌍";

        // When
        String encrypted = encryptionService.encrypt(plaintext);
        String decrypted = encryptionService.decrypt(encrypted);

        // Then
        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    @DisplayName("相同明文加密两次应产生不同密文（随机IV）")
    void shouldProduceDifferentCiphertextForSamePlaintext() {
        // Given
        String plaintext = "Test message";

        // When
        String encrypted1 = encryptionService.encrypt(plaintext);
        String encrypted2 = encryptionService.encrypt(plaintext);

        // Then
        assertThat(encrypted1).isNotEqualTo(encrypted2);
        // Both should decrypt to the same plaintext
        assertThat(encryptionService.decrypt(encrypted1)).isEqualTo(plaintext);
        assertThat(encryptionService.decrypt(encrypted2)).isEqualTo(plaintext);
    }

    @Test
    @DisplayName("加密null应返回null")
    void shouldReturnNullForNullInput() {
        // When
        String result = encryptionService.encrypt(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("解密null应返回null")
    void shouldReturnNullForNullCiphertext() {
        // When
        String result = encryptionService.decrypt(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("加密空字符串应返回空字符串")
    void shouldReturnEmptyStringForEmptyInput() {
        // When
        String result = encryptionService.encrypt("");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("解密空字符串应返回空字符串")
    void shouldReturnEmptyStringForEmptyCiphertext() {
        // When
        String result = encryptionService.decrypt("");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("解密无效密文应抛出EncryptionException")
    void shouldThrowExceptionForInvalidCiphertext() {
        // Given
        String invalidCiphertext = "invalid-base64-data!!!";

        // Then
        assertThatThrownBy(() -> encryptionService.decrypt(invalidCiphertext))
            .isInstanceOf(EncryptionException.class)
            .hasMessageContaining("Decryption failed");
    }

    @Test
    @DisplayName("解密篡改后的密文应抛出EncryptionException")
    void shouldThrowExceptionForTamperedCiphertext() {
        // Given
        String plaintext = "Sensitive data";
        String encrypted = encryptionService.encrypt(plaintext);
        
        // Tamper with the ciphertext by changing a character
        String tampered = encrypted.substring(0, encrypted.length() - 1) + "X";

        // Then
        assertThatThrownBy(() -> encryptionService.decrypt(tampered))
            .isInstanceOf(EncryptionException.class)
            .hasMessageContaining("Decryption failed");
    }

    @Test
    @DisplayName("密文应为Base64格式")
    void shouldProduceBase64EncodedCiphertext() {
        // Given
        String plaintext = "Test data";

        // When
        String encrypted = encryptionService.encrypt(plaintext);

        // Then
        // Base64 strings only contain A-Z, a-z, 0-9, +, /, and =
        assertThat(encrypted).matches("^[A-Za-z0-9+/=]+$");
    }

    @Test
    @DisplayName("密文长度应大于IV长度（12字节Base64编码后约16字符）")
    void shouldProduceCiphertextLongerThanIv() {
        // Given
        String plaintext = "X";

        // When
        String encrypted = encryptionService.encrypt(plaintext);

        // Then
        // 12 bytes IV = 16 Base64 chars, plus at least some ciphertext
        assertThat(encrypted.length()).isGreaterThan(16);
    }
}
