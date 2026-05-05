package com.grace.platform.storage.infrastructure.ali;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.PutObjectRequest;
import com.grace.platform.storage.domain.OssStorageService;
import com.grace.platform.shared.ErrorCode;
import com.grace.platform.shared.infrastructure.exception.ExternalServiceException;
import com.grace.platform.shared.infrastructure.log.DigestLogger;
import com.grace.platform.shared.infrastructure.log.TraceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;

/**
 * 阿里云 OSS 存储服务实现
 * <p>
 * 实现 {@link OssStorageService} 接口，提供 OSS 存储的核心操作。
 * </p>
 * <h3>主要功能</h3>
 * <ul>
 *   <li>服务端上传文件到 OSS</li>
 *   <li>构建 OSS 对象访问 URL</li>
 * </ul>
 */
@Component
public class AliOssStorageServiceImpl implements OssStorageService {

    private static final Logger logger = LoggerFactory.getLogger(AliOssStorageServiceImpl.class);

    private final OssProperties ossProperties;
    private OSS ossClient;

    public AliOssStorageServiceImpl(OssProperties ossProperties) {
        this.ossProperties = ossProperties;
        initOssClient();
    }

    private void initOssClient() {
        try {
            this.ossClient = new OSSClientBuilder().build(
                ossProperties.getEndpoint(),
                ossProperties.getAccessKeyId(),
                ossProperties.getAccessKeySecret()
            );
            logger.info("OSS client initialized successfully for endpoint: {}", ossProperties.getEndpoint());
        } catch (Exception e) {
            logger.error("Failed to initialize OSS client: {}", e.getMessage(), e);
            throw new ExternalServiceException(
                ErrorCode.INFRA_CONFIG_ERROR,
                "OSS",
                "Failed to initialize OSS client: " + e.getMessage()
            );
        }
    }

    @Override
    public String uploadFile(Path localFile, String objectKey) {
        logger.info("Uploading file to OSS: {} -> {}", localFile, objectKey);

        if (localFile == null || !localFile.toFile().exists()) {
            logger.error("Local file does not exist: {}", localFile);
            throw new ExternalServiceException(
                ErrorCode.FILE_OPERATION_ERROR,
                "OSS",
                "Local file does not exist: " + localFile
            );
        }

        if (objectKey == null || objectKey.isBlank()) {
            logger.error("Object key is blank");
            throw new ExternalServiceException(
                ErrorCode.INTERNAL_SERVER_ERROR,
                "OSS",
                "Object key must not be blank"
            );
        }

        String parentRpc = TraceContext.pushSpan();
        long start = System.currentTimeMillis();
        String status = "OK";
        String errCode = "-";
        long sizeBytes = localFile.toFile().length();

        try {
            File file = localFile.toFile();
            PutObjectRequest putRequest = new PutObjectRequest(
                ossProperties.getBucket(),
                objectKey,
                file
            );
            ossClient.putObject(putRequest);

            logger.info("File uploaded successfully to OSS: {} (size: {} bytes)", objectKey, file.length());

            DigestLogger.oss("putObject", ossProperties.getBucket(), objectKey,
                sizeBytes, System.currentTimeMillis() - start, status, errCode);

            return objectKey;

        } catch (Exception e) {
            status = "FAIL";
            errCode = e.getClass().getSimpleName();
            DigestLogger.oss("putObject", ossProperties.getBucket(), objectKey,
                sizeBytes, System.currentTimeMillis() - start, status, errCode);
            throw new ExternalServiceException(
                ErrorCode.PLATFORM_API_ERROR,
                "OSS",
                "Failed to upload file: " + e.getMessage()
            );
        } finally {
            TraceContext.popSpan(parentRpc);
        }
    }

    @Override
    public String getBucketName() {
        return ossProperties.getBucket();
    }

    @Override
    public String getEndpoint() {
        return ossProperties.getEndpoint();
    }

    @Override
    public String buildObjectUrl(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("ObjectKey must not be blank");
        }
        return String.format("https://%s.%s/%s",
            ossProperties.getBucket(),
            ossProperties.getEndpoint(),
            objectKey);
    }
}
