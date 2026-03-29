# FFmpeg 提取关键帧 - 修正方案

**更新时间:** 2026-03-29 14:03  
**问题:** 原方案只提取前 30 秒，未覆盖完整视频

---

## ❌ 原方案问题

**原命令:**
```bash
ffmpeg -i video.mp4 -vf "fps=1/10" -vframes 3 frame_%03d.jpg
```

**问题:**
- `fps=1/10` 表示从视频开始每 10 秒提取 1 帧
- `-vframes 3` 限制最多 3 帧
- **结果:** 只提取 0s, 10s, 20s 的帧（前 30 秒）

**实际测试:**
- 视频：猪排三明治.mp4 (110 秒)
- 提取：3 帧 (0s, 10s, 20s)
- **问题:** 80-110 秒的内容未覆盖 ❌

---

## ✅ 修正方案：真正的均匀分布

### 方案 A: 使用 select 滤镜（推荐）

**命令:**
```bash
# 计算间隔：视频时长 / (帧数 - 1)
# 110 秒视频，提取 3 帧 → 间隔 = 110 / 2 = 55 秒

ffmpeg -i video.mp4 -vf "select='eq(n,0)+eq(n,55)+eq(n,110)'" -vsync vfr frame_%03d.jpg
```

**说明:**
- `select='eq(n,0)+eq(n,55)+eq(n,110)'` - 提取第 0、55、110 帧
- 需要根据帧率计算帧号

---

### 方案 B: 使用 timestamp 提取（最佳）

**命令:**
```bash
# 提取 3 帧：开头、中间、结尾
# 110 秒视频 → 0s, 55s, 110s

ffmpeg -i video.mp4 -ss 0 -vframes 1 frame_001.jpg
ffmpeg -i video.mp4 -ss 55 -vframes 1 frame_002.jpg
ffmpeg -i video.mp4 -ss 110 -vframes 1 frame_003.jpg
```

**优点:**
- ✅ 精确控制时间点
- ✅ 覆盖完整视频（开头、中间、结尾）
- ✅ 简单易懂

---

### 方案 C: Java 动态计算（生产环境推荐）

```java
public List<Path> extractKeyFrames(Path videoPath, Path outputDir, int frameCount) {
    List<Path> framePaths = new ArrayList<>();
    
    // 1. 获取视频时长（秒）
    double duration = getVideoDuration(videoPath);
    // 例如：110.56 秒
    
    // 2. 计算均匀分布的时间点
    List<Double> timestamps = new ArrayList<>();
    for (int i = 0; i < frameCount; i++) {
        double timestamp;
        if (i == 0) {
            timestamp = 0;  // 第 1 帧：开头
        } else if (i == frameCount - 1) {
            timestamp = duration;  // 最后 1 帧：结尾
        } else {
            timestamp = (duration / (frameCount - 1)) * i;  // 中间均匀分布
        }
        timestamps.add(timestamp);
    }
    // 110 秒视频，3 帧 → [0s, 55s, 110s]
    
    // 3. 提取每个时间点的帧
    for (int i = 0; i < timestamps.size(); i++) {
        double timestamp = timestamps.get(i);
        Path framePath = outputDir.resolve(String.format("frame_%03d.jpg", i + 1));
        
        List<String> command = List.of(
            "ffmpeg",
            "-i", videoPath.toString(),
            "-ss", String.format("%.2f", timestamp),
            "-vframes", "1",
            "-q:v", "2",
            "-y",
            framePath.toString()
        );
        
        // 执行命令...
        framePaths.add(framePath);
    }
    
    return framePaths;
}
```

**输出:**
- 10 秒视频 → [0s, 5s, 10s]
- 30 秒视频 → [0s, 15s, 30s]
- 110 秒视频 → [0s, 55s, 110s] ✅
- 300 秒视频 → [0s, 150s, 300s]

---

## 📊 方案对比

| 视频时长 | 原方案 (fps=1/10) | 修正方案 (均匀分布) |
|---------|-----------------|-------------------|
| 10 秒 | 0s, 10s, 20s ❌ | 0s, 5s, 10s ✅ |
| 30 秒 | 0s, 10s, 20s ⚠️ | 0s, 15s, 30s ✅ |
| 110 秒 | 0s, 10s, 20s ❌ | 0s, 55s, 110s ✅ |
| 300 秒 | 0s, 10s, 20s ❌ | 0s, 150s, 300s ✅ |

---

## 🎯 推荐实现

### 生产环境代码

