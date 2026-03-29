package com.grace.platform.storage.infrastructure.ali;

import com.aliyun.sts20150401.Client;
import com.aliyun.sts20150401.models.AssumeRoleRequest;
import com.aliyun.sts20150401.models.AssumeRoleResponse;
import com.aliyun.sts20150401.models.AssumeRoleResponseBody;
import com.aliyun.teaopenapi.models.Config;
import com.grace.platform.storage.domain.StsCredentials;
import com.grace.platform.shared.ErrorCode;
import com.grace.platform.shared.infrastructure.exception.ExternalServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * STS 临时凭证服务
 * <p>
 * 调用阿里云 STS AssumeRole API，生成临时访问凭证供前端直传 OSS。
 * 凭证权限通过 RAM Role Policy 限制为仅能上传到指定路径。
 * </p>
 */
@Component
public class StsTokenService {

    private static final Logger logger = LoggerFactory.getLogger(StsTokenService.class);

    private final OssProperties ossProperties;
    private Client stsClient;

    public StsTokenService(OssProperties ossProperties) {
        this.ossProperties = ossProperties;
        initStsClient();
    }

    private void initStsClient() {
        try {
            Config config = new Config()
                .setAccessKeyId(ossProperties.getAccessKeyId())
                .setAccessKeySecret(ossProperties.getAccessKeySecret())
                .setEndpoint("sts." + extractRegionFromEndpoint(ossProperties.getEndpoint()) + ".aliyuncs.com");

            this.stsClient = new Client(config);
            logger.info("STS client initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize STS client: {}", e.getMessage(), e);
            throw new ExternalServiceException(
                ErrorCode.INFRA_CONFIG_ERROR,
                "STS",
                "Failed to initialize STS client: " + e.getMessage()
            );
        }
    }

    /**
     * 生成 STS 临时凭证
     * <p>
     * 调用 AssumeRole API，返回临时 AccessKeyId、AccessKeySecret、SecurityToken。
     * 通过 Policy 限制权限范围。
     * </p>
     *
     * @param bucket         目标 OSS Bucket
     * @param objectKey      目标 Object Key
     * @param durationSeconds 凭证有效期（秒）
     * @return STS 临时凭证
     */
    public StsCredentials generateStsToken(String bucket, String objectKey, long durationSeconds) {
        logger.info("Generating STS token for bucket: {}, objectKey: {}, duration: {}s",
            bucket, objectKey, durationSeconds);

        try {
            AssumeRoleRequest request = new AssumeRoleRequest()
                .setRoleArn(ossProperties.getSts().getRoleArn())
                .setRoleSessionName(ossProperties.getSts().getRoleSessionName() + "-" + System.currentTimeMillis())
                .setDurationSeconds((long) durationSeconds)
                .setPolicy(buildPolicy(bucket, objectKey));

            AssumeRoleResponse response = stsClient.assumeRole(request);
            AssumeRoleResponseBody body = response.getBody();

            if (body == null || body.getCredentials() == null) {
                logger.error("STS AssumeRole response missing credentials");
                throw new ExternalServiceException(
                    ErrorCode.PLATFORM_API_ERROR,
                    "STS",
                    "STS response missing credentials"
                );
            }

            AssumeRoleResponseBody.AssumeRoleResponseBodyCredentials credentials = body.getCredentials();

            LocalDateTime expiration = convertToLocalDateTime(credentials.getExpiration());

            StsCredentials stsCredentials = new StsCredentials(
                credentials.getAccessKeyId(),
                credentials.getAccessKeySecret(),
                credentials.getSecurityToken(),
                expiration,
                ossProperties.getRegion(),
                bucket
            );

            logger.info("STS token generated successfully, expires at: {}", expiration);
            return stsCredentials;

        } catch (Exception e) {
            logger.error("Failed to generate STS token: {}", e.getMessage(), e);
            throw new ExternalServiceException(
                ErrorCode.PLATFORM_API_ERROR,
                "STS",
                "Failed to generate STS token: " + e.getMessage()
            );
        }
    }

    /**
     * 构建 RAM Policy，限制权限范围
     * <p>
     * Policy 仅允许对指定的 Object Key 进行上传操作。
     * </p>
     */
    private String buildPolicy(String bucket, String objectKey) {
        return String.format(
            "{\"Version\":\"1\",\"Statement\":[{\"Effect\":\"Allow\",\"Action\":[\"oss:PutObject\",\"oss:GetObject\",\"oss:AbortMultipartUpload\",\"oss:ListParts\",\"oss:ListMultipartUploads\"],\"Resource\":[\"acs:oss:*:*:%s/%s\",\"acs:oss:*:*:%s/%s*\"]}]}",
            bucket, objectKey, bucket, objectKey
        );
    }

    /**
     * 从 OSS Endpoint 提取 Region
     * <p>
     * 例如：oss-cn-hangzhou.aliyuncs.com → cn-hangzhou
     * </p>
     */
    private String extractRegionFromEndpoint(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) {
            return "cn-hangzhou";
        }
        int ossIndex = endpoint.indexOf("oss-");
        int aliIndex = endpoint.indexOf(".aliyuncs.com");
        if (ossIndex >= 0 && aliIndex > ossIndex + 4) {
            return endpoint.substring(ossIndex + 4, aliIndex);
        }
        return ossProperties.getRegion() != null ? ossProperties.getRegion() : "cn-hangzhou";
    }

    /**
     * 将 ISO 8601 时间字符串转换为 LocalDateTime
     */
    private LocalDateTime convertToLocalDateTime(String expirationStr) {
        if (expirationStr == null || expirationStr.isBlank()) {
            return LocalDateTime.now().plusSeconds(ossProperties.getSts().getDurationSeconds());
        }
        try {
            return LocalDateTime.parse(expirationStr, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            logger.warn("Failed to parse expiration string: {}, using default duration", expirationStr);
            return LocalDateTime.now().plusSeconds(ossProperties.getSts().getDurationSeconds());
        }
    }
}