package com.grace.platform.video.interfaces;

import com.grace.platform.shared.application.dto.ApiResponse;
import com.grace.platform.shared.application.dto.PageResponse;
import com.grace.platform.video.application.VideoApplicationService;
import com.grace.platform.video.application.command.UploadInitCommand;
import com.grace.platform.video.application.command.VideoQueryCommand;
import com.grace.platform.video.application.dto.*;
import com.grace.platform.video.domain.VideoFormat;
import com.grace.platform.video.interfaces.dto.request.UploadInitRequest;
import com.grace.platform.video.interfaces.dto.response.UploadInitResponse;
import com.grace.platform.video.interfaces.dto.response.VideoDetailResponse;
import com.grace.platform.video.interfaces.dto.response.VideoInfoResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 视频上传控制器。
 * <p>
 * 负责处理视频分片上传相关的 REST API 端点。
 * <p>
 * 端点列表：
 * <ul>
 *   <li>B1: POST /api/videos/upload/init - 初始化上传</li>
 *   <li>B2: POST /api/videos/upload/{uploadId}/chunk - 上传分片</li>
 *   <li>B3: POST /api/videos/upload/{uploadId}/complete - 完成上传</li>
 *   <li>B4: GET /api/videos/upload/{uploadId}/progress - 查询上传进度</li>
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

    // ==================== B1: 初始化上传 ====================

    /**
     * 初始化分片上传。
     * <p>
     * 校验文件格式和大小，创建上传会话。
     *
     * @param request 初始化请求
     * @return 上传会话信息
     */
    @PostMapping("/upload/init")
    public ResponseEntity<ApiResponse<UploadInitResponse>> initUpload(
            @Valid @RequestBody UploadInitRequest request) {
        
        // 转换格式字符串为枚举
        VideoFormat format;
        try {
            format = VideoFormat.valueOf(request.format().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new com.grace.platform.shared.infrastructure.exception.BusinessRuleViolationException(
                com.grace.platform.shared.ErrorCode.UNSUPPORTED_VIDEO_FORMAT,
                String.format("Unsupported video format: %s. Supported formats: MP4, MOV, AVI, MKV", request.format())
            );
        }

        UploadInitCommand command = new UploadInitCommand(
            request.fileName(),
            request.fileSize(),
            format
        );

        UploadInitDTO dto = videoApplicationService.initUpload(command);

        UploadInitResponse response = new UploadInitResponse(
            dto.uploadId(),
            dto.totalChunks(),
            dto.chunkSize(),
            dto.expiresAt()
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== B2: 上传分片 ====================

    /**
     * 上传单个分片。
     * <p>
     * 接收分片文件并存储到临时目录。
     *
     * @param uploadId   上传会话 ID
     * @param chunkIndex 分片索引（从请求参数或表单数据获取）
     * @param chunk      分片文件
     * @return 分片上传结果
     */
    @PostMapping(value = "/upload/{uploadId}/chunk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ChunkUploadResponse>> uploadChunk(
            @PathVariable String uploadId,
            @RequestParam("chunkIndex") int chunkIndex,
            @RequestParam("chunk") MultipartFile chunk) {
        
        try {
            ChunkUploadDTO dto = videoApplicationService.uploadChunk(
                uploadId,
                chunkIndex,
                chunk.getInputStream()
            );

            ChunkUploadResponse response = new ChunkUploadResponse(
                dto.uploadId(),
                dto.chunkIndex(),
                dto.uploadedChunks(),
                dto.totalChunks()
            );

            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read chunk file", e);
        }
    }

    // ==================== B3: 完成上传 ====================

    /**
     * 完成上传。
     * <p>
     * 合并所有分片，提取视频信息，创建视频记录。
     *
     * @param uploadId 上传会话 ID
     * @return 视频信息
     */
    @PostMapping("/upload/{uploadId}/complete")
    public ResponseEntity<ApiResponse<VideoInfoResponse>> completeUpload(
            @PathVariable String uploadId) {
        
        VideoInfoDTO dto = videoApplicationService.completeUpload(uploadId);

        VideoInfoResponse response = new VideoInfoResponse(
            dto.videoId(),
            dto.fileName(),
            dto.fileSize(),
            dto.format(),
            formatDuration(dto.duration()),
            dto.status().name(),
            dto.createdAt()
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== B4: 查询上传进度 ====================

    /**
     * 查询上传进度。
     * <p>
     * 用于前端轮询展示上传进度条。
     *
     * @param uploadId 上传会话 ID
     * @return 上传进度信息
     */
    @GetMapping("/upload/{uploadId}/progress")
    public ResponseEntity<ApiResponse<UploadProgressResponse>> getUploadProgress(
            @PathVariable String uploadId) {
        
        UploadProgressDTO dto = videoApplicationService.getUploadProgress(uploadId);

        UploadProgressResponse response = new UploadProgressResponse(
            dto.uploadId(),
            dto.uploadedChunks(),
            dto.totalChunks(),
            dto.progressPercent(),
            dto.status().name()
        );

        return ResponseEntity.ok(ApiResponse.success(response));
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

    /**
     * 解析状态列表字符串。
     */
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

    /**
     * 格式化 Duration 为 ISO 8601 字符串。
     */
    private String formatDuration(Duration duration) {
        if (duration == null) {
            return "PT0S";
        }
        return duration.toString();
    }

    /**
     * 转换 VideoListItemDTO 为 VideoListItemResponse。
     */
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

    /**
     * 转换 VideoDetailDTO 为 VideoDetailResponse。
     */
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

    /**
     * 分片上传响应。
     */
    private record ChunkUploadResponse(
        String uploadId,
        int chunkIndex,
        int uploadedChunks,
        int totalChunks
    ) {
    }

    /**
     * 上传进度响应。
     */
    private record UploadProgressResponse(
        String uploadId,
        int uploadedChunks,
        int totalChunks,
        int progressPercent,
        String status
    ) {
    }

    /**
     * 视频列表项响应。
     */
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