```java
@Component
public class VideoFrameExtractor {
    
    private static final int DEFAULT_FRAME_COUNT = 3;
    
    /**
     * 提取视频关键帧（均匀分布：开头、中间、结尾）
     */
    public List<Path> extractKeyFrames(Path videoPath, Path outputDir) {
        return extractKeyFrames(videoPath, outputDir, DEFAULT_FRAME_COUNT);
    }
    
    /**
     * 提取视频关键帧（均匀分布）
     */
    public List<Path> extractKeyFrames(Path videoPath, Path outputDir, int frameCount) {
        log.info("Extracting {} key frames from video: {}", frameCount, videoPath);
        
        List<Path> framePaths = new ArrayList<>();
        outputDir.toFile().mkdirs();
        
        try {
            // 1. 获取视频时长
            double duration = getVideoDuration(videoPath);
            log.debug("Video duration: {} seconds", duration);
            
            // 2. 计算均匀分布的时间点
            List<Double> timestamps = calculateUniformTimestamps(duration, frameCount);
            log.debug("Extraction timestamps: {}", timestamps);
            
            // 3. 提取每个时间点的帧
            for (int i = 0; i < timestamps.size(); i++) {
                double timestamp = timestamps.get(i);
                Path framePath = outputDir.resolve(String.format("frame_%03d.jpg", i + 1));
                
                extractFrameAtTime(videoPath, framePath, timestamp);
                framePaths.add(framePath);
                
                log.debug("Extracted frame {} at {}s -> {}", i + 1, timestamp, framePath);
            }
            
            log.info("Successfully extracted {} frames", framePaths.size());
            
        } catch (Exception e) {
            log.error("Failed to extract key frames: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to extract key frames", e);
        }
        
        return framePaths;
    }
    
    /**
     * 计算均匀分布的时间点（开头、中间、结尾）
     */
    private List<Double> calculateUniformTimestamps(double duration, int frameCount) {
        List<Double> timestamps = new ArrayList<>();
        
        for (int i = 0; i < frameCount; i++) {
            double timestamp;
            if (i == 0) {
                // 第 1 帧：开头
                timestamp = 0;
            } else if (i == frameCount - 1) {
                // 最后 1 帧：结尾
                timestamp = duration;
            } else {
                // 中间帧：均匀分布
                timestamp = (duration / (frameCount - 1)) * i;
            }
            timestamps.add(timestamp);
        }
        
        return timestamps;
    }
    
    /**
     * 在指定时间点提取单帧
     */
    private void extractFrameAtTime(Path videoPath, Path outputPath, double timestamp)
            throws IOException, InterruptedException {
        
        List<String> command = List.of(
            "ffmpeg",
            "-i", videoPath.toString(),
            "-ss", String.format("%.2f", timestamp),
            "-vframes", "1",
            "-q:v", "2",
            "-y",
            outputPath.toString()
        );
        
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("FFmpeg exited with code: " + exitCode);
        }
    }
    
    /**
     * 获取视频时长（秒）
     */
    private double getVideoDuration(Path videoPath) throws IOException, InterruptedException {
        List<String> command = List.of(
            "ffprobe",
            "-v", "quiet",
            "-show_entries", "format=duration",
            "-of", "default=noprint_wrappers=1:nokey=1",
            videoPath.toString()
        );
        
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        
        String output = new String(process.getInputStream().readAllBytes());
        process.waitFor();
        
        return Double.parseDouble(output.trim());
    }
}
```

---

## 🧪 测试验证

### 测试视频

| 视频 | 时长 | 提取时间点 | 状态 |
|------|------|-----------|------|
| 猪排三明治.mp4 | 110 秒 | 0s, 55s, 110s | ⏳ 待测试 |
| 大排面.mp4 | ? 秒 | 0s, 50%, 100% | ⏳ 待测试 |
| 推推乐蛋糕.mp4 | ? 秒 | 0s, 50%, 100% | ⏳ 待测试 |
| 鲜花饼.mp4 | ? 秒 | 0s, 50%, 100% | ⏳ 待测试 |

---

## 📝 更新 Issue #30

需要更新 Issue #30 的实现方案：

**原方案:**
```bash
ffmpeg -i video.mp4 -vf "fps=1/10" -vframes 3 frame_%03d.jpg
```

**修正方案:**
```java
// Java 动态计算时间点
List<Double> timestamps = calculateUniformTimestamps(duration, 3);
// [0s, 55s, 110s] for 110s video
```

---

**更新时间:** 2026-03-29 14:03  
**问题发现:** 用户反馈
