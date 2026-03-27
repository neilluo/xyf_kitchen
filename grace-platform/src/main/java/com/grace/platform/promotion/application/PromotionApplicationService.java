package com.grace.platform.promotion.application;

import com.grace.platform.metadata.domain.VideoMetadata;
import com.grace.platform.metadata.domain.VideoMetadataRepository;
import com.grace.platform.promotion.application.command.ExecutePromotionCommand;
import com.grace.platform.promotion.application.dto.PromotionCopyDTO;
import com.grace.platform.promotion.application.dto.PromotionRecordDTO;
import com.grace.platform.promotion.application.dto.PromotionReportDTO;
import com.grace.platform.promotion.application.dto.PromotionResultDTO;
import com.grace.platform.promotion.domain.*;
import com.grace.platform.promotion.domain.vo.PromotionCopy;
import com.grace.platform.promotion.domain.vo.PromotionReport;
import com.grace.platform.shared.ErrorCode;
import com.grace.platform.shared.application.dto.PageResponse;
import com.grace.platform.shared.domain.PageRequest;
import com.grace.platform.shared.domain.id.ChannelId;
import com.grace.platform.shared.domain.id.PromotionRecordId;
import com.grace.platform.shared.domain.id.VideoId;
import com.grace.platform.shared.infrastructure.encryption.EncryptionService;
import com.grace.platform.shared.infrastructure.exception.EntityNotFoundException;
import com.grace.platform.video.domain.Video;
import com.grace.platform.video.domain.VideoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 推广应用服务
 * <p>
 * 负责编排推广相关的用例流程，包括：
 * <ul>
 *   <li>生成 AI 推广文案 (F1)</li>
 *   <li>批量执行推广任务 (F2) - 按优先级排序，单失败不中断</li>
 *   <li>查询推广历史 (F3)</li>
 *   <li>获取推广报告 (F4)</li>
 *   <li>重试失败的推广 (F5)</li>
 * </ul>
 * </p>
 *
 * @see com.grace.platform.promotion.domain.PromotionExecutor
 * @see com.grace.platform.promotion.domain.PromotionExecutorRegistry
 */
@Service
@Transactional
public class PromotionApplicationService {

    private final VideoRepository videoRepository;
    private final VideoMetadataRepository videoMetadataRepository;
    private final PromotionChannelRepository channelRepository;
    private final PromotionRecordRepository recordRepository;
    private final PromotionCopyGenerationService copyGenerationService;
    private final PromotionExecutorRegistry executorRegistry;
    private final EncryptionService encryptionService;

    /**
     * 创建推广应用服务
     *
     * @param videoRepository           视频仓储
     * @param videoMetadataRepository   元数据仓储
     * @param channelRepository         渠道仓储
     * @param recordRepository          推广记录仓储
     * @param copyGenerationService     文案生成服务
     * @param executorRegistry          执行器注册表
     * @param encryptionService         加密服务
     */
    public PromotionApplicationService(
            VideoRepository videoRepository,
            VideoMetadataRepository videoMetadataRepository,
            PromotionChannelRepository channelRepository,
            PromotionRecordRepository recordRepository,
            PromotionCopyGenerationService copyGenerationService,
            PromotionExecutorRegistry executorRegistry,
            EncryptionService encryptionService) {
        this.videoRepository = videoRepository;
        this.videoMetadataRepository = videoMetadataRepository;
        this.channelRepository = channelRepository;
        this.recordRepository = recordRepository;
        this.copyGenerationService = copyGenerationService;
        this.executorRegistry = executorRegistry;
        this.encryptionService = encryptionService;
    }

    /**
     * 为指定视频的推广渠道生成 AI 推广文案 (F1)
     * <p>
     * 流程：
     * 1. 验证视频存在
     * 2. 获取视频元数据
     * 3. 查询指定渠道（或未指定则查所有 ENABLED 渠道）
     * 4. 逐渠道调用 LLM 生成文案
     * </p>
     *
     * @param videoId    视频 ID
     * @param channelIds 指定渠道列表（可为 null，表示所有 ENABLED 渠道）
     * @return 各渠道的推广文案 DTO 列表
     * @throws EntityNotFoundException 当视频不存在时
     */
    @Transactional(readOnly = true)
    public List<PromotionCopyDTO> generateCopy(VideoId videoId, List<ChannelId> channelIds) {
        // 1. 验证视频存在
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new EntityNotFoundException(
                        ErrorCode.VIDEO_NOT_FOUND,
                        "Video",
                        videoId.value()
                ));

