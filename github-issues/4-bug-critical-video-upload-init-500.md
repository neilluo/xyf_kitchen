## 🔴 POST /api/videos/upload/init 返回 500 错误 - 视频上传功能完全不可用

**严重程度:** CRITICAL  
**类别:** bug  
**发现时间:** 2026-03-28  
**发现者:** Kelly (QA Lead)  
**影响范围:** 视频上传功能完全不可用  

### 问题描述

视频上传初始化接口返回 500 Internal Server Error，导致整个上传流程无法启动。这是视频上传工作流的第一个接口，此问题阻塞了所有后续功能。

### 错误信息

```
HTTP 500 Internal Server Error
```

### 相关 API

`POST /api/videos/upload/init`

### 复现步骤

1. 启动后端服务（http://localhost:8080）
2. 启动前端服务（http://localhost:3001）
3. 访问 http://localhost:3001/upload
4. 点击"选择文件"按钮
5. 选择任意视频文件（MP4/MOV/AVI/MKV 格式）
6. 打开浏览器开发者工具，观察网络请求

### 预期行为

API 应该返回 200 OK 并创建上传会话：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "uploadId": "upl_x7k9m2",
    "totalChunks": 64,
    "chunkSize": 16777216,
    "expiresAt": "2024-01-16T10:30:00Z"
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### 实际行为

API 返回 500 Internal Server Error，无响应体或错误信息不明确。

### 可能原因分析

1. **Controller 未实现或实现有误**
   - VideoUploadController.initUpload() 方法可能未正确实现
   - 请求参数校验逻辑可能有 bug

2. **数据库问题**
   - upload_session 表可能不存在
   - 数据库连接配置错误
   - MyBatis Mapper 未正确配置

3. **文件系统问题**
   - 视频存储目录未配置或无权限
   - 临时目录创建失败

4. **依赖注入问题**
   - VideoApplicationService 未正确注入
   - VideoFileInspector 实现缺失

### 调试建议

1. **检查后端日志**
   ```bash
   cd grace-platform
   mvn spring-boot:run
   # 查看完整错误堆栈
   ```

2. **验证数据库表**
   ```sql
   SHOW TABLES LIKE 'upload_session';
   DESCRIBE upload_session;
   ```

3. **直接调用 API 测试**
   ```bash
   curl -X POST http://localhost:8080/api/videos/upload/init \
     -H "Content-Type: application/json" \
     -d '{"fileName":"test.mp4","fileSize":1024,"format":"MP4"}' \
     -v
   ```

4. **检查代码实现**
   - 查看 VideoUploadController.java
   - 查看 VideoApplicationService.java
   - 查看 UploadSessionRepository.java

### 建议修复步骤

1. 查看后端日志，定位具体错误原因
2. 验证数据库表结构是否与 docs/backend/db-design.md 一致
3. 检查 VideoUploadController 的实现是否遵循 api.md §B1
4. 确保所有依赖正确注入
5. 添加适当的错误处理和日志记录
6. 实现统一响应格式（ApiResponse）

### 验收标准

- [ ] API 返回 200 OK 和正确的响应结构
- [ ] 创建 upload_session 记录
- [ ] 返回有效的 uploadId、totalChunks、chunkSize、expiresAt
- [ ] 支持 MP4/MOV/AVI/MKV 格式校验
- [ ] 支持文件大小校验（≤5GB）
- [ ] 返回统一的错误响应格式（如校验失败）

### 关联文档

- [api.md §B1](../api.md) - POST /api/videos/upload/init
- [requirements.md 需求 1](../requirements.md) - 需求 1.1, 1.3, 1.4, 1.5
- [docs/backend/03-context-video.md](../docs/backend/03-context-video.md) - 视频上下文实现
- [docs/backend/db-design.md](../docs/backend/db-design.md) - 数据库设计

### 截图

![上传错误](../test-results/upload-error.png)

---

*由 Kelly (QA Lead) 在回归测试中发现*
