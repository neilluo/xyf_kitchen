# FFmpeg 关键帧提取 - 每 10 秒 1 帧方案

**更新时间:** 2026-03-29 14:40  
**需求:** 每 10 秒提取 1 帧，覆盖完整视频内容

---

## 📋 提取策略

### 方案：每 10 秒 1 帧

**FFmpeg 命令:**
```bash
ffmpeg -i video.mp4 -vf "fps=1/10" -q:v 2 frame_%03d.jpg
```

**参数说明:**
- `fps=1/10` - 每 10 秒提取 1 帧
- `-q:v 2` - 高质量（2-31，越小质量越高）
- `frame_%03d.jpg` - 输出 frame_001.jpg, frame_002.jpg...

**无帧数限制:** 根据视频时长自动提取

---

## 📊 提取结果示例

### 110 秒视频

| 帧号 | 时间点 | 内容 |
|------|--------|------|
| frame_001.jpg | 0s | 开头 |
| frame_002.jpg | 10s | 前段 |
| frame_003.jpg | 20s | 前段 |
| frame_004.jpg | 30s | 中前段 |
| frame_005.jpg | 40s | 中前段 |
| frame_006.jpg | 50s | 中间 |
| frame_007.jpg | 60s | 中后段 |
| frame_008.jpg | 70s | 中后段 |
| frame_009.jpg | 80s | 后段 |
| frame_010.jpg | 90s | 后段 |
| frame_011.jpg | 100s | 后段 |
| frame_012.jpg | 110s | 结尾 |

**总计:** 12 帧

---

### 不同时长的视频

| 视频时长 | 提取帧数 | 时间点 |
|---------|---------|--------|
| 30 秒 | 4 帧 | 0s, 10s, 20s, 30s |
| 60 秒 | 7 帧 | 0s, 10s, ..., 60s |
| 110 秒 | 12 帧 | 0s, 10s, ..., 110s |
| 180 秒 | 19 帧 | 0s, 10s, ..., 180s |
| 300 秒 | 31 帧 | 0s, 10s, ..., 300s |

**公式:**
```
帧数 = (视频时长 / 10) + 1
```

---

## 🎯 Java 实现

### VideoFrameExtractor (每 10 秒 1 帧)

```java
@Component
public class VideoFrameExtractor {
    
    private static final int INTERVAL_SECONDS = 10;  // 每 10 秒 1 帧
    
    /**
     * 提取视频关键帧（每 10 秒 1 帧）
     */
    public List<Path> extractKeyFrames(Path videoPath, Path outputDir) {
        List<Path> framePaths = new ArrayList<>();
        outputDir.toFile().mkdirs();
        
        // 1. 获取视频时长
        double duration = getVideoDuration(videoPath);
        int frameCount = (int) (duration / INTERVAL_SECONDS) + 1;
        
        log.info("Extracting {} frames from {}s video (every {}s)", 
            frameCount, duration, INTERVAL_SECONDS);
        
        // 2. 使用 FFmpeg 提取
        List<String> command = List.of(
            "ffmpeg",
            "-i", videoPath.toString(),
            "-vf", String.format("fps=1/%d", INTERVAL_SECONDS),
            "-q:v", "2",
            "-y",
            outputDir.resolve("frame_%03d.jpg").toString()
        );
        
        // 3. 执行命令
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg failed with code: " + exitCode);
        }
        
        // 4. 收集输出的文件
        for (int i = 1; i <= frameCount; i++) {
            Path framePath = outputDir.resolve(String.format("frame_%03d.jpg", i));
            if (framePath.toFile().exists()) {
                framePaths.add(framePath);
            }
        }
        
        log.info("Successfully extracted {} frames", framePaths.size());
        return framePaths;
    }
}
```

---

## 📊 成本评估

### Token 使用

| 视频时长 | 提取帧数 | 图片 Token | 总 Token | 成本估算 |
|---------|---------|-----------|---------|---------|
| 30 秒 | 4 帧 | ~8,000 | ~9,000 | ¥0.20-0.30 |
| 60 秒 | 7 帧 | ~14,000 | ~15,000 | ¥0.35-0.50 |
| 110 秒 | 12 帧 | ~24,000 | ~25,000 | ¥0.60-0.80 |
| 180 秒 | 19 帧 | ~38,000 | ~39,000 | ¥0.90-1.20 |
| 300 秒 | 31 帧 | ~62,000 | ~63,000 | ¥1.50-2.00 |

**对比 3 帧方案:**

| 视频时长 | 3 帧方案 | 10 秒方案 | 成本增加 |
|---------|---------|---------|---------|
| 110 秒 | ¥0.15-0.25 | ¥0.60-0.80 | 4 倍 |
| 180 秒 | ¥0.15-0.25 | ¥0.90-1.20 | 6 倍 |
| 300 秒 | ¥0.15-0.25 | ¥1.50-2.00 | 10 倍 |

---

## 🎯 优缺点对比

### 3 帧方案（开头/中间/结尾）

**优点:**
- ✅ 成本低（固定 3 帧）
- ✅ 速度快
- ✅ 适合短视频

**缺点:**
- ❌ 长视频覆盖不足
- ❌ 可能遗漏关键内容

### 10 秒方案（每 10 秒 1 帧）

**优点:**
- ✅ 覆盖完整视频内容
- ✅ 不会遗漏关键步骤
- ✅ 适合长视频

**缺点:**
- ❌ 成本较高（随视频时长增加）
- ❌ Token 使用多

---

## 📝 推荐配置

### 环境变量

```bash
# .env.local
# 关键帧提取间隔（秒）
GRACE_FRAME_INTERVAL=10

# 最大提取帧数（可选，控制成本）
GRACE_MAX_FRAMES=20
```

### 应用配置

```yaml
# application.yml
grace:
  metadata:
    frame-extraction:
      enabled: true
      interval-seconds: 10      # 每 10 秒 1 帧
      max-frames: 20            # 最多 20 帧（控制成本）
      quality: 2                # 高质量
```

---

## 🔧 优化建议

### 1. 混合方案

**短视频 (<60 秒):** 每 10 秒 1 帧  
**长视频 (≥60 秒):** 每 20 秒 1 帧 或 限制最多 10 帧

```java
public int calculateInterval(double duration) {
    if (duration < 60) {
        return 10;  // 短视频：每 10 秒 1 帧
    } else if (duration < 180) {
        return 20;  // 中长视频：每 20 秒 1 帧
    } else {
        return 30;  // 长视频：每 30 秒 1 帧
    }
}
```

### 2. 帧数限制

```java
int frameCount = (int) (duration / INTERVAL_SECONDS) + 1;
int maxFrames = 20;  // 最多 20 帧
frameCount = Math.min(frameCount, maxFrames);
```

### 3. 图片压缩

```bash
# 降低质量节省成本
-q:v 5  #  instead of -q:v 2
```

---

## ✅ 测试验证

### 测试视频

| 视频 | 时长 | 预计帧数 | 实际帧数 | 状态 |
|------|------|---------|---------|------|
| 猪排三明治.mp4 | 110 秒 | 12 帧 | 12 帧 | ✅ |
| 大排面.mp4 | TBD | TBD | TBD | ⏳ |
| 推推乐蛋糕.mp4 | TBD | TBD | TBD | ⏳ |
| 鲜花饼.mp4 | TBD | TBD | TBD | ⏳ |

---

**更新时间:** 2026-03-29 14:40  
**需求来源:** 用户反馈
