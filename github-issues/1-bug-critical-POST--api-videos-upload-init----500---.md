## 🔴 POST /api/videos/upload/init 返回 500 错误

**严重程度:** CRITICAL
**类别:** bug
**发现时间:** 2026-03-28T02:40:21.318Z

### 问题描述
视频上传初始化接口返回 500 Internal Server Error，导致无法开始上传流程。

### 错误信息
```
Internal server error
```


### 相关 API
`POST /api/videos/upload/init`


### 复现步骤
1. 访问 http://localhost:3001/upload
2. 点击"选择文件"按钮
3. 选择任意视频文件
4. 观察网络请求

### 预期行为
API 应该返回 200 并创建上传会话

### 实际行为
API 返回 500 错误

### 截图
![test-results/upload-error.png](test-results/upload-error.png)


---
*由 Playwright 自动化测试生成*
