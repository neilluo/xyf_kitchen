package com.grace.platform.distribution.interfaces;

import com.grace.platform.shared.application.dto.ApiResponse;
import com.grace.platform.distribution.infrastructure.youtube.YouTubeOAuthService;
import com.grace.platform.distribution.infrastructure.youtube.YouTubeUploadRequest;
import com.grace.platform.distribution.infrastructure.youtube.YouTubeUploadResult;
import com.grace.platform.distribution.infrastructure.youtube.YouTubeUploadService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * YouTube 发布控制器
 */
@RestController
@RequestMapping("/api/distribution/youtube")
public class YouTubeController {
    
    private static final Logger logger = LoggerFactory.getLogger(YouTubeController.class);
    
    private final YouTubeOAuthService oAuthService;
    private final YouTubeUploadService uploadService;
    
    public YouTubeController(YouTubeOAuthService oAuthService, YouTubeUploadService uploadService) {
        this.oAuthService = oAuthService;
        this.uploadService = uploadService;
    }
    
    /**
     * 开始 YouTube OAuth 授权
     * GET /api/distribution/youtube/auth
     */
    @GetMapping("/auth")
    public void authenticate(HttpServletResponse response) throws IOException {
        String authUrl = oAuthService.getAuthorizationUrl();
        logger.info("Redirecting to YouTube OAuth: {}", authUrl);
        response.sendRedirect(authUrl);
    }
    
    /**
     * YouTube OAuth 回调
     * GET /api/distribution/youtube/callback
     */
    @GetMapping("/callback")
    public ResponseEntity<ApiResponse<String>> handleCallback(@RequestParam("code") String code) {
        try {
            oAuthService.handleCallback(code);
            return ResponseEntity.ok(ApiResponse.success("YouTube 授权成功"));
        } catch (IOException e) {
            logger.error("YouTube OAuth callback failed", e);
            return ResponseEntity.status(500)
                .body(ApiResponse.error("授权失败: " + e.getMessage()));
        }
    }
    
    /**
     * 检查 YouTube 授权状态
     * GET /api/distribution/youtube/status
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Boolean>> checkStatus() {
        boolean authorized = oAuthService.isAuthorized();
        return ResponseEntity.ok(ApiResponse.success(authorized));
    }
    
    /**
     * 上传视频到 YouTube
     * POST /api/distribution/youtube/upload
     */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<YouTubeUploadResult>> uploadVideo(@RequestBody YouTubeUploadRequest request) {
        try {
            YouTubeUploadResult result = uploadService.uploadVideo(request);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            logger.error("YouTube upload failed", e);
            return ResponseEntity.status(500)
                .body(ApiResponse.error("上传失败: " + e.getMessage()));
        }
    }
}
