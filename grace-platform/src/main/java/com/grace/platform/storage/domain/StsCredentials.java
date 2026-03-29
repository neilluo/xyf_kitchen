package com.grace.platform.storage.domain;

import java.time.LocalDateTime;

/**
 * STS 临时凭证值对象
 * <p>
 * 阿里云 STS（Security Token Service）颁发的临时访问凭证，
 * 用于前端直传 OSS。包含 AccessKeyId、AccessKeySecret、SecurityToken 和过期时间。
 * </p>
 */
public record StsCredentials(
    String accessKeyId,
    String accessKeySecret,
    String securityToken,
    LocalDateTime expiration,
    String region,
    String bucket
) {
    public StsCredentials {
        if (accessKeyId == null || accessKeyId.isBlank()) {
            throw new IllegalArgumentException("AccessKeyId must not be blank");
        }
        if (accessKeySecret == null || accessKeySecret.isBlank()) {
            throw new IllegalArgumentException("AccessKeySecret must not be blank");
        }
        if (securityToken == null || securityToken.isBlank()) {
            throw new IllegalArgumentException("SecurityToken must not be blank");
        }
        if (expiration == null) {
            throw new IllegalArgumentException("Expiration must not be null");
        }
        if (region == null || region.isBlank()) {
            throw new IllegalArgumentException("Region must not be blank");
        }
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalArgumentException("Bucket must not be blank");
        }
    }

    /**
     * 检查凭证是否已过期
     *
     * @return true 如果凭证已过期
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiration);
    }

    /**
     * 检查凭证是否即将过期（在指定秒数内）
     *
     * @param seconds 预警秒数
     * @return true 如果凭证在指定秒数内将过期
     */
    public boolean isExpiringSoon(long seconds) {
        return LocalDateTime.now().plusSeconds(seconds).isAfter(expiration);
    }
}