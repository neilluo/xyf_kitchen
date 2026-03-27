package com.grace.platform.promotion.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * OpenCrawl 配置属性
 * <p>
 * 从 application.yml 加载 OpenCrawl Agentic API 相关配置。
 * </p>
 */
@Component
@ConfigurationProperties(prefix = "grace.opencrawl")
public class OpenCrawlProperties {

    private String baseUrl;
    private String apiKey;
    private int timeoutSeconds;
    private RetryProperties retry;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public RetryProperties getRetry() {
        return retry;
    }

    public void setRetry(RetryProperties retry) {
        this.retry = retry;
    }

    /**
     * 重试配置属性
     */
    public static class RetryProperties {
        private int maxAttempts;

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }
    }
}