        // 2. 获取视频元数据
        VideoMetadata metadata = videoMetadataRepository.findLatestByVideoId(videoId)
                .orElseThrow(() -> new EntityNotFoundException(
                        ErrorCode.METADATA_NOT_FOUND,
                        "VideoMetadata",
                        videoId.value()
                ));

        // 3. 查询渠道
        List<PromotionChannel> channels;
        if (channelIds == null || channelIds.isEmpty()) {
            // 未指定渠道，查询所有 ENABLED 渠道
            channels = channelRepository.findByStatus(ChannelStatus.ENABLED);
        } else {
            // 查询指定渠道
            channels = channelIds.stream()
                    .map(channelRepository::findById)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .filter(PromotionChannel::isEnabled)
                    .toList();
        }

        if (channels.isEmpty()) {
            return Collections.emptyList();
        }

        // 4. 逐渠道生成文案
        // 构造视频 URL（基于视频 ID）
        String videoUrl = buildVideoUrl(video);

        List<PromotionCopyDTO> result = new ArrayList<>();
        for (PromotionChannel channel : channels) {
            PromotionCopy copy = copyGenerationService.generate(metadata, channel, videoUrl);
            result.add(toCopyDto(copy));
        }

        return result;
    }

    /**
     * 批量执行推广任务 (F2)
     * <p>
     * 编排流程：
     * 1. 验证视频存在
     * 2. 按渠道 ID 查询并排序（priority 升序，数值小 = 优先级高）
     * 3. 逐个渠道执行：
     *    - 创建 PromotionRecord(PENDING)
     *    - 保存记录
     *    - 获取对应执行器
     *    - 状态设为 EXECUTING
     *    - 执行推广
     *    - 成功则 markCompleted，失败则 markFailed
     *    - 保存记录（单失败不中断整体流程）
     * 4. 返回所有渠道的执行结果
     * </p>
     *
     * @param command 执行推广命令
     * @return 各渠道的执行结果 DTO 列表
     * @throws EntityNotFoundException 当视频或渠道不存在时
     */
    public List<PromotionResultDTO> executePromotion(ExecutePromotionCommand command) {
        // 1. 验证视频存在
        VideoId videoId = command.videoId();
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new EntityNotFoundException(
                        ErrorCode.VIDEO_NOT_FOUND,
                        "Video",
                        videoId.value()
                ));

        // 2. 按渠道 ID 查询并构建 ID->channel 映射
        Map<ChannelId, ExecutePromotionCommand.PromotionItem> itemMap = new HashMap<>();
        List<ChannelId> channelIds = command.promotionItems().stream()
                .map(ExecutePromotionCommand.PromotionItem::channelId)
                .toList();

        // 验证所有渠道存在且启用
        List<PromotionChannel> channels = new ArrayList<>();
        for (ExecutePromotionCommand.PromotionItem item : command.promotionItems()) {
            ChannelId channelId = item.channelId();
            PromotionChannel channel = channelRepository.findById(channelId)
                    .orElseThrow(() -> new EntityNotFoundException(
                            ErrorCode.CHANNEL_NOT_FOUND,
                            "PromotionChannel",
                            channelId.value()
                    ));

            if (!channel.isEnabled()) {
                throw new com.grace.platform.shared.infrastructure.exception.BusinessRuleViolationException(
                        ErrorCode.CHANNEL_DISABLED,
                        "Channel is disabled: " + channelId.value()
                );
            }

            channels.add(channel);
            itemMap.put(channelId, item);
        }

        // 3. 按 priority 升序排序（数值小 = 优先级高）
        channels.sort(Comparator.comparingInt(PromotionChannel::getPriority));

        // 4. 逐个渠道执行（单失败不中断）
        List<PromotionResultDTO> results = new ArrayList<>();

        for (PromotionChannel channel : channels) {
            ExecutePromotionCommand.PromotionItem item = itemMap.get(channel.getId());
            PromotionResultDTO result = executeSinglePromotion(video, channel, item);
            results.add(result);
        }

        return results;
    }

    /**
     * 执行单个渠道推广
     *
     * @param video   视频
     * @param channel 渠道
     * @param item    推广任务项
     * @return 执行结果 DTO
     */
    private PromotionResultDTO executeSinglePromotion(
            Video video,
            PromotionChannel channel,
            ExecutePromotionCommand.PromotionItem item) {

        // 构建推广文案
        String promotionCopy = item.promotionTitle() + "\n\n" + item.promotionBody();

        // 创建推广记录 (PENDING)
        PromotionRecord record = PromotionRecord.create(
                video.getId(),
                channel.getId(),
                promotionCopy,
                item.method()
        );

        // 保存记录
        record = recordRepository.save(record);

        // 状态设为 EXECUTING
        record.startExecution();

        try {
            // 获取对应执行器（使用 "opencrawl" 作为默认类型）
            PromotionExecutor executor = executorRegistry.getExecutor("opencrawl");

            // 构建 PromotionCopy 值对象
            PromotionCopy copy = new PromotionCopy(
                    channel.getId(),
                    channel.getName(),
                    channel.getType().name(),
                    item.promotionTitle(),
                    item.promotionBody(),
                    item.method(),
                    "Manual execution"
            );

            // 执行推广
            PromotionResult executionResult = executor.execute(copy, channel);

            // 处理执行结果
            if (executionResult.status() == PromotionStatus.COMPLETED) {
                record.markCompleted(executionResult.resultUrl());
            } else {
                record.markFailed(executionResult.errorMessage());
            }

        } catch (Exception e) {
            // 执行异常，标记为失败
            record.markFailed(e.getMessage());
        }

        // 保存最终状态
        record = recordRepository.save(record);

        // 转换为 DTO
        return toResultDto(record, channel);
    }

    /**
     * 查询指定视频的推广历史 (F3)
     * <p>
     * 委托仓储层进行分页查询。
     * </p>
     *
     * @param videoId     视频 ID
     * @param pageRequest 分页请求
     * @param status      状态筛选（可选）
     * @param channelId   渠道筛选（可选）
     * @param startDate   执行起始日期（可选）
     * @param endDate     执行截止日期（可选）
     * @return 分页推广记录
     * @throws EntityNotFoundException 当视频不存在时
     */
    @Transactional(readOnly = true)
    public PageResponse<PromotionRecordDTO> getPromotionHistory(
            VideoId videoId,
            PageRequest pageRequest,
            PromotionStatus status,
            ChannelId channelId,
            LocalDate startDate,
            LocalDate endDate) {

        // 验证视频存在
        if (videoRepository.findById(videoId).isEmpty()) {
            throw new EntityNotFoundException(
                    ErrorCode.VIDEO_NOT_FOUND,
                    "Video",
                    videoId.value()
            );
        }

        // 委托仓储层查询
        PageResponse<PromotionRecord> pageResult = recordRepository.findByVideoId(videoId, pageRequest, status);

        // 转换为 DTO
        List<PromotionRecordDTO> dtos = pageResult.items().stream()
                .map(this::toRecordDto)
                .toList();

        return new PageResponse<>(
                dtos,
                pageResult.total(),
                pageResult.page(),
                pageResult.pageSize(),
                pageResult.totalPages()
        );
    }

    /**
     * 获取指定视频的推广报告 (F4)
     * <p>
     * 流程：
     * 1. 验证视频存在
     * 2. 查询视频元数据获取标题
     * 3. 查询所有推广记录
     * 4. 聚合统计信息
     * </p>
     *
     * @param videoId 视频 ID
     * @return 推广报告 DTO
     * @throws EntityNotFoundException 当视频不存在时
     */
    @Transactional(readOnly = true)
    public PromotionReportDTO getPromotionReport(VideoId videoId) {
        // 1. 验证视频存在
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new EntityNotFoundException(
                        ErrorCode.VIDEO_NOT_FOUND,
                        "Video",
                        videoId.value()
                ));

        // 2. 查询视频元数据获取标题
        String videoTitle = videoMetadataRepository.findLatestByVideoId(videoId)
                .map(VideoMetadata::getTitle)
                .orElse(video.getFileName());

        // 3. 查询所有推广记录
        List<PromotionRecord> records = recordRepository.findByVideoIdForReport(videoId);

        // 4. 聚合统计
        int totalChannels = records.size();
        int successCount = 0;
        int failedCount = 0;
        int pendingCount = 0;

        List<PromotionReportDTO.ChannelSummaryDTO> summaries = new ArrayList<>();

        for (PromotionRecord record : records) {
            switch (record.getStatus()) {
                case COMPLETED -> successCount++;
                case FAILED -> failedCount++;
                case PENDING, EXECUTING -> pendingCount++;
            }

            // 查询渠道信息
            Optional<PromotionChannel> channelOpt = channelRepository.findById(record.getChannelId());
            String channelName = channelOpt.map(PromotionChannel::getName).orElse("Unknown");
            String channelType = channelOpt.map(c -> c.getType().name()).orElse("UNKNOWN");

            summaries.add(new PromotionReportDTO.ChannelSummaryDTO(
                    record.getChannelId().value(),
                    channelName,
                    channelType,
                    record.getMethod().name(),
                    record.getStatus().name(),
                    record.getResultUrl(),
                    record.getErrorMessage(),
                    record.getExecutedAt() != null ? record.getExecutedAt().toString() : null
            ));
        }

        double overallSuccessRate = totalChannels > 0
                ? (double) successCount / totalChannels
                : 0.0;

        return new PromotionReportDTO(
                videoId.value(),
                videoTitle,
                totalChannels,
                successCount,
                failedCount,
                pendingCount,
                overallSuccessRate,
                summaries
        );
    }

    /**
     * 重试失败的推广记录 (F5)
     * <p>
     * 流程：
     * 1. 查询失败的推广记录
     * 2. 可选更新文案
     * 3. 重新执行推广
     * </p>
     *
     * @param recordId        推广记录 ID
     * @param promotionTitle  新标题（可选，null 表示使用原标题）
     * @param promotionBody   新正文（可选，null 表示使用原文）
     * @return 执行结果 DTO
     * @throws EntityNotFoundException 当记录不存在时
     * @throws com.grace.platform.shared.infrastructure.exception.BusinessRuleViolationException 当记录状态不为 FAILED 时
     */
    public PromotionResultDTO retryPromotion(
            PromotionRecordId recordId,
            String promotionTitle,
            String promotionBody) {

        // 1. 查询记录
        PromotionRecord record = recordRepository.findById(recordId)
                .orElseThrow(() -> new EntityNotFoundException(
                        ErrorCode.PROMOTION_RECORD_NOT_FOUND,
                        "PromotionRecord",
                        recordId.value()
                ));

        // 2. 验证状态为 FAILED
        if (record.getStatus() != PromotionStatus.FAILED) {
            throw new com.grace.platform.shared.infrastructure.exception.BusinessRuleViolationException(
                    ErrorCode.INVALID_PROMOTION_STATUS,
                    "Can only retry promotion records in FAILED status, current status: " + record.getStatus()
            );
        }

        // 3. 获取关联的视频和渠道
        VideoId videoId = record.getVideoId();
        ChannelId channelId = record.getChannelId();

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new EntityNotFoundException(
                        ErrorCode.VIDEO_NOT_FOUND,
                        "Video",
                        videoId.value()
                ));

        PromotionChannel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> new EntityNotFoundException(
                        ErrorCode.CHANNEL_NOT_FOUND,
                        "PromotionChannel",
                        channelId.value()
                ));

        if (!channel.isEnabled()) {
            throw new com.grace.platform.shared.infrastructure.exception.BusinessRuleViolationException(
                    ErrorCode.CHANNEL_DISABLED,
                    "Channel is disabled: " + channel.getId().value()
            );
        }

        // 4. 更新文案（如提供）
        String title = promotionTitle != null ? promotionTitle : extractTitle(record.getPromotionCopy());
        String body = promotionBody != null ? promotionBody : extractBody(record.getPromotionCopy());
        String newCopy = title + "\n\n" + body;

        record.updateCopy(newCopy);

        // 5. 执行重试
        record.retry();

        try {
            // 获取执行器
            PromotionExecutor executor = executorRegistry.getExecutor("opencrawl");

            // 构建 PromotionCopy 值对象
            PromotionCopy copy = new PromotionCopy(
                    channel.getId(),
                    channel.getName(),
                    channel.getType().name(),
                    title,
                    body,
                    record.getMethod(),
                    "Retry execution"
            );

            // 执行推广
            PromotionResult executionResult = executor.execute(copy, channel);

            // 处理执行结果
            if (executionResult.status() == PromotionStatus.COMPLETED) {
                record.markCompleted(executionResult.resultUrl());
            } else {
                record.markFailed(executionResult.errorMessage());
            }

        } catch (Exception e) {
            // 执行异常，标记为失败
            record.markFailed(e.getMessage());
        }

        // 6. 保存最终状态
        record = recordRepository.save(record);

        // 7. 返回结果
        return toResultDto(record, channel);
    }

    // ===== Helper Methods =====

    /**
     * 构建视频 URL
     * <p>
     * 基于视频平台和视频 ID 构造视频链接。
     * </p>
     *
     * @param video 视频
     * @return 视频 URL
     */
    private String buildVideoUrl(Video video) {
        // 简化实现：基于视频 ID 构造 URL
        // 实际生产环境应该根据发布平台构造实际的 URL
        return "https://youtube.com/watch?v=" + video.getId().value();
    }

    /**
     * 从推广文案中提取标题（第一行）
     *
     * @param promotionCopy 推广文案
     * @return 标题
     */
    private String extractTitle(String promotionCopy) {
        if (promotionCopy == null || promotionCopy.isBlank()) {
            return "";
        }
        int newlineIndex = promotionCopy.indexOf('\n');
        if (newlineIndex > 0) {
            return promotionCopy.substring(0, newlineIndex).trim();
        }
        return promotionCopy.trim();
    }

    /**
     * 从推广文案中提取正文（标题之后的部分）
     *
     * @param promotionCopy 推广文案
     * @return 正文
     */
    private String extractBody(String promotionCopy) {
        if (promotionCopy == null || promotionCopy.isBlank()) {
            return "";
        }
        int newlineIndex = promotionCopy.indexOf('\n');
        if (newlineIndex > 0 && newlineIndex + 1 < promotionCopy.length()) {
            return promotionCopy.substring(newlineIndex + 1).trim();
        }
        return "";
    }

    /**
     * 将 PromotionCopy 值对象转换为 DTO
     *
     * @param copy 推广文案值对象
     * @return 推广文案 DTO
     */
    private PromotionCopyDTO toCopyDto(PromotionCopy copy) {
        return new PromotionCopyDTO(
                copy.channelId().value(),
                copy.channelName(),
                copy.channelType(),
                copy.promotionTitle(),
                copy.promotionBody(),
                copy.recommendedMethod(),
                copy.methodReason()
        );
    }

    /**
     * 将 PromotionRecord 实体转换为 Result DTO
     *
     * @param record  推广记录
     * @param channel 渠道
     * @return 推广结果 DTO
     */
    private PromotionResultDTO toResultDto(PromotionRecord record, PromotionChannel channel) {
        return new PromotionResultDTO(
                record.getId().value(),
                record.getChannelId().value(),
                channel.getName(),
                record.getMethod(),
                record.getStatus(),
                record.getResultUrl(),
                record.getErrorMessage(),
                record.getExecutedAt()
        );
    }

    /**
     * 将 PromotionRecord 实体转换为 Record DTO
     *
     * @param record 推广记录
     * @return 推广记录 DTO
     */
    private PromotionRecordDTO toRecordDto(PromotionRecord record) {
        // 查询渠道信息
        Optional<PromotionChannel> channelOpt = channelRepository.findById(record.getChannelId());
        String channelName = channelOpt.map(PromotionChannel::getName).orElse("Unknown");
        String channelType = channelOpt.map(c -> c.getType().name()).orElse("UNKNOWN");

        // 解析推广文案为标题和正文
        String title = extractTitle(record.getPromotionCopy());
        String body = extractBody(record.getPromotionCopy());

        return new PromotionRecordDTO(
                record.getId().value(),
                record.getVideoId().value(),
                record.getChannelId().value(),
                channelName,
                channelType,
                title,
                body,
                record.getMethod(),
                record.getStatus(),
                record.getResultUrl(),
                record.getErrorMessage(),
                record.getExecutedAt(),
                record.getCreatedAt()
        );
    }
}
