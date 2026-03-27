package com.grace.platform.distribution.application.scheduler;

import com.grace.platform.distribution.domain.*;
import com.grace.platform.shared.ErrorCode;
import com.grace.platform.shared.infrastructure.exception.ExternalServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 配额超限重试调度器
 * <p>
 * 定时扫描 QUOTA_EXCEEDED 状态的发布记录，尝试恢复上传。
 * 重试策略：30分钟间隔，最多5次重试。
 * </p>
 *
 * @author Grace Platform Team
 * @see ResumableVideoDistributor
 * @see PublishRecord
 */
@Component
public class QuotaRetryScheduler {

    private static final Logger logger = LoggerFactory.getLogger(QuotaRetryScheduler.class);

    private final PublishRecordRepository publishRecordRepository;
    private final VideoDistributorRegistry distributorRegistry;
    private final int maxRetries;

    /**
     * 构造配额重试调度器
     *
     * @param publishRecordRepository 发布记录仓储
     * @param distributorRegistry     分发器注册表
     * @param maxRetries              最大重试次数（从配置读取）
     */
    public QuotaRetryScheduler(
            PublishRecordRepository publishRecordRepository,
            VideoDistributorRegistry distributorRegistry,
            @Value("${grace.scheduler.quota-retry.max-retries:5}") int maxRetries) {
        this.publishRecordRepository = publishRecordRepository;
        this.distributorRegistry = distributorRegistry;
        this.maxRetries = maxRetries;
    }

    /**
     * 重试配额超限任务
     * <p>
     * 固定延迟调度：上一次执行完成后等待配置的时间间隔再执行。
     * 查询所有 QUOTA_EXCEEDED 状态的记录，过滤掉已达到最大重试次数的记录。
     * 对每个符合条件的记录：
     * 1. 获取对应平台的分发器
     * 2. 如果支持断点续传，调用 resumeUpload 尝试恢复
     * 3. 成功则标记为 COMPLETED
     * 4. 仍配额超限则增加重试计数
     * 5. 达到最大重试次数则标记为 FAILED
     * </p>
     */
    @Scheduled(fixedDelayString = "${grace.scheduler.quota-retry.fixed-delay}")
    public void retryQuotaExceededTasks() {
        logger.debug("Starting quota retry scheduler scan...");

        List<PublishRecord> quotaExceededRecords = publishRecordRepository.findByStatus(PublishStatus.QUOTA_EXCEEDED);

        if (quotaExceededRecords.isEmpty()) {
            logger.debug("No QUOTA_EXCEEDED records found");
            return;
        }

        logger.info("Found {} records in QUOTA_EXCEEDED status", quotaExceededRecords.size());

        for (PublishRecord record : quotaExceededRecords) {
            processQuotaExceededRecord(record);
        }

        logger.debug("Quota retry scheduler scan completed");
    }

    /**
     * 处理单个配额超限记录
     *
     * @param record 配额超限的发布记录
     */
    private void processQuotaExceededRecord(PublishRecord record) {
        // 检查是否已达到最大重试次数
        if (record.getRetryCount() >= maxRetries) {
            logger.warn("Record {} has exceeded max retry attempts ({}), marking as FAILED",
                    record.getId().value(), maxRetries);
            record.markAsFailed("Exceeded maximum retry attempts for quota recovery");
            publishRecordRepository.save(record);
            return;
        }

        String platform = record.getPlatform();
        String taskId = record.getUploadTaskId();

        if (taskId == null || taskId.isBlank()) {
            logger.error("Record {} has no upload task ID, marking as FAILED", record.getId().value());
            record.markAsFailed("Missing upload task ID");
            publishRecordRepository.save(record);
            return;
        }

        try {
            VideoDistributor distributor = distributorRegistry.getDistributor(platform);

            // 检查是否支持断点续传
            if (!(distributor instanceof ResumableVideoDistributor resumableDistributor)) {
                logger.error("Platform {} does not support resumable uploads, marking record {} as FAILED",
                        platform, record.getId().value());
                record.markAsFailed("Platform does not support resumable uploads");
                publishRecordRepository.save(record);
                return;
            }

            logger.info("Attempting to resume upload for record {}, platform: {}, retry count: {}/{}",
                    record.getId().value(), platform, record.getRetryCount(), maxRetries);

            // 尝试恢复上传
            PublishResult result = resumableDistributor.resumeUpload(taskId);

            // 恢复成功，更新状态
            if (result.status() == PublishStatus.COMPLETED) {
                logger.info("Upload resumed successfully for record {}, video URL: {}",
                        record.getId().value(), result.videoUrl());
                record.markAsCompleted(result.videoUrl());
            } else {
                // 仍在处理中，恢复为 UPLOADING 状态
                logger.info("Upload resumed for record {}, new status: {}",
                        record.getId().value(), result.status());
                record.resumeFromQuotaExceeded();
            }

            publishRecordRepository.save(record);

        } catch (ExternalServiceException e) {
            handleExternalServiceException(record, e);
        } catch (Exception e) {
            logger.error("Unexpected error while resuming upload for record {}: {}",
                    record.getId().value(), e.getMessage(), e);
            // 非配额错误，增加重试计数
            record.incrementRetryCount();

            if (record.getRetryCount() >= maxRetries) {
                logger.warn("Record {} reached max retries after error, marking as FAILED",
                        record.getId().value());
                record.markAsFailed("Failed to resume upload: " + e.getMessage());
            }

            publishRecordRepository.save(record);
        }
    }

    /**
     * 处理外部服务异常
     *
     * @param record 发布记录
     * @param e      外部服务异常
     */
    private void handleExternalServiceException(PublishRecord record, ExternalServiceException e) {
        // 检查是否仍为配额超限
        if (e.getErrorCode() == ErrorCode.PLATFORM_QUOTA_EXCEEDED) {
            logger.warn("Platform quota still exceeded for record {}, incrementing retry count",
                    record.getId().value());
            record.incrementRetryCount();

            if (record.getRetryCount() >= maxRetries) {
                logger.warn("Record {} reached max retries for quota exceeded, marking as FAILED",
                        record.getId().value());
                record.markAsFailed("Platform quota exceeded for maximum retry attempts");
            }

            publishRecordRepository.save(record);
        } else {
            // 其他外部服务错误（如 token 过期等）
            logger.error("External service error for record {}: {} (code: {})",
                    record.getId().value(), e.getMessage(), e.getErrorCode());
            record.incrementRetryCount();

            if (record.getRetryCount() >= maxRetries) {
                record.markAsFailed("External service error: " + e.getMessage());
            }

            publishRecordRepository.save(record);
        }
    }
}
