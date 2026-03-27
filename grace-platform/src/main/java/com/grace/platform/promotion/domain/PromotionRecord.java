package com.grace.platform.promotion.domain;

import com.grace.platform.shared.ErrorCode;
import com.grace.platform.shared.domain.id.ChannelId;
import com.grace.platform.shared.domain.id.PromotionRecordId;
import com.grace.platform.shared.domain.id.VideoId;
import com.grace.platform.shared.infrastructure.exception.BusinessRuleViolationException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * PromotionRecord 聚合根
 * <p>
 * 记录视频推广执行状态和结果。
 * 状态机：PENDING -> EXECUTING -> COMPLETED/FAILED
 * </p>
 */
public class PromotionRecord {

    // 状态机定义：当前状态 -> 允许的目标状态
    private static final Map<PromotionStatus, Set<PromotionStatus>> STATUS_TRANSITIONS;

    static {
        Map<PromotionStatus, Set<PromotionStatus>> transitions = Map.of(
            // PENDING 可以转移到 EXECUTING
            PromotionStatus.PENDING, Set.of(PromotionStatus.EXECUTING),
            // EXECUTING 可以转移到 COMPLETED 或 FAILED
            PromotionStatus.EXECUTING, Set.of(PromotionStatus.COMPLETED, PromotionStatus.FAILED),
            // FAILED 可以转移到 EXECUTING（重试）
            PromotionStatus.FAILED, Set.of(PromotionStatus.EXECUTING),
            // COMPLETED 是终态，无转移
            PromotionStatus.COMPLETED, Collections.emptySet()
        );
        STATUS_TRANSITIONS = Collections.unmodifiableMap(transitions);
    }

    // 聚合根字段
    private PromotionRecordId id;
    private VideoId videoId;
    private ChannelId channelId;
    private String promotionCopy;
    private PromotionMethod method;
    private PromotionStatus status;
    private String resultUrl;
    private String errorMessage;
    private LocalDateTime executedAt;
    private LocalDateTime createdAt;

    /**
     * 私有构造器，通过工厂方法创建
     */
    private PromotionRecord() {
    }

    /**
     * 创建新的 PromotionRecord 实例
     *
     * @param videoId        视频 ID
     * @param channelId      渠道 ID
     * @param promotionCopy  推广文案
     * @param method         推广方式
     * @return 新建的 PromotionRecord 实例（状态为 PENDING）
     * @throws IllegalArgumentException 当参数校验失败时抛出
     */
    public static PromotionRecord create(VideoId videoId, ChannelId channelId,
                                         String promotionCopy, PromotionMethod method) {
        // 验证视频 ID
        if (videoId == null) {
            throw new IllegalArgumentException("VideoId must not be null");
        }

        // 验证渠道 ID
        if (channelId == null) {
            throw new IllegalArgumentException("ChannelId must not be null");
        }

        // 验证推广文案
        if (promotionCopy == null || promotionCopy.isBlank()) {
            throw new IllegalArgumentException("Promotion copy must not be blank");
        }

        // 验证推广方式
        if (method == null) {
            throw new IllegalArgumentException("Promotion method must not be null");
        }

        PromotionRecord record = new PromotionRecord();
        record.id = PromotionRecordId.generate();
        record.videoId = videoId;
        record.channelId = channelId;
        record.promotionCopy = promotionCopy;
        record.method = method;
        record.status = PromotionStatus.PENDING;
        record.createdAt = LocalDateTime.now();

        return record;
    }

    /**
     * 开始执行推广
     *
     * @throws BusinessRuleViolationException 当状态转换不合法时抛出
     */
    public void startExecution() {
        transitionTo(PromotionStatus.EXECUTING);
    }

    /**
     * 标记执行成功
     *
     * @param resultUrl 推广结果 URL
     * @throws BusinessRuleViolationException 当状态转换不合法时抛出
     */
    public void markCompleted(String resultUrl) {
        transitionTo(PromotionStatus.COMPLETED);
        this.resultUrl = resultUrl;
        this.executedAt = LocalDateTime.now();
    }

