package com.grace.platform.metadata.interfaces;

import com.grace.platform.metadata.application.MetadataApplicationService;
import com.grace.platform.metadata.application.command.UpdateMetadataCommand;
import com.grace.platform.metadata.application.dto.VideoMetadataDTO;
import com.grace.platform.metadata.interfaces.dto.request.GenerateMetadataRequest;
import com.grace.platform.metadata.interfaces.dto.request.UpdateMetadataRequest;
import com.grace.platform.metadata.interfaces.dto.response.VideoMetadataResponse;
import com.grace.platform.shared.application.dto.ApiResponse;
import com.grace.platform.shared.domain.id.MetadataId;
import com.grace.platform.shared.domain.id.VideoId;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 元数据管理控制器。
 * <p>
 * 负责处理元数据相关的 REST API 端点。
 * <p>
 * 端点列表：
 * <ul>
 *   <li>C1: POST /api/metadata/generate - AI 生成元数据</li>
 *   <li>C2: PUT /api/metadata/{id} - 更新元数据</li>
 *   <li>C3: POST /api/metadata/{id}/regenerate - 重新生成元数据</li>
 *   <li>C4: POST /api/metadata/{id}/confirm - 确认元数据</li>
 *   <li>C5: GET /api/metadata/video/{videoId} - 按视频 ID 获取元数据</li>
 * </ul>
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/metadata")
public class MetadataController {

    private final MetadataApplicationService metadataApplicationService;

    /**
     * 构造器注入依赖。
     *
     * @param metadataApplicationService 元数据应用服务
     */
    public MetadataController(MetadataApplicationService metadataApplicationService) {
        this.metadataApplicationService = metadataApplicationService;
    }

    // ==================== C1: AI 生成元数据 ====================

    /**
     * 为视频生成 AI 元数据。
     * <p>
     * 调用阿里云 LLM 根据视频信息生成标题、描述和标签。
     * 通常由 VideoUploadedEvent 自动触发，也可由前端手动调用。
     *
     * @param request 生成请求，包含视频 ID
     * @return 生成的元数据
     */
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<VideoMetadataResponse>> generateMetadata(
            @Valid @RequestBody GenerateMetadataRequest request) {

        VideoId videoId = new VideoId(request.videoId());
        VideoMetadataDTO dto = metadataApplicationService.generateMetadata(videoId);

        VideoMetadataResponse response = toResponse(dto);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== C2: 更新元数据 ====================

    /**
     * 更新元数据。
     * <p>
     * 用户编辑元数据字段。更新后 source 变为 AI_EDITED（原为 AI_GENERATED 时）或 MANUAL。
     * 已确认的元数据不可编辑。
     *
     * @param id      元数据 ID
     * @param request 更新请求
     * @return 更新后的元数据
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VideoMetadataResponse>> updateMetadata(
            @PathVariable String id,
            @Valid @RequestBody UpdateMetadataRequest request) {

        MetadataId metadataId = new MetadataId(id);
        UpdateMetadataCommand command = new UpdateMetadataCommand(
                request.title(),
                request.description(),
                request.tags()
        );

        VideoMetadataDTO dto = metadataApplicationService.updateMetadata(metadataId, command);

        VideoMetadataResponse response = toResponse(dto);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== C3: 重新生成元数据 ====================

    /**
     * 重新生成元数据。
     * <p>
     * 重新调用 LLM 生成元数据，覆盖当前版本。
     * source 重置为 AI_GENERATED。
     *
     * @param id 元数据 ID
     * @return 重新生成的元数据
     */
    @PostMapping("/{id}/regenerate")
    public ResponseEntity<ApiResponse<VideoMetadataResponse>> regenerateMetadata(
            @PathVariable String id) {

        MetadataId metadataId = new MetadataId(id);
        VideoMetadataDTO dto = metadataApplicationService.regenerateMetadata(metadataId);

        VideoMetadataResponse response = toResponse(dto);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== C4: 确认元数据 ====================

    /**
     * 确认元数据。
     * <p>
     * 将视频状态更新为 READY_TO_PUBLISH，发布 MetadataConfirmedEvent。
     * 此操作不可逆。
     *
     * @param id 元数据 ID
     * @return 确认后的元数据
     */
    @PostMapping("/{id}/confirm")
    public ResponseEntity<ApiResponse<VideoMetadataResponse>> confirmMetadata(
            @PathVariable String id) {

        MetadataId metadataId = new MetadataId(id);
        VideoMetadataDTO dto = metadataApplicationService.confirmMetadata(metadataId);

        VideoMetadataResponse response = toResponse(dto);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== C5: 按视频 ID 获取元数据 ====================

    /**
     * 根据视频 ID 获取关联的最新元数据。
     * <p>
     * 适用于仅需刷新元数据的场景（如重新生成后）。
     * 如无元数据则返回 null。
     *
     * @param videoId 视频 ID
     * @return 最新元数据，如无则返回 null
     */
    @GetMapping("/video/{videoId}")
    public ResponseEntity<ApiResponse<VideoMetadataResponse>> getMetadataByVideoId(
            @PathVariable String videoId) {

        VideoId vid = new VideoId(videoId);
        VideoMetadataDTO dto = metadataApplicationService.getMetadataByVideoId(vid);

        if (dto == null) {
            return ResponseEntity.ok(ApiResponse.success(null));
        }

        VideoMetadataResponse response = toResponse(dto);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 将 DTO 转换为响应对象。
     *
     * @param dto 元数据 DTO
     * @return 元数据响应
     */
    private VideoMetadataResponse toResponse(VideoMetadataDTO dto) {
        return new VideoMetadataResponse(
                dto.metadataId(),
                dto.videoId(),
                dto.title(),
                dto.description(),
                dto.tags(),
                dto.source().name(),
                dto.createdAt(),
                dto.updatedAt()
        );
    }
}
