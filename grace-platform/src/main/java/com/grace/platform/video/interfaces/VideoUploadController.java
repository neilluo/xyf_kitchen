package com.grace.platform.video.interfaces;

import com.grace.platform.shared.application.dto.ApiResponse;
import com.grace.platform.shared.application.dto.PageResponse;
import com.grace.platform.video.application.VideoApplicationService;
import com.grace.platform.video.application.command.VideoQueryCommand;
import com.grace.platform.video.application.dto.*;
import com.grace.platform.video.interfaces.dto.response.VideoDetailResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 视频查询控制器。
 * <p>
 * 负责视频列表与详情查询的 REST API 端点。视频上传统一通过
 * {@link ServerUploadController}（{@code /api/videos/upload/server/...}）走服务端分片上传。
 * <p>
 * 端点列表：
 * <ul>
 *   <li>B5: GET /api/videos - 视频列表（分页/搜索/筛选）</li>
 *   <li>B6: GET /api/videos/{videoId} - 视频详情</li>
 * </ul>
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/videos")
public class VideoUploadController {

    private final VideoApplicationService videoApplicationService;

    /**
     * 构造器注入依赖。
     *
     * @param videoApplicationService 视频应用服务
     */
    public VideoUploadController(VideoApplicationService videoApplicationService) {
        this.videoApplicationService = videoApplicationService;
    }

    // ==================== B5: 视频列表查询 ====================

    /**
     * 分页查询视频列表。
     * <p>
     * 支持文件名搜索、状态筛选、日期范围筛选。
     *
     * @param page       页码（从 1 开始）
     * @param pageSize   每页条数
     * @param keyword    文件名搜索关键词
     * @param status     状态筛选（逗号分隔）
     * @param startDate  上传起始日期
     * @param endDate    上传截止日期
     * @param sort       排序字段
     * @param order      排序方向
     * @return 分页视频列表
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<VideoListItemResponse>>> listVideos(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String order) {

        VideoQueryCommand command = new VideoQueryCommand(
            keyword,
            parseStatusList(status),
            startDate,
            endDate,
            page,
            pageSize,
            sort,
            order
        );

        PageResponse<VideoListItemDTO> pageDto = videoApplicationService.listVideos(command);

        List<VideoListItemResponse> items = pageDto.items().stream()
            .map(this::toVideoListItemResponse)
            .collect(Collectors.toList());

        PageResponse<VideoListItemResponse> response = PageResponse.of(
            items,
            pageDto.total(),
            pageDto.page(),
            pageDto.pageSize()
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== B6: 视频详情查询 ====================

    /**
     * 获取视频详情。
     * <p>
     * 包含视频基本信息、元数据和发布记录。
     *
     * @param videoId 视频 ID
     * @return 视频详情
     */
    @GetMapping("/{videoId}")
    public ResponseEntity<ApiResponse<VideoDetailResponse>> getVideoDetail(
            @PathVariable String videoId) {

        VideoDetailDTO dto = videoApplicationService.getVideoDetail(videoId);

        VideoDetailResponse response = toVideoDetailResponse(dto);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== 私有辅助方法 ====================

    private List<com.grace.platform.video.domain.VideoStatus> parseStatusList(String status) {
        if (status == null || status.isBlank()) {
            return List.of();
        }
        return Arrays.stream(status.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(s -> {
                try {
                    return com.grace.platform.video.domain.VideoStatus.valueOf(s.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return null;
                }
            })
            .filter(java.util.Objects::nonNull)
            .collect(Collectors.toList());
    }

    private String formatDuration(Duration duration) {
        if (duration == null) {
            return "PT0S";
        }
        return duration.toString();
    }

    private VideoListItemResponse toVideoListItemResponse(VideoListItemDTO dto) {
        return new VideoListItemResponse(
            dto.videoId(),
            dto.fileName(),
            dto.format(),
            dto.fileSize(),
            formatDuration(dto.duration()),
            dto.status().name(),
            dto.thumbnailUrl(),
            dto.hasMetadata(),
            dto.createdAt(),
            dto.updatedAt()
        );
    }

    private VideoDetailResponse toVideoDetailResponse(VideoDetailDTO dto) {
        VideoDetailResponse.MetadataResponse metadata = null;
        if (dto.metadata() != null) {
            metadata = new VideoDetailResponse.MetadataResponse(
                dto.metadata().metadataId(),
                dto.metadata().title(),
                dto.metadata().description(),
                dto.metadata().tags(),
                dto.metadata().source(),
                dto.metadata().createdAt(),
                dto.metadata().updatedAt()
            );
        }

        List<VideoDetailResponse.PublishRecordResponse> publishRecords = List.of();
        if (dto.publishRecords() != null) {
            publishRecords = dto.publishRecords().stream()
                .map(pr -> new VideoDetailResponse.PublishRecordResponse(
                    pr.publishRecordId(),
                    pr.platform(),
                    pr.status(),
                    pr.videoUrl(),
                    pr.progressPercent(),
                    pr.errorMessage(),
                    pr.publishedAt(),
                    pr.createdAt()
                ))
                .collect(Collectors.toList());
        }

        return new VideoDetailResponse(
            dto.videoId(),
            dto.fileName(),
            dto.format(),
            dto.fileSize(),
            formatDuration(dto.duration()),
            dto.filePath(),
            dto.status().name(),
            dto.thumbnailUrl(),
            dto.createdAt(),
            dto.updatedAt(),
            metadata,
            publishRecords
        );
    }

    // ==================== 内部响应记录 ====================

    private record VideoListItemResponse(
        String videoId,
        String fileName,
        String format,
        long fileSize,
        String duration,
        String status,
        String thumbnailUrl,
        boolean hasMetadata,
        java.time.LocalDateTime createdAt,
        java.time.LocalDateTime updatedAt
    ) {
    }
}
