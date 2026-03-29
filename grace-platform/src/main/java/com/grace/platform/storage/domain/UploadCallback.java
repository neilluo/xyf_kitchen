package com.grace.platform.storage.domain;

/**
 * OSS 上传回调数据对象
 * <p>
 * OSS 分片上传完成后的回调数据，包含上传结果信息。
 * </p>
 */
public record UploadCallback(
    String bucket,
    String objectKey,
    String etag,
    long fileSize,
    String mimeType,
    String uploadId
) {
    public UploadCallback {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalArgumentException("Bucket must not be blank");
        }
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("ObjectKey must not be blank");
        }
        if (etag == null || etag.isBlank()) {
            throw new IllegalArgumentException("ETag must not be blank");
        }
        if (fileSize < 0) {
            throw new IllegalArgumentException("FileSize must be non-negative");
        }
        if (uploadId == null || uploadId.isBlank()) {
            throw new IllegalArgumentException("UploadId must not be blank");
        }
    }

    /**
     * 获取存储 URL（OSS 对象访问路径）
     *
     * @return OSS 对象路径格式：oss://{bucket}/{objectKey}
     */
    public String getStoragePath() {
        return String.format("oss://%s/%s", bucket, objectKey);
    }
}