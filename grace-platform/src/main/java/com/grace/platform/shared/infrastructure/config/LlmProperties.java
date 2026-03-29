package com.grace.platform.shared.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * LLM（通义千问）配置属性
 * <p>
 * 从 application.yml 加载 Qwen LLM 相关配置。
 * </p>
 * <h3>多模态配置说明</h3>
 * <p>
 * 当使用多模态模型（如 qwen-vl-max）时，系统会从视频中提取关键帧，
 * 连同文本提示一起发送给 LLM，以生成更精准的元数据。
 * </p>
 * <ul>
 *   <li>{@code multimodalEnabled}: 是否启用多模态功能，启用后将提取视频关键帧进行分析</li>
 *   <li>{@code frameCount}: 提取的关键帧数量，建议 3-5 帧，过多会增加 API 调用成本</li>
 * </ul>
 */
@Component
@ConfigurationProperties(prefix = "grace.llm")
public class LlmProperties {

    private String provider;
    private String apiKey;
    private String model;
    private String baseUrl;
    private double temperature;
    private int maxTokens;
    private int timeoutSeconds;
    private RetryProperties retry;
    private boolean multimodalEnabled = true;
    private int frameCount = 3;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
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

    public boolean isMultimodalEnabled() {
        return multimodalEnabled;
    }

    public void setMultimodalEnabled(boolean multimodalEnabled) {
        this.multimodalEnabled = multimodalEnabled;
    }

    public int getFrameCount() {
        return frameCount;
    }

    public void setFrameCount(int frameCount) {
        this.frameCount = frameCount;
    }

    /**
     * 重试配置属性
     */
    public static class RetryProperties {
        private int maxAttempts;
        private int backoffMultiplier;
        private long initialIntervalMs;

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public int getBackoffMultiplier() {
            return backoffMultiplier;
        }

        public void setBackoffMultiplier(int backoffMultiplier) {
            this.backoffMultiplier = backoffMultiplier;
        }

        public long getInitialIntervalMs() {
            return initialIntervalMs;
        }

        public void setInitialIntervalMs(long initialIntervalMs) {
            this.initialIntervalMs = initialIntervalMs;
        }
    }
}
