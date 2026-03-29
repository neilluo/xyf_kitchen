package com.grace.platform.storage.infrastructure.ali;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.utils.BinaryUtil;
import com.aliyun.oss.model.PutObjectRequest;
import com.grace.platform.storage.domain.OssStorageService;
import com.grace.platform.storage.domain.StsCredentials;
import com.grace.platform.shared.ErrorCode;
import com.grace.platform.shared.infrastructure.exception.ExternalServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * 阿里云 OSS 存储服务实现
 * <p>
 * 实现 {@link OssStorageService} 接口，提供 OSS 存储的核心操作。
 * </p>
 * <h3>主要功能</h3>
 * <ul>
 *   <li>STS 临时凭证生成（通过 {@link StsTokenService}）</li>
 *   <li>OSS 上传回调签名验证</li>
 *   <li>构建 OSS 对象访问 URL</li>
 * </ul>
 */
@Component
public class AliOssStorageServiceImpl implements OssStorageService {

    private static final Logger logger = LoggerFactory.getLogger(AliOssStorageServiceImpl.class);

    private final OssProperties ossProperties;
    private final StsTokenService stsTokenService;
    private OSS ossClient;

    public AliOssStorageServiceImpl(OssProperties ossProperties, StsTokenService stsTokenService) {
        this.ossProperties = ossProperties;
        this.stsTokenService = stsTokenService;
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

        try {
            File file = localFile.toFile();
            PutObjectRequest putRequest = new PutObjectRequest(
                ossProperties.getBucket(),
                objectKey,
                file
            );
            ossClient.putObject(putRequest);

            logger.info("File uploaded successfully to OSS: {} (size: {} bytes)", objectKey, file.length());
            return objectKey;

        } catch (Exception e) {
            logger.error("Failed to upload file to OSS: {} -> {}", localFile, objectKey, e);
            throw new ExternalServiceException(
                ErrorCode.PLATFORM_API_ERROR,
                "OSS",
                "Failed to upload file: " + e.getMessage()
            );
        }
    }

    @Override
    public StsCredentials generateStsCredentials(String bucket, String objectKey, long durationSeconds) {
        logger.debug("Generating STS credentials for bucket: {}, objectKey: {}", bucket, objectKey);

        if (bucket == null || bucket.isBlank()) {
            bucket = ossProperties.getBucket();
        }

        if (durationSeconds <= 0) {
            durationSeconds = ossProperties.getSts().getDurationSeconds();
        }

        return stsTokenService.generateStsToken(bucket, objectKey, durationSeconds);
    }

    @Override
    public boolean verifyCallbackSignature(String authorization, String pubKeyUrl, String body) {
        logger.debug("Verifying OSS callback signature");

        if (!ossProperties.getCallback().isAuthEnabled()) {
            logger.warn("OSS callback auth is disabled, skipping signature verification");
            return true;
        }

        if (authorization == null || authorization.isBlank()) {
            logger.error("OSS callback authorization header is missing");
            return false;
        }

        if (pubKeyUrl == null || pubKeyUrl.isBlank()) {
            logger.error("OSS callback pubKeyUrl header is missing");
            return false;
        }

        try {
            PublicKey publicKey = getPublicKeyFromUrl(pubKeyUrl);
            boolean verified = doVerifySignature(publicKey, authorization, body);

            if (verified) {
                logger.debug("OSS callback signature verified successfully");
            } else {
                logger.warn("OSS callback signature verification failed");
            }

            return verified;

        } catch (Exception e) {
            logger.error("Failed to verify OSS callback signature: {}", e.getMessage(), e);
            return false;
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

    /**
     * 从 OSS 公钥 URL 获取公钥
     * <p>
     * OSS 回调时会提供公钥 URL，需要从该 URL 下载公钥用于签名验证。
     * </p>
     */
    private PublicKey getPublicKeyFromUrl(String pubKeyUrl) throws Exception {
        logger.debug("Fetching public key from URL: {}", pubKeyUrl);

        URL url = new URL(pubKeyUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        if (connection.getResponseCode() != 200) {
            throw new ExternalServiceException(
                ErrorCode.PLATFORM_API_ERROR,
                "OSS",
                "Failed to fetch public key, HTTP status: " + connection.getResponseCode()
            );
        }

        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
        }

        String publicKeyStr = result.toString().trim();
        byte[] keyBytes = Base64.getDecoder().decode(publicKeyStr);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(keySpec);
    }

    /**
     * 执行签名验证
     * <p>
     * OSS 回调签名格式：base64(rsa_sha1(url_decode(pub_key_url) + "\n" + body))
     * </p>
     */
    private boolean doVerifySignature(PublicKey publicKey, String authorization, String body) throws Exception {
        byte[] authorizationBytes = BinaryUtil.fromBase64String(authorization);
        byte[] bodyDigest = BinaryUtil.calculateMd5(body.getBytes(StandardCharsets.UTF_8));

        Signature signature = Signature.getInstance("SHA1withRSA");
        signature.initVerify(publicKey);
        signature.update(bodyDigest);

        return signature.verify(authorizationBytes);
    }
}