package com.grace.platform.video.interfaces;

import com.grace.platform.shared.application.dto.ApiResponse;
import com.grace.platform.shared.ErrorCode;
import com.grace.platform.shared.infrastructure.exception.BusinessRuleViolationException;
import com.grace.platform.video.application.ServerUploadApplicationService;
import com.grace.platform.video.application.command.ServerUploadInitCommand;
import com.grace.platform.video.application.dto.ServerChunkUploadDTO;
import com.grace.platform.video.application.dto.ServerUploadCompleteDTO;
import com.grace.platform.video.application.dto.ServerUploadInitDTO;
import com.grace.platform.video.domain.VideoFormat;
import com.grace.platform.video.interfaces.dto.request.ServerUploadInitRequest;
import com.grace.platform.video.interfaces.dto.request.ServerChunkUploadRequest;
import com.grace.platform.video.interfaces.dto.response.ServerUploadInitResponse;
import com.grace.platform.video.interfaces.dto.response.ServerChunkUploadResponse;
import com.grace.platform.video.interfaces.dto.response.ServerUploadCompleteResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@RestController
@RequestMapping("/api/videos/upload/server")
public class ServerUploadController {

    private final ServerUploadApplicationService serverUploadApplicationService;

    public ServerUploadController(ServerUploadApplicationService serverUploadApplicationService) {
        this.serverUploadApplicationService = serverUploadApplicationService;
    }

    @PostMapping("/init")
    public ResponseEntity<ApiResponse<ServerUploadInitResponse>> initUpload(
            @Valid @RequestBody ServerUploadInitRequest request) {
        
        VideoFormat format;
        try {
            format = VideoFormat.valueOf(request.format().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleViolationException(
                ErrorCode.UNSUPPORTED_VIDEO_FORMAT,
                String.format("Unsupported video format: %s. Supported formats: MP4, MOV, AVI, MKV", request.format())
            );
        }

        ServerUploadInitCommand command = new ServerUploadInitCommand(
            request.fileName(),
            request.fileSize(),
            format
        );

        ServerUploadInitDTO dto = serverUploadApplicationService.initUpload(command);

        ServerUploadInitResponse response = new ServerUploadInitResponse(
            dto.uploadId(),
            dto.totalChunks(),
            dto.chunkSize(),
            dto.tempDirectory(),
            dto.expiresAt()
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping(value = "/{uploadId}/chunk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ServerChunkUploadResponse>> uploadChunk(
            @PathVariable String uploadId,
            @RequestParam("chunkIndex") int chunkIndex,
            @RequestPart("chunk") MultipartFile chunk) throws IOException {
        
        InputStream chunkStream = chunk.getInputStream();
        
        ServerChunkUploadDTO dto = serverUploadApplicationService.uploadChunk(
            uploadId, 
            chunkIndex, 
            chunkStream
        );

        ServerChunkUploadResponse response = new ServerChunkUploadResponse(
            dto.uploadId(),
            dto.chunkIndex(),
            dto.uploadedChunks(),
            dto.totalChunks()
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{uploadId}/complete")
    public ResponseEntity<ApiResponse<ServerUploadCompleteResponse>> completeUpload(
            @PathVariable String uploadId) {
        
        ServerUploadCompleteDTO dto = serverUploadApplicationService.completeUpload(uploadId);

        ServerUploadCompleteResponse response = new ServerUploadCompleteResponse(
            dto.videoId(),
            dto.fileName(),
            dto.fileSize(),
            dto.format(),
            formatDuration(dto.duration()),
            dto.status().name(),
            dto.storageUrl(),
            dto.createdAt()
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private String formatDuration(java.time.Duration duration) {
        if (duration == null) {
            return "PT0S";
        }
        return duration.toString();
    }
}