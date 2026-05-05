package com.grace.platform.storage.domain;

import java.nio.file.Path;

/**
 * OSS 存储服务接口
 * <p>
 * 定义 OSS 存储的核心操作：服务端上传文件、构建对象 URL。
 * domain 层接口，不依赖具体的 OSS SDK 或 Spring 框架。
 * </p>
 */
public interface OssStorageService {

    /**
     * 上传本地文件到 OSS
     * <p>
     * 用于服务端上传场景：合并分片后上传到 OSS。
     * </p>
     *
     * @param localFile  本地文件路径
     * @param objectKey  OSS Object Key
     * @return 上传成功后的 Object Key
     */
    String uploadFile(Path localFile, String objectKey);

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
