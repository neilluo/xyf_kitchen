package com.grace.platform.shared.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 加密配置属性
 * <p>
 * 从 application.yml 加载 AES-256-GCM 加密相关配置。
 * 用于加密 OAuth Token 和 Promotion Channel API Key。
 * </p>
 */
@Component
@ConfigurationProperties(prefix = "grace.encryption")
public class EncryptionProperties {

    private String algorithm;
    private String key;
    private int ivLength;
    private int tagLength;

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public int getIvLength() {
        return ivLength;
    }

    public void setIvLength(int ivLength) {
        this.ivLength = ivLength;
    }

    public int getTagLength() {
        return tagLength;
    }

    public void setTagLength(int tagLength) {
        this.tagLength = tagLength;
    }
}
