package com.grace.platform.storage.domain;

/**
 * 存储 URL 值对象
 * <p>
 * 表示视频文件的存储位置，支持不同存储提供者。
 * 格式：{provider}://{path}，如 oss://bucket/objectKey、local:///path/to/file
 * </p>
 */
public record StorageUrl(
    StorageProvider provider,
    String path
) {
    public StorageUrl {
        if (provider == null) {
            throw new IllegalArgumentException("Provider must not be null");
        }
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Path must not be blank");
        }
    }

    /**
     * 从完整 URL 字符串解析 StorageUrl
     *
     * @param url 完整 URL（如 oss://bucket/key 或 local:///path）
     * @return 解析后的 StorageUrl
     * @throws IllegalArgumentException 如果 URL 格式无效
     */
    public static StorageUrl fromUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL must not be blank");
        }

        int separatorIndex = url.indexOf("://");
        if (separatorIndex < 0) {
            throw new IllegalArgumentException("Invalid URL format: missing provider separator");
        }

        String providerStr = url.substring(0, separatorIndex);
        String path = url.substring(separatorIndex + 3);

        StorageProvider provider = switch (providerStr.toLowerCase()) {
            case "oss" -> StorageProvider.OSS;
            case "local" -> StorageProvider.LOCAL;
            case "s3" -> StorageProvider.S3;
            default -> throw new IllegalArgumentException("Unknown storage provider: " + providerStr);
        };

        return new StorageUrl(provider, path);
    }

    /**
     * 构建 OSS 存储 URL
     *
     * @param bucket    OSS Bucket
     * @param objectKey Object Key
     * @return OSS 存储 URL
     */
    public static StorageUrl oss(String bucket, String objectKey) {
        return new StorageUrl(StorageProvider.OSS, bucket + "/" + objectKey);
    }

    /**
     * 构建本地文件存储 URL
     *
     * @param filePath 本地文件路径
     * @return 本地存储 URL
     */
    public static StorageUrl local(String filePath) {
        return new StorageUrl(StorageProvider.LOCAL, filePath);
    }

    /**
     * 构建 S3 存储 URL
     *
     * @param bucket S3 Bucket
     * @param key    S3 Key
     * @return S3 存储 URL
     */
    public static StorageUrl s3(String bucket, String key) {
        return new StorageUrl(StorageProvider.S3, bucket + "/" + key);
    }

    /**
     * 转换为完整 URL 字符串
     *
     * @return 完整 URL（如 oss://bucket/key）
     */
    public String toUrl() {
        String providerStr = switch (provider) {
            case OSS -> "oss";
            case LOCAL -> "local";
            case S3 -> "s3";
        };
        return providerStr + "://" + path;
    }

    /**
     * 检查是否为 OSS 存储
     *
     * @return true 如果存储提供者是 OSS
     */
    public boolean isOss() {
        return provider == StorageProvider.OSS;
    }

    /**
     * 检查是否为本地存储
     *
     * @return true 如果存储提供者是本地文件系统
     */
    public boolean isLocal() {
        return provider == StorageProvider.LOCAL;
    }

    /**
     * 检查是否为 S3 存储
     *
     * @return true 如果存储提供者是 AWS S3
     */
    public boolean isS3() {
        return provider == StorageProvider.S3;
    }

    /**
     * 获取 OSS Bucket（仅适用于 OSS 存储）
     *
     * @return OSS Bucket 名称，或 null 如果非 OSS 存储
     */
    public String getOssBucket() {
        if (!isOss()) {
            return null;
        }
        int slashIndex = path.indexOf("/");
        return slashIndex > 0 ? path.substring(0, slashIndex) : path;
    }

    /**
     * 获取 OSS Object Key（仅适用于 OSS 存储）
     *
     * @return OSS Object Key，或 null 如果非 OSS 存储
     */
    public String getOssObjectKey() {
        if (!isOss()) {
            return null;
        }
        int slashIndex = path.indexOf("/");
        return slashIndex > 0 && slashIndex < path.length() - 1
            ? path.substring(slashIndex + 1)
            : null;
    }

    @Override
    public String toString() {
        return toUrl();
    }
}