package com.grace.platform.promotion.interfaces;

import com.grace.platform.promotion.application.PromotionApplicationService;
import com.grace.platform.promotion.application.command.ExecutePromotionCommand;
import com.grace.platform.promotion.application.dto.*;
import com.grace.platform.promotion.domain.PromotionMethod;
import com.grace.platform.promotion.domain.PromotionStatus;
import com.grace.platform.promotion.interfaces.dto.request.ExecutePromotionRequest;
import com.grace.platform.promotion.interfaces.dto.request.GenerateCopyRequest;
import com.grace.platform.promotion.interfaces.dto.response.*;
import com.grace.platform.shared.application.dto.ApiResponse;
import com.grace.platform.shared.application.dto.PageResponse;
import com.grace.platform.shared.domain.PageRequest;
import com.grace.platform.shared.domain.id.ChannelId;
import com.grace.platform.shared.domain.id.PromotionRecordId;
import com.grace.platform.shared.domain.id.VideoId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 推广任务控制器。
 * <p>
 * 负责处理推广任务相关的 REST API 端点，包括：
 * <ul>
 *   <li>生成 AI 推广文案</li>
 *   <li>批量执行推广任务</li>
 *   <li>查询推广历史</li>
 *   <li>获取推广报告</li>
 *   <li>重试失败的推广</li>
 * </ul>
 * <p>
 * 端点列表：
 * <ul>
 *   <li>F1: POST /api/promotions/generate-copy - 生成推广文案</li>
 *   <li>F2: POST /api/promotions/execute - 执行推广</li>
 *   <li>F3: GET /api/promotions/history/{videoId} - 查询推广历史</li>
 *   <li>F4: GET /api/promotions/report/{videoId} - 获取推广报告</li>
 *   <li>F5: POST /api/promotions/{promotionRecordId}/retry - 重试推广</li>
 * </ul>
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/promotions")
public class PromotionController {

    private final PromotionApplicationService promotionApplicationService;

    /**
     * 构造器注入依赖。
     *
     * @param promotionApplicationService 推广应用服务
     */
    public PromotionController(PromotionApplicationService promotionApplicationService) {
        this.promotionApplicationService = promotionApplicationService;
    }

    // ==================== F1: 生成推广文案 ====================

