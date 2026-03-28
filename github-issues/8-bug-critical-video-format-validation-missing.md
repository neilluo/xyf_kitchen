---
name: Bug #8 - 视频格式校验缺失
about: CRITICAL bug - unsupported video formats are not rejected
title: '[CRITICAL] 视频格式校验缺失 - WMV 等不支持的格式未被拦截'
labels: 'bug, critical, blocking'
assignees: 'neilluo'
---

## 🔴 问题描述

视频上传初始化接口 `POST /api/videos/upload/init` 未正确校验视频格式，不支持的格式（如 WMV）被接受，违反了 API 契约和需求文档。

## 严重程度

**CRITICAL** - 阻塞发布

## 复现步骤

```bash
curl -X POST http://localhost:8080/api/videos/upload/init \
  -H "Content-Type: application/json" \
  -d '{"fileName":"test.wmv","fileSize":1024,"format":"WMV"}' \
  | jq .
```

## 预期行为

```json
{
  "code": 1001,
  "message": "不支持的视频格式：WMV。支持的格式：MP4, MOV, AVI, MKV",
  "errors": [
    { "field": "format", "message": "格式不在支持列表中" }
  ]
}
```

## 实际行为

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "uploadId": "upl_xxx",
    "totalChunks": 1,
    "chunkSize": 16777216,
    "expiresAt": "2026-03-30T02:08:39Z"
  }
}
```

## 影响范围

- 用户可以上传不支持的视频格式
- 后续处理流程会失败
- 违反需求 1.3 和 1.5 的验收标准

## 相关文档

- [api.md §B1](api.md) - POST /api/videos/upload/init
- [api.md §2.3](api.md) - 错误码 1001 UNSUPPORTED_VIDEO_FORMAT
- [requirements.md 需求 1](requirements.md) - 需求 1.3, 1.5

## 建议修复

在 VideoUploadController 或 VideoApplicationService 中添加格式白名单校验：

```java
// 方案：在 Service 层校验
try {
    VideoFormat format = VideoFormat.valueOf(request.format());
} catch (IllegalArgumentException e) {
    throw new BusinessException(ErrorCode.UNSUPPORTED_VIDEO_FORMAT, 
        request.format(), "MP4, MOV, AVI, MKV");
}
```

## 验收标准

- [ ] WMV/FLV 等不支持格式返回 1001 错误
- [ ] MP4/MOV/AVI/MKV 格式正常接受
- [ ] 错误信息明确列出支持的格式
- [ ] 添加单元测试验证格式校验

## 测试报告

详细测试报告见：`test-results/TEST-SUMMARY.md`

---

**发现者:** Kelly (QA Lead)  
**发现时间:** 2026-03-29T02:08  
**测试类型:** API 边界条件测试