    /**
     * 标记执行失败
     *
     * @param errorMessage 错误信息
     * @throws BusinessRuleViolationException 当状态转换不合法时抛出
     */
    public void markFailed(String errorMessage) {
        transitionTo(PromotionStatus.FAILED);
        this.errorMessage = errorMessage;
        this.executedAt = LocalDateTime.now();
    }

    /**
     * 重试推广（从 FAILED 回到 EXECUTING）
     *
     * @throws BusinessRuleViolationException 当状态转换不合法时抛出
     */
    public void retry() {
        if (this.status != PromotionStatus.FAILED) {
            throw new BusinessRuleViolationException(
                ErrorCode.INVALID_PROMOTION_STATUS,
                String.format("Cannot retry promotion record in status %s, must be FAILED", this.status)
            );
        }
        // 清除之前的结果
        this.resultUrl = null;
        this.errorMessage = null;
        this.executedAt = null;
        transitionTo(PromotionStatus.EXECUTING);
    }

    /**
     * 更新推广文案（仅在 PENDING 或 FAILED 状态允许）
     *
     * @param newCopy 新的推广文案
     * @throws BusinessRuleViolationException 当在不合适的状态更新时抛出
     */
    public void updateCopy(String newCopy) {
        if (newCopy == null || newCopy.isBlank()) {
            throw new IllegalArgumentException("Promotion copy must not be blank");
        }

        if (this.status == PromotionStatus.EXECUTING || this.status == PromotionStatus.COMPLETED) {
            throw new BusinessRuleViolationException(
                ErrorCode.INVALID_PROMOTION_STATUS,
                String.format("Cannot update copy in status %s", this.status)
            );
        }

        this.promotionCopy = newCopy;
    }

    /**
     * 状态转换
     *
     * @param targetStatus 目标状态
     * @throws BusinessRuleViolationException 当状态转换不合法时抛出
     */
    private void transitionTo(PromotionStatus targetStatus) {
        if (targetStatus == null) {
            throw new IllegalArgumentException("Target status must not be null");
        }

        if (this.status == targetStatus) {
            // 相同状态，无需转换
            return;
        }

        Set<PromotionStatus> allowedTransitions = STATUS_TRANSITIONS.get(this.status);
        if (allowedTransitions == null || !allowedTransitions.contains(targetStatus)) {
            throw new BusinessRuleViolationException(
                ErrorCode.INVALID_PROMOTION_STATUS,
                String.format("Invalid status transition from %s to %s", this.status, targetStatus)
            );
        }

        this.status = targetStatus;
    }

    // Getters
    public PromotionRecordId getId() {
        return id;
    }

    public VideoId getVideoId() {
        return videoId;
    }

    public ChannelId getChannelId() {
        return channelId;
    }

    public String getPromotionCopy() {
        return promotionCopy;
    }

    public PromotionMethod getMethod() {
        return method;
    }

    public PromotionStatus getStatus() {
        return status;
    }

    public String getResultUrl() {
        return resultUrl;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public LocalDateTime getExecutedAt() {
        return executedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // Setters for persistence layer (package-private)
    void setId(PromotionRecordId id) {
        this.id = id;
    }

    void setVideoId(VideoId videoId) {
        this.videoId = videoId;
    }

    void setChannelId(ChannelId channelId) {
        this.channelId = channelId;
    }

    void setPromotionCopy(String promotionCopy) {
        this.promotionCopy = promotionCopy;
    }

    void setMethod(PromotionMethod method) {
        this.method = method;
    }

    void setStatus(PromotionStatus status) {
        this.status = status;
    }

    void setResultUrl(String resultUrl) {
        this.resultUrl = resultUrl;
    }

    void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    void setExecutedAt(LocalDateTime executedAt) {
        this.executedAt = executedAt;
    }

    void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return String.format("PromotionRecord[id=%s, videoId=%s, channelId=%s, method=%s, status=%s]",
            id != null ? id.value() : "null",
            videoId != null ? videoId.value() : "null",
            channelId != null ? channelId.value() : "null",
            method, status);
    }
}
