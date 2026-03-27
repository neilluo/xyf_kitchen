package com.grace.platform.distribution.interfaces;

import com.grace.platform.distribution.application.DistributionApplicationService;
import com.grace.platform.distribution.application.command.PublishCommand;
import com.grace.platform.distribution.application.dto.PlatformInfoDTO;
import com.grace.platform.distribution.application.dto.PublishRecordDTO;
import com.grace.platform.distribution.application.dto.PublishResultDTO;
import com.grace.platform.distribution.application.dto.UploadStatusDTO;
import com.grace.platform.distribution.interfaces.dto.request.PublishRequest;
import com.grace.platform.distribution.interfaces.dto.response.AuthUrlResponse;
import com.grace.platform.distribution.interfaces.dto.response.PlatformInfoResponse;
import com.grace.platform.distribution.interfaces.dto.response.PublishResultResponse;
import com.grace.platform.distribution.interfaces.dto.response.UploadStatusResponse;
import com.grace.platform.shared.application.dto.ApiResponse;
import com.grace.platform.shared.domain.id.MetadataId;
import com.grace.platform.shared.domain.id.VideoId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 视频分发控制器。
 * <p>
 * 负责处理视频分发到各平台的 REST API 端点，包括：
 * <ul>
 *   <li>发布视频到平台</li>
 *   <li>查询发布状态</li>
 *   <li>管理 OAuth 授权流程</li>
 *   <li>列出可用平台</li>
 *   <li>查询发布记录</li>
 * </ul>
 * <p>
 * 端点列表：
 * <ul>
 *   <li>D1: POST /api/distribution/publish - 发布视频到平台</li>
 *   <li>D2: GET /api/distribution/status/{taskId} - 查询发布状态</li>
 *   <li>D3: POST /api/distribution/auth/{platform} - 发起 OAuth 授权</li>
 *   <li>D4: GET /api/distribution/auth/{platform}/callback - OAuth 授权回调</li>
 *   <li>D5: GET /api/distribution/platforms - 可用平台列表及授权状态</li>
 *   <li>D6: GET /api/distribution/records/{videoId} - 获取视频发布记录</li>
 * </ul>
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/distribution")
public class DistributionController {

    private final DistributionApplicationService distributionApplicationService;

    /**
     * 构造器注入依赖。
     *
     * @param distributionApplicationService 分发应用服务
     */
    public DistributionController(DistributionApplicationService distributionApplicationService) {
        this.distributionApplicationService = distributionApplicationService;
    }

    // ==================== D1: 发布视频到平台 ====================

