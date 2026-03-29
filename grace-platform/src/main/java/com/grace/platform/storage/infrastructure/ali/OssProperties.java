package com.grace.platform.storage.infrastructure.ali;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * OSS 存储配置属性
 * <p>
 * 从 application.yml 加载阿里云 OSS 相关配置。
 * </p>
 */
@Component
@ConfigurationProperties(prefix = "grace.oss")
public class OssProperties {

    private String endpoint;
    private String bucket;
    private String region;
    private String accessKeyId;
    private String accessKeySecret;
    private StsProperties sts;
    private CallbackProperties callback;

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getAccessKeySecret() {
        return accessKeySecret;
    }

    public void setAccessKeySecret(String accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
    }

    public StsProperties getSts() {
        return sts;
    }

    public void setSts(StsProperties sts) {
        this.sts = sts;
    }

    public CallbackProperties getCallback() {
        return callback;
    }

    public void setCallback(CallbackProperties callback) {
        this.callback = callback;
    }

    /**
     * STS 临时凭证配置
     */
    public static class StsProperties {
        private String roleArn;
        private long durationSeconds = 3600;
        private String roleSessionName = "grace-oss-upload";

        public String getRoleArn() {
            return roleArn;
        }

        public void setRoleArn(String roleArn) {
            this.roleArn = roleArn;
        }

        public long getDurationSeconds() {
            return durationSeconds;
        }

        public void setDurationSeconds(long durationSeconds) {
            this.durationSeconds = durationSeconds;
        }

        public String getRoleSessionName() {
            return roleSessionName;
        }

        public void setRoleSessionName(String roleSessionName) {
            this.roleSessionName = roleSessionName;
        }
    }

    /**
     * OSS 回调配置
     */
    public static class CallbackProperties {
        private String url;
        private boolean authEnabled = true;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public boolean isAuthEnabled() {
            return authEnabled;
        }

        public void setAuthEnabled(boolean authEnabled) {
            this.authEnabled = authEnabled;
        }
    }
}