---
name: Bug #9 - 视频完成上传返回 null
about: MAJOR bug - complete upload returns null videoId
title: '[MAJOR] 视频完成上传返回 null - 无法获取 videoId'
labels: 'bug, major, blocking'
assignees: 'neilluo'
---

## 🟠 问题描述

调用 `POST /api/videos/upload/{uploadId}/complete` 接口后，返回的 videoId 为 null，导致无法继续后续的元数据生成流程。

## 严重程度

**MAJOR** - 阻塞核心流程

## 复现步骤

```bash
# 1. 初始化上传
upload_result=$(curl -s -X POST http://localhost:8080/api/videos/upload/init \
  -H "Content-Type: application/json" \
  -d '{"fileName":"test.mp4","fileSize":1024,"format":"MP4"}')
upload_id=$(echo "$upload_result" | jq -r '.data.uploadId')
echo "Upload ID: $upload_id"

# 2. 完成上传
curl -s -X POST "http://localhost:8080/api/videos/upload/${upload_id}/complete" | jq .
```

## 预期行为

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "videoId": "vid_abc123",
    "fileName": "test.mp4",
    "fileSize": 1024,
    "format": "MP4",
    "duration": "PT0S",
    "status": "UPLOADED",
    "createdAt": "2026-03-29T02:08:00Z"
  }
}
```

## 实际行为

```json
{
  "videoId": null
}
```

## 影响范围

- 无法完成视频上传流程
- 阻塞元数据生成功能
- 阻塞视频发布功能
- 阻塞推广任务功能

## 可能原因

1. **分片合并失败** - ChunkMergeService 实现问题
2. **数据库插入失败** - VideoRepository.save() 失败或表不存在
3. **异常被吞掉** - try-catch 未正确 rethrow
4. **Flyway 未启用** - 数据库表可能不存在

## 调试建议

1. **查看后端日志**
   ```bash
   tail -100 ~/logs/grace-platform.log | grep -i "complete\|upload\|error"
   ```

2. **检查数据库表**
   ```bash
   mysql -u grace -p -e "USE grace_platform; SHOW TABLES LIKE 'video';"
   ```

3. **检查 Flyway 配置**
   ```bash
   grep -r "flyway.enabled" grace-platform/src/main/resources/
   ```

## 相关文档

- [api.md §B3](api.md) - POST /api/videos/upload/{uploadId}/complete
- [requirements.md 需求 1](requirements.md) - 需求 1.7
- [design.md](design.md) - Domain Event 机制

## 修复状态

**已修复** ✅

**修复时间**: 2026-03-29 02:25
**修复文件**: `grace-platform/src/main/java/com/grace/platform/video/infrastructure/persistence/VideoRepositoryImpl.java`

**修复内容**:
```java
// 修改前（Bug）:
if (video.getId() == null) {
    // 新增 - 但 Video.create() 已经预生成 ID，所以不会进入这里
    videoMapper.insert(video);
} else {
    // 更新 - 错误地进入了这里
    videoMapper.update(video);
}

// 修改后（Fix）:
if (video.getId() == null || findById(video.getId()).isEmpty()) {
    // 新增 - 正确检测新视频（ID 为空或数据库中不存在）
    videoMapper.insert(video);
} else {
    // 更新
    videoMapper.update(video);
}
```

**根因**: Video.create() 使用 VideoId.generate() 预生成 ID，导致原逻辑误判为更新操作。

## 验收标准

- [x] complete 接口返回有效的 videoId
- [x] 视频记录成功保存到数据库
- [x] VideoUploadedEvent 成功发布
- [x] 后续可以调用 metadata/generate 接口
- [x] 添加完整的上传流程集成测试
- [x] 编译通过 (mvn compile)
- [x] 单元测试通过 (VideoUnitTest)
- [x] 属性测试通过 (VideoPropertyTest)

## 依赖关系

此 Bug 阻塞：
- 元数据生成
- 视频发布
- 推广任务

---

**发现者:** Kelly (QA Lead)  
**发现时间:** 2026-03-29T02:08  
**测试类型:** API 集成测试
