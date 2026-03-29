package com.grace.platform.storage.domain;

/**
 * 存储服务提供者枚举
 * <p>
 * 支持的存储类型：阿里云 OSS、本地文件系统、AWS S3
 * </p>
 */
public enum StorageProvider {
    OSS,
    LOCAL,
    S3
}