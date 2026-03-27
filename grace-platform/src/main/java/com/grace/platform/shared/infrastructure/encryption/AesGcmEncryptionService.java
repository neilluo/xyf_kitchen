package com.grace.platform.shared.infrastructure.encryption;

import com.grace.platform.shared.infrastructure.exception.EncryptionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class AesGcmEncryptionService implements EncryptionService {

    // 从配置注入 grace.encryption.master-key
    private final byte[] masterKey;  // 32 bytes (256 bit)

    // AES-256-GCM 参数：
    // - IV 长度：12 bytes
    // - Tag 长度：128 bits
    // - 存储格式：Base64(IV + ciphertext + tag)
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;

    public AesGcmEncryptionService(@Value("${grace.encryption.master-key}") String masterKeyBase64) {
        this.masterKey = Base64.getDecoder().decode(masterKeyBase64);
    }

    @Override
    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        if (plaintext.isEmpty()) {
            return "";
        }

        try {
            // 1. 生成随机 12 字节 IV
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            // 2. 使用 AES-256-GCM 加密
            SecretKey key = new SecretKeySpec(masterKey, ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH, iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            // 3. 拼接 IV + ciphertext (GCM already includes tag in ciphertext)
            byte[] result = new byte[IV_LENGTH + ciphertext.length];
            System.arraycopy(iv, 0, result, 0, IV_LENGTH);
            System.arraycopy(ciphertext, 0, result, IV_LENGTH, ciphertext.length);

            // 4. Base64 编码返回
            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            throw new EncryptionException("Encryption failed", e);
        }
    }

    @Override
    public String decrypt(String ciphertext) {
        if (ciphertext == null) {
            return null;
        }
        if (ciphertext.isEmpty()) {
            return "";
        }

        try {
            // 1. Base64 解码
            byte[] decoded = Base64.getDecoder().decode(ciphertext);
            if (decoded.length < IV_LENGTH) {
                throw new IllegalArgumentException("Invalid ciphertext format");
            }

            // 2. 提取 IV (前12字节)
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(decoded, 0, iv, 0, IV_LENGTH);

            // 3. 提取 ciphertext + tag (剩余字节)
            byte[] encryptedData = new byte[decoded.length - IV_LENGTH];
            System.arraycopy(decoded, IV_LENGTH, encryptedData, 0, encryptedData.length);

            // 4. AES-256-GCM 解密
            SecretKey key = new SecretKeySpec(masterKey, ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(TAG_LENGTH, iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);
            byte[] plaintext = cipher.doFinal(encryptedData);

            // 5. 返回明文
            return new String(plaintext, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new EncryptionException("Decryption failed", e);
        }
    }
}
