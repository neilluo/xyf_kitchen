package com.grace.platform.promotion.interfaces;

import com.grace.platform.promotion.application.ChannelApplicationService;
import com.grace.platform.promotion.application.command.CreateChannelCommand;
import com.grace.platform.promotion.application.command.UpdateChannelCommand;
import com.grace.platform.promotion.application.dto.ChannelDTO;
import com.grace.platform.promotion.domain.ChannelStatus;
import com.grace.platform.promotion.domain.ChannelType;
import com.grace.platform.promotion.interfaces.dto.request.CreateChannelRequest;
import com.grace.platform.promotion.interfaces.dto.request.UpdateChannelRequest;
import com.grace.platform.promotion.interfaces.dto.response.ChannelResponse;
import com.grace.platform.shared.application.dto.ApiResponse;
import com.grace.platform.shared.domain.id.ChannelId;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 渠道管理控制器。
 * <p>
 * 负责处理推广渠道相关的 REST API 端点，包括：
 * <ul>
 *   <li>创建渠道</li>
 *   <li>更新渠道配置</li>
 *   <li>删除渠道</li>
 *   <li>列出所有渠道</li>
 *   <li>获取单个渠道详情</li>
 * </ul>
 * <p>
 * 端点列表：
 * <ul>
 *   <li>E1: POST /api/channels - 创建渠道</li>
 *   <li>E2: PUT /api/channels/{id} - 更新渠道</li>
 *   <li>E3: DELETE /api/channels/{id} - 删除渠道</li>
 *   <li>E4: GET /api/channels - 列出渠道</li>
 *   <li>E5: GET /api/channels/{id} - 获取渠道详情</li>
 * </ul>
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/channels")
public class ChannelController {

    private final ChannelApplicationService channelApplicationService;

    /**
     * 构造器注入依赖。
     *
     * @param channelApplicationService 渠道应用服务
     */
    public ChannelController(ChannelApplicationService channelApplicationService) {
        this.channelApplicationService = channelApplicationService;
    }

    // ==================== E1: 创建渠道 ====================

    /**
     * 创建推广渠道。
     * <p>
     * API Key 以 AES-256-GCM 加密后存储。
     *
     * @param request 创建渠道请求
     * @return 创建的渠道信息
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ChannelResponse>> createChannel(
            @Valid @RequestBody CreateChannelRequest request) {

        CreateChannelCommand command = new CreateChannelCommand(
                request.name(),
                ChannelType.valueOf(request.type()),
                request.channelUrl(),
                request.apiKey(),
                request.priority()
        );

        ChannelDTO dto = channelApplicationService.createChannel(command);

        ChannelResponse response = toChannelResponse(dto);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== E2: 更新渠道 ====================

    /**
     * 更新推广渠道配置。
     * <p>
     * 支持部分更新，包括启用/禁用切换。
     *
     * @param id      渠道 ID
     * @param request 更新渠道请求
     * @return 更新后的渠道信息
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ChannelResponse>> updateChannel(
            @PathVariable String id,
            @Valid @RequestBody UpdateChannelRequest request) {

        ChannelId channelId = new ChannelId(id);

        UpdateChannelCommand command = new UpdateChannelCommand(
                request.name(),
                request.type() != null ? ChannelType.valueOf(request.type()) : null,
                request.channelUrl(),
                request.apiKey(),
                request.priority(),
                request.status() != null ? ChannelStatus.valueOf(request.status()) : null
        );

        ChannelDTO dto = channelApplicationService.updateChannel(channelId, command);

        ChannelResponse response = toChannelResponse(dto);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== E3: 删除渠道 ====================

    /**
     * 删除推广渠道。
     * <p>
     * 如有关联推广记录则软删除（标记为 DISABLED），否则硬删除。
     *
     * @param id 渠道 ID
     * @return 空响应
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteChannel(@PathVariable String id) {

        ChannelId channelId = new ChannelId(id);
        channelApplicationService.deleteChannel(channelId);

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ==================== E4: 列出渠道 ====================

    /**
     * 列出所有推广渠道。
     * <p>
     * 支持按状态和类型筛选。
     *
     * @param status 状态筛选（可选）
     * @param type   类型筛选（可选）
     * @return 渠道列表
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ChannelResponse>>> listChannels(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type) {

        ChannelStatus statusFilter = status != null ? ChannelStatus.valueOf(status) : null;

        List<ChannelDTO> dtos = channelApplicationService.listChannels(statusFilter);

        // 如指定了类型筛选，在内存中过滤
        List<ChannelResponse> responses = dtos.stream()
                .filter(dto -> type == null || dto.type().name().equals(type))
                .map(this::toChannelResponse)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    // ==================== E5: 获取渠道详情 ====================

    /**
     * 获取单个渠道详情。
     *
     * @param id 渠道 ID
     * @return 渠道详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ChannelResponse>> getChannel(@PathVariable String id) {

        ChannelId channelId = new ChannelId(id);
        ChannelDTO dto = channelApplicationService.getChannel(channelId);

        ChannelResponse response = toChannelResponse(dto);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 将 ChannelDTO 转换为 ChannelResponse。
     *
     * @param dto 渠道 DTO
     * @return 渠道响应
     */
    private ChannelResponse toChannelResponse(ChannelDTO dto) {
        return ChannelResponse.of(
                dto.channelId(),
                dto.name(),
                dto.type().name(),
                dto.channelUrl(),
                dto.hasApiKey(),
                dto.priority(),
                dto.status().name(),
                dto.createdAt(),
                dto.updatedAt()
        );
    }
}