    /**
     * 为指定视频的推广渠道生成 AI 推广文案。
     * <p>
     * 调用阿里云 LLM，根据视频元数据和渠道特性生成定制化内容。
     *
     * @param request 生成文案请求
     * @return 各渠道的推广文案列表
     */
    @PostMapping("/generate-copy")
    public ResponseEntity<ApiResponse<List<PromotionCopyResponse>>> generateCopy(
            @Valid @RequestBody GenerateCopyRequest request) {

        VideoId videoId = new VideoId(request.videoId());

        List<ChannelId> channelIds = request.channelIds() != null
                ? request.channelIds().stream().map(ChannelId::new).toList()
                : null;

        List<PromotionCopyDTO> dtos = promotionApplicationService.generateCopy(videoId, channelIds);

        List<PromotionCopyResponse> responses = dtos.stream()
                .map(this::toCopyResponse)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    // ==================== F2: 执行推广 ====================

    /**
     * 批量执行推广任务。
     * <p>
     * 按渠道优先级依次通过 OpenCrawl 发布推广内容。
     * 单个渠道失败不中断整体流程。
     *
     * @param request 执行推广请求
     * @return 各渠道的执行结果列表
     */
    @PostMapping("/execute")
    public ResponseEntity<ApiResponse<List<PromotionResultResponse>>> executePromotion(
            @Valid @RequestBody ExecutePromotionRequest request) {

        VideoId videoId = new VideoId(request.videoId());

        List<ExecutePromotionCommand.PromotionItem> items = request.promotionItems().stream()
                .map(item -> new ExecutePromotionCommand.PromotionItem(
                        new ChannelId(item.channelId()),
                        item.promotionTitle(),
                        item.promotionBody(),
                        PromotionMethod.valueOf(item.method())
                ))
                .toList();

        ExecutePromotionCommand command = new ExecutePromotionCommand(videoId, items);

        List<PromotionResultDTO> dtos = promotionApplicationService.executePromotion(command);

        List<PromotionResultResponse> responses = dtos.stream()
                .map(this::toResultResponse)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    // ==================== F3: 查询推广历史 ====================

    /**
     * 分页查询指定视频的推广历史记录。
     *
     * @param videoId   视频 ID
     * @param page      页码（从 1 开始）
     * @param pageSize  每页条数
     * @param status    状态筛选（可选）
     * @param channelId 渠道筛选（可选）
     * @param startDate 执行起始日期（可选）
     * @param endDate   执行截止日期（可选）
     * @return 分页推广记录
     */
    @GetMapping("/history/{videoId}")
    public ResponseEntity<ApiResponse<PageResponse<PromotionRecordResponse>>> getPromotionHistory(
            @PathVariable String videoId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false)
            @Pattern(regexp = "PENDING|EXECUTING|COMPLETED|FAILED", message = "状态必须是 PENDING, EXECUTING, COMPLETED 或 FAILED")
            String status,
            @RequestParam(required = false) String channelId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        VideoId vid = new VideoId(videoId);
        PageRequest pageRequest = PageRequest.of(page, pageSize);

        PromotionStatus statusFilter = status != null ? PromotionStatus.valueOf(status) : null;
        ChannelId channelFilter = channelId != null ? new ChannelId(channelId) : null;

        PageResponse<PromotionRecordDTO> pageResult = promotionApplicationService.getPromotionHistory(
                vid, pageRequest, statusFilter, channelFilter, startDate, endDate);

        List<PromotionRecordResponse> items = pageResult.items().stream()
                .map(this::toRecordResponse)
                .toList();

        PageResponse<PromotionRecordResponse> response = new PageResponse<>(
                items,
                pageResult.total(),
                pageResult.page(),
                pageResult.pageSize(),
                pageResult.totalPages()
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== F4: 获取推广报告 ====================

    /**
     * 获取指定视频的推广执行报告汇总。
     *
     * @param videoId 视频 ID
     * @return 推广报告
     */
    @GetMapping("/report/{videoId}")
    public ResponseEntity<ApiResponse<PromotionReportResponse>> getPromotionReport(
            @PathVariable String videoId) {

        VideoId vid = new VideoId(videoId);
        PromotionReportDTO dto = promotionApplicationService.getPromotionReport(vid);

        PromotionReportResponse response = toReportResponse(dto);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== F5: 重试推广 ====================

    /**
     * 重试单条失败的推广记录。
     * <p>
     * 可选修改文案后重试。
     *
     * @param promotionRecordId 推广记录 ID
     * @param requestBody       可选的请求体（包含修改后的文案）
     * @return 执行结果
     */
    @PostMapping("/{promotionRecordId}/retry")
    public ResponseEntity<ApiResponse<PromotionResultResponse>> retryPromotion(
            @PathVariable String promotionRecordId,
            @RequestBody(required = false) RetryPromotionRequestBody requestBody) {

        PromotionRecordId recordId = new PromotionRecordId(promotionRecordId);

        String title = requestBody != null ? requestBody.promotionTitle() : null;
        String body = requestBody != null ? requestBody.promotionBody() : null;

        PromotionResultDTO dto = promotionApplicationService.retryPromotion(recordId, title, body);

        PromotionResultResponse response = toResultResponse(dto);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 将 PromotionCopyDTO 转换为 PromotionCopyResponse。
     *
     * @param dto 推广文案 DTO
     * @return 推广文案响应
     */
    private PromotionCopyResponse toCopyResponse(PromotionCopyDTO dto) {
        return new PromotionCopyResponse(
                dto.channelId(),
                dto.channelName(),
                dto.channelType(),
                dto.promotionTitle(),
                dto.promotionBody(),
                dto.recommendedMethod().name(),
                dto.methodReason()
        );
    }

    /**
     * 将 PromotionResultDTO 转换为 PromotionResultResponse。
     *
     * @param dto 推广结果 DTO
     * @return 推广结果响应
     */
    private PromotionResultResponse toResultResponse(PromotionResultDTO dto) {
        return PromotionResultResponse.of(
                dto.promotionRecordId(),
                dto.channelId(),
                dto.channelName(),
                dto.method().name(),
                dto.status().name(),
                dto.resultUrl(),
                dto.errorMessage(),
                dto.executedAt()
        );
    }

    /**
     * 将 PromotionRecordDTO 转换为 PromotionRecordResponse。
     *
     * @param dto 推广记录 DTO
     * @return 推广记录响应
     */
    private PromotionRecordResponse toRecordResponse(PromotionRecordDTO dto) {
        return PromotionRecordResponse.of(
                dto.promotionRecordId(),
                dto.videoId(),
                dto.channelId(),
                dto.channelName(),
                dto.channelType(),
                dto.promotionTitle(),
                dto.promotionBody(),
                dto.method().name(),
                dto.status().name(),
                dto.resultUrl(),
                dto.errorMessage(),
                dto.executedAt(),
                dto.createdAt()
        );
    }

    /**
     * 将 PromotionReportDTO 转换为 PromotionReportResponse。
     *
     * @param dto 推广报告 DTO
     * @return 推广报告响应
     */
    private PromotionReportResponse toReportResponse(PromotionReportDTO dto) {
        List<PromotionReportResponse.ChannelSummaryResponse> summaries = dto.channelSummaries().stream()
                .map(summary -> new PromotionReportResponse.ChannelSummaryResponse(
                        summary.channelId(),
                        summary.channelName(),
                        summary.channelType(),
                        summary.method(),
                        summary.status(),
                        summary.resultUrl(),
                        summary.errorMessage(),
                        summary.executedAt()
                ))
                .toList();

        return new PromotionReportResponse(
                dto.videoId(),
                dto.videoTitle(),
                dto.totalChannels(),
                dto.successCount(),
                dto.failedCount(),
                dto.pendingCount(),
                dto.overallSuccessRate(),
                summaries
        );
    }

    // ==================== 内部请求体记录 ====================

    /**
     * 重试推广请求体。
     * <p>
     * 包含可选的文案修改字段。
     */
    private record RetryPromotionRequestBody(
            String promotionTitle,
            String promotionBody
    ) {
        /**
         * 创建重试推广请求体。
         *
         * @param promotionTitle 修改后的推广标题（可选）
         * @param promotionBody  修改后的推广正文（可选）
         */
        private RetryPromotionRequestBody {
            // Record compact constructor
        }
    }
}
