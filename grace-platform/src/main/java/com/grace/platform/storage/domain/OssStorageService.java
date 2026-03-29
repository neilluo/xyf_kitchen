package com.grace.platform.storage.domain;

/**
 * OSS 存储服务接口
 * <p>
 * 定义 OSS 存储的核心操作：获取 STS 临时凭证、验证上传回调签名。
 * domain 层接口，不依赖具体的 OSS SDK 或 Spring 框架。
 * </p>
 */
public interface OssStorageService {

    /**
     * 生成 STS 临时凭证
     * <p>
     * 调用阿里云 STS 服务，创建临时访问凭证供前端直传 OSS。
     * 凭证权限限制为仅能上传到指定的 Object Key 路径。
     * </p>
     *
     * @param bucket    目标 OSS Bucket
     * @param objectKey 目标 Object Key（OSS 路径）
     * @param durationSeconds 凭证有效期（秒）
     * @return STS 临时凭证
     */
    StsCredentials generateStsCredentials(String bucket, String objectKey, long durationSeconds);

    /**
     * 验证 OSS 上传回调签名
     * <p>
     * OSS 上传完成后会回调后端，需要验证签名防止伪造回调。
     * 使用 OSS 公钥验证签名。
     * </p>
     *
     * @param authorization OSS 回调请求头中的 Authorization
     * @param pubKeyUrl     OSS 回调请求头中的 x-oss-pub-key-url
     * @param body          回调请求体
     * @return true 如果签名验证通过
     */
    boolean verifyCallbackSignature(String authorization, String pubKeyUrl, String body);

    /**
     * 获取 OSS Bucket 名称
     *
     * @return 配置的 OSS Bucket 名称
     */
    String getBucketName();

    /**
     * 获取 OSS Endpoint
     *
     * @return OSS Endpoint（如 oss-cn-hangzhou.aliyuncs.com）
     */
    String getEndpoint();

    /**
     * 构建完整的 OSS 对象 URL
     *
     * @param objectKey Object Key
     * @return 完整的访问 URL
     */
    String buildObjectUrl(String objectKey);
}