package com.grace.platform.distribution.application.scheduler;

import com.grace.platform.distribution.domain.*;
import com.grace.platform.shared.ErrorCode;
import com.grace.platform.shared.infrastructure.exception.ExternalServiceException;
import com.grace.platform.video.domain.Video;
import com.grace.platform.video.domain.VideoRepository;
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
    private final VideoRepository videoRepository;
    private final int maxRetries;

    public QuotaRetryScheduler(
            PublishRecordRepository publishRecordRepository,
            VideoDistributorRegistry distributorRegistry,
            VideoRepository videoRepository,
            @Value("${grace.scheduler.quota-retry.max-retries:5}") int maxRetries) {
        this.publishRecordRepository = publishRecordRepository;
        this.distributorRegistry = distributorRegistry;
        this.videoRepository = videoRepository;
        this.maxRetries = maxRetries;
    }

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

    private void processQuotaExceededRecord(PublishRecord record) {
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

        Video video = videoRepository.findById(record.getVideoId()).orElse(null);
        if (video == null) {
            logger.error("Video not found for record {}, marking as FAILED", record.getId().value());
            record.markAsFailed("Video not found");
            publishRecordRepository.save(record);
            return;
        }

        String storageUrl = getStorageUrl(video);
        if (storageUrl == null || storageUrl.isBlank()) {
            logger.error("No storage URL for video {}, marking record {} as FAILED",
                    video.getId().value(), record.getId().value());
            record.markAsFailed("No storage URL available");
            publishRecordRepository.save(record);
            return;
        }

        try {
            VideoDistributor distributor = distributorRegistry.getDistributor(platform);

            if (!(distributor instanceof ResumableVideoDistributor resumableDistributor)) {
                logger.error("Platform {} does not support resumable uploads, marking record {} as FAILED",
                        platform, record.getId().value());
                record.markAsFailed("Platform does not support resumable uploads");
                publishRecordRepository.save(record);
                return;
            }

            logger.info("Attempting to resume upload for record {}, platform: {}, retry count: {}/{}",
                    record.getId().value(), platform, record.getRetryCount(), maxRetries);

            PublishResult result = resumableDistributor.resumeUpload(taskId, storageUrl);

            if (result.status() == PublishStatus.COMPLETED) {
                logger.info("Upload resumed successfully for record {}, video URL: {}",
                        record.getId().value(), result.videoUrl());
                record.markAsCompleted(result.videoUrl());
            } else {
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
            record.incrementRetryCount();

            if (record.getRetryCount() >= maxRetries) {
                logger.warn("Record {} reached max retries after error, marking as FAILED",
                        record.getId().value());
                record.markAsFailed("Failed to resume upload: " + e.getMessage());
            }

            publishRecordRepository.save(record);
        }
    }

    private String getStorageUrl(Video video) {
        String storageUrl = video.getStorageUrl();
        if (storageUrl != null && !storageUrl.isBlank()) {
            return storageUrl;
        }
        return video.getFilePath();
    }

    private void handleExternalServiceException(PublishRecord record, ExternalServiceException e) {
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
