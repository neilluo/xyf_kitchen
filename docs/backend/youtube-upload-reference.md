# YouTube 上传功能参考实现

> 基于 Spring Boot + YouTube Data API v3 的视频上传实现参考

---

## 📚 参考资料

1. **GitHub 示例项目**: [spring-boot-youtube-api](https://github.com/dipacode90/spring-boot-youtube-api)
2. **Medium 教程**: [How I Automated Uploading Videos to YouTube](https://medium.com/@subash12396/how-i-automated-uploading-videos-to-youtube-a747763f2104)
3. **YouTube Data API 官方文档**: https://developers.google.com/youtube/v3

---

## 🛠️ 技术栈

- **Spring Boot** - 后端框架
- **YouTube Data API v3** - YouTube 官方 API
- **Google OAuth 2.0** - 身份认证
- **Google API Client Library** - Java 客户端库

---

## 📦 Maven 依赖

```xml
<!-- Google API Client -->
<dependency>
    <groupId>com.google.api-client</groupId>
    <artifactId>google-api-client</artifactId>
    <version>1.35.2</version>
</dependency>

<!-- Google OAuth2 -->
<dependency>
    <groupId>com.google.auth</groupId>
    <artifactId>google-auth-library-oauth2-http</artifactId>
    <version>1.18.0</version>
</dependency>

<!-- YouTube Data API -->
<dependency>
    <groupId>com.google.apis</groupId>
    <artifactId>google-api-services-youtube</artifactId>
    <version>v3-rev222-1.25.0</version>
</dependency>

<!-- HTTP Transport -->
<dependency>
    <groupId>com.google.http-client</groupId>
    <artifactId>google-http-client-jackson2</artifactId>
    <version>1.43.3</version>
</dependency>
```

---

## 🔧 配置

### 1. application.yml

```yaml
youtube:
  client-id: ${YOUTUBE_CLIENT_ID}
  client-secret: ${YOUTUBE_CLIENT_SECRET}
  redirect-uri: ${YOUTUBE_REDIRECT_URI:http://localhost:8080/api/distribution/auth/youtube/callback}
  scopes: https://www.googleapis.com/auth/youtube.upload,https://www.googleapis.com/auth/youtube.readonly
```

### 2. YouTubeConfig.java

```java
@Configuration
@ConfigurationProperties(prefix = "youtube")
public class YouTubeConfig {
    
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private List<String> scopes;
    
    // Getters and Setters
}
```

---

## 🔐 OAuth 2.0 认证流程

### 1. YouTubeAuthController.java

```java
@RestController
@RequestMapping("/api/distribution/auth/youtube")
public class YouTubeAuthController {
    
    private static final String APPLICATION_NAME = "Grace Platform";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    
    private final YouTubeConfig config;
    private final OAuthTokenRepository tokenRepository;
    
    private static GoogleAuthorizationCodeFlow flow;
    
    @PostConstruct
    public void init() throws Exception {
        GoogleClientSecrets clientSecrets = new GoogleClientSecrets()
            .setWeb(new GoogleClientSecrets.Details()
                .setClientId(config.getClientId())
                .setClientSecret(config.getClientSecret()));
        
        flow = new GoogleAuthorizationCodeFlow.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JSON_FACTORY,
                clientSecrets,
                config.getScopes())
            .setDataStoreFactory(new MemoryDataStoreFactory())
            .setAccessType("offline")
            .build();
    }
    
    /**
     * 开始 OAuth 授权
     */
    @GetMapping("/login")
    public void authenticate(HttpServletResponse response) throws IOException {
        String authUrl = flow.newAuthorizationUrl()
            .setRedirectUri(config.getRedirectUri())
            .build();
        response.sendRedirect(authUrl);
    }
    
    /**
     * OAuth 回调处理
     */
    @GetMapping("/callback")
    public ResponseEntity<ApiResponse<String>> handleOAuthCallback(
            @RequestParam("code") String code) {
        try {
            AuthorizationCodeTokenRequest tokenRequest = flow
                .newTokenRequest(code)
                .setRedirectUri(config.getRedirectUri());
            
            TokenResponse tokenResponse = tokenRequest.execute();
            Credential credential = flow.createAndStoreCredential(tokenResponse, "user");
            
            // 保存 Token 到数据库
            saveToken(credential);
            
            return ResponseEntity.ok(ApiResponse.success("YouTube 授权成功"));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("授权失败: " + e.getMessage()));
        }
    }
    
    private void saveToken(Credential credential) {
        OAuthToken token = new OAuthToken();
        token.setPlatform("youtube");
        token.setEncryptedAccessToken(encrypt(credential.getAccessToken()));
        token.setEncryptedRefreshToken(encrypt(credential.getRefreshToken()));
        token.setExpiresAt(LocalDateTime.now().plusSeconds(credential.getExpiresInSeconds()));
        tokenRepository.save(token);
    }
}
```

---

## 📤 视频上传实现

### 1. YouTubeUploadService.java

```java
@Service
public class YouTubeUploadService {
    
    private static final String APPLICATION_NAME = "Grace Platform";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    
    private final YouTubeConfig config;
    private final OAuthTokenRepository tokenRepository;
    
    /**
     * 上传视频到 YouTube
     */
    public YouTubeUploadResult uploadVideo(VideoUploadRequest request) throws Exception {
        // 1. 获取已保存的 Token
        OAuthToken token = tokenRepository.findByPlatform("youtube")
            .orElseThrow(() -> new BusinessException("YouTube 未授权"));
        
        // 2. 创建 Credential
        Credential credential = createCredential(token);
        
        // 3. 创建 YouTube 服务
        YouTube youtube = new YouTube.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JSON_FACTORY,
                credential)
            .setApplicationName(APPLICATION_NAME)
            .build();
        
        // 4. 构建视频元数据
        Video video = new Video();
        VideoSnippet snippet = new VideoSnippet();
        snippet.setTitle(request.getTitle());
        snippet.setDescription(request.getDescription());
        snippet.setTags(request.getTags());
        snippet.setCategoryId(request.getCategoryId()); // 22 = People & Blogs
        video.setSnippet(snippet);
        
        VideoStatus status = new VideoStatus();
        status.setPrivacyStatus(request.getPrivacyStatus()); // public, unlisted, private
        video.setStatus(status);
        
        // 5. 上传视频
        File videoFile = new File(request.getVideoFilePath());
        InputStreamContent mediaContent = new InputStreamContent(
            "video/*",
            new BufferedInputStream(new FileInputStream(videoFile)));
        mediaContent.setLength(videoFile.length());
        
        YouTube.Videos.Insert videoInsert = youtube.videos()
            .insert("snippet,status", video, mediaContent);
        
        // 6. 设置上传监听器（可选）
        videoInsert.getMediaHttpUploader().setProgressListener(
            new MediaHttpUploaderProgressListener() {
                @Override
                public void progressChanged(MediaHttpUploader uploader) throws IOException {
                    switch (uploader.getUploadState()) {
                        case INITIATION_STARTED:
                            System.out.println("Initiation Started");
                            break;
                        case INITIATION_COMPLETE:
                            System.out.println("Initiation Completed");
                            break;
                        case MEDIA_IN_PROGRESS:
                            double progress = uploader.getProgress() * 100;
                            System.out.println("Upload Progress: " + progress + "%");
                            break;
                        case MEDIA_COMPLETE:
                            System.out.println("Upload Completed");
                            break;
                        case NOT_STARTED:
                            System.out.println("Upload Not Started");
                            break;
                    }
                }
            });
        
        // 7. 执行上传
        Video returnedVideo = videoInsert.execute();
        
        // 8. 返回结果
        return new YouTubeUploadResult(
            returnedVideo.getId(),
            "https://www.youtube.com/watch?v=" + returnedVideo.getId(),
            returnedVideo.getSnippet().getTitle(),
            returnedVideo.getStatus().getUploadStatus()
        );
    }
    
    private Credential createCredential(OAuthToken token) {
        return new Credential.Builder(BearerToken.authorizationHeaderAccessMethod())
            .setTransport(GoogleNetHttpTransport.newTrustedTransport())
            .setJsonFactory(JSON_FACTORY)
            .setClientAuthentication(new ClientParametersAuthentication(
                config.getClientId(), config.getClientSecret()))
            .setTokenServerEncodedUrl("https://oauth2.googleapis.com/token")
            .build()
            .setAccessToken(decrypt(token.getEncryptedAccessToken()))
            .setRefreshToken(decrypt(token.getEncryptedRefreshToken()));
    }
}
```

---

## 📋 数据模型

### VideoUploadRequest.java

```java
@Data
public class VideoUploadRequest {
    private String videoFilePath;      // 本地视频文件路径
    private String title;              // 视频标题
    private String description;        // 视频描述
    private List<String> tags;         // 标签
    private String categoryId;         // YouTube 分类 ID
    private String privacyStatus;      // public, unlisted, private
}
```

### YouTubeUploadResult.java

```java
@Data
@AllArgsConstructor
public class YouTubeUploadResult {
    private String videoId;            // YouTube 视频 ID
    private String videoUrl;           // YouTube 视频链接
    private String title;              // 视频标题
    private String uploadStatus;       // 上传状态
}
```

---

## 🔑 关键配置步骤

### 1. Google Cloud Console 设置

1. 访问 https://console.cloud.google.com/
2. 创建新项目或选择现有项目
3. 启用 **YouTube Data API v3**
4. 创建 **OAuth 2.0 客户端 ID**
5. 下载 `client_secret.json` 文件
6. 配置授权重定向 URI：`http://localhost:8080/api/distribution/auth/youtube/callback`

### 2. 环境变量配置

```bash
export YOUTUBE_CLIENT_ID="your-client-id.apps.googleusercontent.com"
export YOUTUBE_CLIENT_SECRET="your-client-secret"
export YOUTUBE_REDIRECT_URI="http://localhost:8080/api/distribution/auth/youtube/callback"
```

---

## ⚠️ 注意事项

1. **配额限制**: YouTube API 有每日配额限制（默认 10,000 units）
2. **视频大小**: 最大支持 128GB 或 12 小时
3. **隐私设置**: 注意 `privacyStatus` 的设置（public/unlisted/private）
4. **Token 刷新**: Access Token 会过期，需要使用 Refresh Token 刷新
5. **错误处理**: 需要处理各种 API 错误（配额超限、网络错误等）

---

## 📊 YouTube 分类 ID 参考

| ID | 分类 |
|----|------|
| 1 | Film & Animation |
| 2 | Autos & Vehicles |
| 10 | Music |
| 15 | Pets & Animals |
| 17 | Sports |
| 19 | Travel & Events |
| 20 | Gaming |
| 22 | People & Blogs |
| 23 | Comedy |
| 24 | Entertainment |
| 25 | News & Politics |
| 26 | Howto & Style |
| 27 | Education |
| 28 | Science & Technology |

---

## 🔗 相关文档

- [YouTube Data API 官方文档](https://developers.google.com/youtube/v3)
- [Google OAuth 2.0 文档](https://developers.google.com/identity/protocols/oauth2)
- [Spring Boot 官方文档](https://spring.io/projects/spring-boot)

---

*文档创建时间: 2026-03-28*
*参考来源: GitHub spring-boot-youtube-api 项目 + Medium 教程*