    /**
     * 发布视频到指定平台。
     * <p>
     * 通过 platform 字段路由到对应的 VideoDistributor 实现。
     * 校验视频状态为 READY_TO_PUBLISH，创建发布记录并启动上传。
     *
     * @param request 发布请求，包含视频 ID、元数据 ID、平台标识和隐私状态
     * @return 发布结果，包含发布记录 ID 和上传任务 ID
     */
    @PostMapping("/publish")
    public ResponseEntity<ApiResponse<PublishResultResponse>> publish(
            @Valid @RequestBody PublishRequest request) {

        VideoId videoId = new VideoId(request.videoId());
        MetadataId metadataId = new MetadataId(request.metadataId());

        PublishCommand command = new PublishCommand(
                videoId,
                metadataId,
                request.platform(),
                request.privacyStatus()
        );

        PublishResultDTO dto = distributionApplicationService.publish(command);

        PublishResultResponse response = new PublishResultResponse(
                dto.publishRecordId(),
                dto.videoId(),
                dto.platform(),
                dto.uploadTaskId(),
                dto.status(),
                dto.createdAt()
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== D2: 查询发布状态 ====================

    /**
     * 查询发布上传状态。
     * <p>
     * 前端轮询此接口展示发布进度。
     * 根据平台返回的最新状态更新 PublishRecord。
     * 如果发布完成，会更新视频状态为 PUBLISHED 并发布 VideoPublishedEvent。
     *
     * @param taskId 上传任务 ID
     * @return 上传状态，包含进度百分比和错误信息
     */
    @GetMapping("/status/{taskId}")
    public ResponseEntity<ApiResponse<UploadStatusResponse>> getUploadStatus(
            @PathVariable String taskId) {

        UploadStatusDTO dto = distributionApplicationService.getUploadStatus(taskId);

        UploadStatusResponse response = new UploadStatusResponse(
                dto.publishRecordId(),
                dto.taskId(),
                dto.platform(),
                dto.status(),
                dto.progressPercent(),
                dto.videoUrl(),
                dto.errorMessage(),
                dto.publishedAt()
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== D3: 发起 OAuth 授权 ====================

    /**
     * 发起平台 OAuth 授权。
     * <p>
     * 返回 OAuth 授权 URL，前端引导用户跳转。
     * 生成 state 参数用于 CSRF 防护。
     *
     * @param platform 平台标识，如 "youtube"
     * @param request  包含回调地址的请求体
     * @return 授权 URL 和 state 参数
     */
    @PostMapping("/auth/{platform}")
    public ResponseEntity<ApiResponse<AuthUrlResponse>> initiateAuth(
            @PathVariable String platform,
            @Valid @RequestBody AuthCallbackRequest request) {

        DistributionApplicationService.AuthUrlDTO dto =
                distributionApplicationService.initiateAuth(platform, request.redirectUri());

        AuthUrlResponse response = new AuthUrlResponse(dto.authUrl(), dto.state());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== D4: OAuth 授权回调 ====================

    /**
     * OAuth 授权回调端点。
     * <p>
     * 平台 OAuth 服务器将用户重定向到此 URL 并携带 authorization code。
     * 后端用 code 交换 access_token / refresh_token 并加密存储。
     * 完成后重定向回前端页面。
     *
     * @param platform 平台标识
     * @param code     授权码
     * @param state    状态参数（CSRF 校验）
     * @return 302 重定向到前端页面，URL 携带 auth=success 或 auth=failed 参数
     */
    @GetMapping("/auth/{platform}/callback")
    public ResponseEntity<Void> handleAuthCallback(
            @PathVariable String platform,
            @RequestParam String code,
            @RequestParam String state) {

        distributionApplicationService.handleAuthCallback(platform, code, state);

        // 重定向到前端成功页面
        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", "/settings/oauth/callback?auth=success")
                .build();
    }

    // ==================== D5: 可用平台列表 ====================

    /**
     * 获取所有可用的分发平台列表及其 OAuth 授权状态。
     * <p>
     * 用于前端展示平台选择界面，显示各平台的连接状态。
     *
     * @return 平台信息列表，包含授权状态和过期信息
     */
    @GetMapping("/platforms")
    public ResponseEntity<ApiResponse<List<PlatformInfoResponse>>> listPlatforms() {

        List<PlatformInfoDTO> dtos = distributionApplicationService.listPlatforms();

        List<PlatformInfoResponse> response = dtos.stream()
                .map(dto -> new PlatformInfoResponse(
                        dto.platform(),
                        dto.displayName(),
                        dto.authorized(),
                        dto.authExpired(),
                        dto.enabled()
                ))
                .toList();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== D6: 获取视频发布记录 ====================

    /**
     * 获取指定视频的所有发布记录。
     * <p>
     * 用于展示视频详情页面的发布历史。
     *
     * @param videoId 视频 ID
     * @return 发布记录列表
     */
    @GetMapping("/records/{videoId}")
    public ResponseEntity<ApiResponse<List<PublishRecordResponse>>> getPublishRecords(
            @PathVariable String videoId) {

        VideoId vid = new VideoId(videoId);
        List<PublishRecordDTO> dtos = distributionApplicationService.getPublishRecords(vid);

        List<PublishRecordResponse> response = dtos.stream()
                .map(this::toPublishRecordResponse)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 将 PublishRecordDTO 转换为 PublishRecordResponse。
     *
     * @param dto 发布记录 DTO
     * @return 发布记录响应
     */
    private PublishRecordResponse toPublishRecordResponse(PublishRecordDTO dto) {
        return new PublishRecordResponse(
                dto.publishRecordId(),
                dto.platform(),
                dto.status(),
                dto.videoUrl(),
                dto.progressPercent(),
                dto.errorMessage(),
                dto.publishedAt(),
                dto.createdAt()
        );
    }

    // ==================== 内部请求/响应记录 ====================

    /**
     * OAuth 授权请求。
     *
     * @param redirectUri OAuth 授权完成后回调的前端 URL
     */
    private record AuthCallbackRequest(
            @NotBlank(message = "回调地址不能为空")
            String redirectUri
    ) {
    }

    /**
     * 发布记录响应。
     * <p>
     * 对应 API D6 响应的数组元素。
     */
    private record PublishRecordResponse(
            String publishRecordId,
            String platform,
            com.grace.platform.distribution.domain.PublishStatus status,
            String videoUrl,
            int progressPercent,
            String errorMessage,
            java.time.LocalDateTime publishedAt,
            java.time.LocalDateTime createdAt
    ) {
    }
}
