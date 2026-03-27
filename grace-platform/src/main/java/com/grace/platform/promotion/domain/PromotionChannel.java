package com.grace.platform.promotion.domain;

import com.grace.platform.shared.domain.id.ChannelId;
import com.grace.platform.shared.infrastructure.encryption.EncryptionService;

import java.time.LocalDateTime;

/**
 * PromotionChannel 聚合根
 * <p>
 * 管理推广渠道配置，支持 API Key 加密存储。
 * </p>
 */
public class PromotionChannel {

    // 优先级范围常量
    public static final int MIN_PRIORITY = 1;
    public static final int MAX_PRIORITY = 99;
    public static final int DEFAULT_PRIORITY = 1;

    // 聚合根字段
    private ChannelId id;
    private String name;
    private ChannelType type;
    private String channelUrl;
    private String encryptedApiKey;
    private int priority;
    private ChannelStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 私有构造器，通过工厂方法创建
     */
    private PromotionChannel() {
    }

    /**
     * 创建新的 PromotionChannel 实例
     *
     * @param name      渠道名称
     * @param type      渠道类型
     * @param channelUrl 渠道 URL
     * @param priority  优先级（1-99，默认 1）
     * @return 新建的 PromotionChannel 实例（状态为 ENABLED）
     * @throws IllegalArgumentException 当参数校验失败时抛出
     */
    public static PromotionChannel create(String name, ChannelType type, String channelUrl, int priority) {
        // 验证名称
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Channel name must not be blank");
        }

        // 验证类型
        if (type == null) {
            throw new IllegalArgumentException("Channel type must not be null");
        }

        // 验证 URL
        if (channelUrl == null || channelUrl.isBlank()) {
            throw new IllegalArgumentException("Channel URL must not be blank");
        }

        // 验证优先级范围
        if (priority < MIN_PRIORITY || priority > MAX_PRIORITY) {
            throw new IllegalArgumentException(
                String.format("Priority must be between %d and %d, got: %d", MIN_PRIORITY, MAX_PRIORITY, priority)
            );
        }

        PromotionChannel channel = new PromotionChannel();
        channel.id = ChannelId.generate();
        channel.name = name;
        channel.type = type;
        channel.channelUrl = channelUrl;
        channel.priority = priority;
        channel.status = ChannelStatus.ENABLED;
        channel.createdAt = LocalDateTime.now();
        channel.updatedAt = channel.createdAt;

        return channel;
    }

    /**
     * 创建新的 PromotionChannel 实例（使用默认优先级）
     *
     * @param name       渠道名称
     * @param type       渠道类型
     * @param channelUrl 渠道 URL
     * @return 新建的 PromotionChannel 实例
     */
    public static PromotionChannel create(String name, ChannelType type, String channelUrl) {
        return create(name, type, channelUrl, DEFAULT_PRIORITY);
    }

    /**
     * 设置 API Key（加密存储）
     *
     * @param rawApiKey          明文 API Key
     * @param encryptionService  加密服务
     */
    public void setApiKey(String rawApiKey, EncryptionService encryptionService) {
        if (rawApiKey == null || rawApiKey.isBlank()) {
            this.encryptedApiKey = null;
        } else {
            this.encryptedApiKey = encryptionService.encrypt(rawApiKey);
        }
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 获取解密后的 API Key
     *
     * @param encryptionService 加密服务
     * @return 明文 API Key，如果未设置则返回 null
     */
    public String getDecryptedApiKey(EncryptionService encryptionService) {
        return encryptedApiKey == null ? null : encryptionService.decrypt(encryptedApiKey);
    }

    /**
     * 启用渠道
     */
    public void enable() {
        this.status = ChannelStatus.ENABLED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 禁用渠道
     */
    public void disable() {
        this.status = ChannelStatus.DISABLED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 检查渠道是否启用
     *
     * @return true 如果渠道状态为 ENABLED
     */
    public boolean isEnabled() {
        return this.status == ChannelStatus.ENABLED;
    }

    /**
     * 更新渠道信息
     *
     * @param name       新名称（可选）
     * @param type       新类型（可选）
     * @param channelUrl 新 URL（可选）
     * @param priority   新优先级（可选，1-99）
     */
    public void updateInfo(String name, ChannelType type, String channelUrl, Integer priority) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        if (type != null) {
            this.type = type;
        }
        if (channelUrl != null && !channelUrl.isBlank()) {
            this.channelUrl = channelUrl;
        }
        if (priority != null) {
            if (priority < MIN_PRIORITY || priority > MAX_PRIORITY) {
                throw new IllegalArgumentException(
                    String.format("Priority must be between %d and %d, got: %d", MIN_PRIORITY, MAX_PRIORITY, priority)
                );
            }
            this.priority = priority;
        }
        this.updatedAt = LocalDateTime.now();
    }

    // Getters
    public ChannelId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public ChannelType getType() {
        return type;
    }

    public String getChannelUrl() {
        return channelUrl;
    }

    public String getEncryptedApiKey() {
        return encryptedApiKey;
    }

    public int getPriority() {
        return priority;
    }

    public ChannelStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // Setters for persistence layer (package-private)
    void setId(ChannelId id) {
        this.id = id;
    }

    void setName(String name) {
        this.name = name;
    }

    void setType(ChannelType type) {
        this.type = type;
    }

    void setChannelUrl(String channelUrl) {
        this.channelUrl = channelUrl;
    }

    void setEncryptedApiKey(String encryptedApiKey) {
        this.encryptedApiKey = encryptedApiKey;
    }

    void setPriority(int priority) {
        this.priority = priority;
    }

    void setStatus(ChannelStatus status) {
        this.status = status;
    }

    void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return String.format("PromotionChannel[id=%s, name=%s, type=%s, status=%s, priority=%d]",
            id != null ? id.value() : "null", name, type, status, priority);
    }
}
