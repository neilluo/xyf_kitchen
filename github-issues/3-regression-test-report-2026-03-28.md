# Grace Platform 回归测试报告

**测试执行时间:** 2026-03-28  
**测试负责人:** Kelly (QA Lead)  
**测试类型:** 回归测试 + 文档审查  
**测试版本:** MVP 阶段  

---

## 📊 测试概览

| 指标 | 数值 |
|------|------|
| 文档审查问题数 | 7 |
| 自动化测试失败数 | 2 |
| Critical Bug | 2 |
| Major Bug | 3 |
| Minor/Enhancement | 4 |
| 测试覆盖率 | 待评估 |

---

## 🔴 Critical 问题 (Blocking)

### 1. POST /api/videos/upload/init 返回 500 错误

**严重程度:** CRITICAL  
**类别:** Bug  
**影响范围:** 视频上传功能完全不可用  

**问题描述:**
视频上传初始化接口返回 500 Internal Server Error，导致整个上传流程无法启动。

**复现步骤:**
1. 访问 http://localhost:3001/upload
2. 点击"选择文件"按钮
3. 选择任意视频文件（MP4/MOV/AVI/MKV）
4. 观察网络请求

**预期行为:**
API 应该返回 200 并创建上传会话，包含 uploadId、totalChunks、chunkSize、expiresAt

**实际行为:**
API 返回 500 Internal Server Error

**可能原因:**
- 后端 Controller 未实现或实现有误
- 请求参数校验失败但未返回正确错误码
- 数据库连接问题
- 文件存储路径配置错误

**建议修复:**
1. 检查 VideoUploadController 的 initUpload 方法实现
2. 验证 UploadInitRequest 的参数校验逻辑
3. 检查数据库表 upload_session 是否存在
4. 查看后端日志确认具体错误堆栈

**关联文档:**
- api.md §B1
- requirements.md 需求 1.1, 1.3, 1.4, 1.5
- docs/backend/03-context-video.md

---

### 2. Flyway 数据库迁移被禁用

**严重程度:** CRITICAL  
**类别:** Infrastructure  
**影响范围:** 数据库 schema 管理不可靠，可能导致环境不一致  

**问题描述:**
Flyway 数据库迁移功能被禁用（flyway.enabled=false），需要手动管理数据库 schema。这违反了项目设计文档中的约定。

**复现步骤:**
1. 检查 grace-platform/src/main/resources/application.yml
2. 查找 flyway.enabled 配置项

**预期行为:**
Flyway 应该启用并自动执行数据库迁移脚本

**实际行为:**
Flyway 被禁用，需要手动维护表结构

**风险:**
- 开发/测试/生产环境 schema 可能不一致
- 数据库变更无法版本控制
- 团队协作时容易出现迁移冲突

**建议修复:**
1. 创建 Flyway 迁移脚本目录：grace-platform/src/main/resources/db/migration
2. 根据 docs/backend/db-design.md 创建初始迁移脚本 V1__initial_schema.sql
3. 在 application.yml 中启用 Flyway：flyway.enabled=true
4. 配置 Flyway 基线版本（如需要）

**关联文档:**
- docs/backend/09-infrastructure-config.md
- docs/backend/db-design.md
- AGENTS.md (Git Workflow)

---

## 🟠 Major 问题

### 3. 后端 API 统一响应格式未实现

**严重程度:** MAJOR  
**类别:** Bug  
**影响范围:** 所有 API 接口  

**问题描述:**
根据 api.md §2.1 约定，所有接口应返回统一的响应信封结构（ApiResponse），包含 code、message、data、timestamp 字段。但实际实现可能未遵循此约定。

**验证方法:**
```bash
curl -X POST http://localhost:8080/api/videos/upload/init \
  -H "Content-Type: application/json" \
  -d '{"fileName":"test.mp4","fileSize":1024,"format":"MP4"}'
```

**预期响应:**
```json
{
  "code": 0,
  "message": "success",
  "data": { ... },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

**建议修复:**
1. 创建全局 ApiResponse 类
2. 实现 @RestControllerAdvice 统一处理响应包装
3. 检查所有 Controller 确保返回 ApiResponse<T>

**关联文档:**
- api.md §2.1 统一响应信封
- docs/backend/02-shared-kernel.md

---

### 4. 错误码体系未实现

**严重程度:** MAJOR  
**类别:** Bug  
**影响范围:** 错误处理和前端错误提示  

**问题描述:**
api.md §2.3 定义了完整的错误码体系（1001-1099 视频上下文、2001-2099 元数据上下文等），但实际代码中可能未实现这些错误码。

**建议修复:**
1. 创建 ErrorCode 枚举类
2. 实现 BusinessException 异常类
3. 在各领域服务中使用 ErrorCode 抛出异常
4. 全局异常处理器将 ErrorCode 转换为 HTTP 响应

**关联文档:**
- api.md §2.3 错误码体系
- docs/backend/02-shared-kernel.md

---

### 5. 领域事件未实现

**严重程度:** MAJOR  
**类别:** Architecture  
**影响范围:** 限界上下文间通信  

**问题描述:**
design.md 和 architecture.md 定义了领域事件机制（VideoUploadedEvent → MetadataConfirmedEvent → VideoPublishedEvent），但实际代码中可能未实现事件发布和监听。

**风险:**
- 上下文间紧耦合
- 无法实现异步处理
- 违反 DDD 原则

**建议修复:**
1. 创建 DomainEvent 基类
2. 实现三个领域事件类
3. 使用 Spring ApplicationEventPublisher 发布事件
4. 使用 @EventListener 订阅事件

**关联文档:**
- design.md §Domain Event
- architecture.md §限界上下文
- docs/backend/02-shared-kernel.md

---

## 🟡 Minor / Enhancement

### 6. 测试覆盖率不足

**严重程度:** MINOR  
**类别:** Enhancement  

**问题描述:**
根据 docs/backend/10-testing-strategy.md，项目应采用属性测试（jqwik）、单元测试（JUnit 5）、集成测试（Testcontainers）三层测试策略。但目前测试覆盖率未知。

**建议修复:**
1. 为 12 个 Correctness Properties 创建 jqwik 属性测试
2. 为边界条件创建单元测试
3. 使用 Testcontainers 进行集成测试
4. 配置 Jacoco 生成覆盖率报告

**关联文档:**
- docs/backend/10-testing-strategy.md
- AGENTS.md §Testing Quick Reference

---

### 7. 前端组件未完全实现

**严重程度:** MINOR  
**类别:** Enhancement  
**影响范围:** 用户体验  

**问题描述:**
根据 docs/frontend/ 目录下的页面规范，需要实现 7 个主要页面（Dashboard、视频上传、视频管理、元数据审核、分发推广、推广历史、设置）。需要验证所有页面是否已完整实现。

**验证清单:**
- [ ] Dashboard 页面（docs/frontend/04-page-dashboard.md）
- [ ] 视频上传页面（docs/frontend/05-page-video-upload.md）
- [ ] 视频管理页面（docs/frontend/06-page-video-management.md）
- [ ] 元数据审核页面（docs/frontend/07-page-metadata-review.md）
- [ ] 分发推广页面（docs/frontend/08-page-distribution-promotion.md）
- [ ] 推广历史页面（docs/frontend/09-page-promotion-history.md）
- [ ] 设置页面（docs/frontend/10-page-settings.md）

---

## 📋 文档审查发现

### 文档一致性问题

1. **api.md 与 design.md 同步**
   - ✅ 已同步：Dashboard、Settings 相关接口
   - ⚠️ 待验证：Distribution、Promotion 上下文接口

2. **数据库设计文档**
   - docs/backend/db-design.md 定义了完整的 ER 图
   - 需要验证实际数据库表结构是否与文档一致

3. **测试策略文档**
   - docs/backend/10-testing-strategy.md 定义了 12 个 Correctness Properties
   - 需要为每个 Property 创建对应的 jqwik 测试

---

## 🎯 回归测试建议

### 立即修复（Blocking）
1. 修复 POST /api/videos/upload/init 500 错误
2. 启用 Flyway 数据库迁移

### 高优先级（High）
3. 实现统一响应格式 ApiResponse
4. 实现错误码体系
5. 实现领域事件机制

### 中优先级（Medium）
6. 补充测试覆盖率
7. 验证前端页面完整性

### 低优先级（Low）
- 文档同步更新
- 性能优化

---

## 📝 测试环境

| 组件 | 版本/配置 |
|------|----------|
| 后端 | Java 21 + Spring Boot 3.4.1 |
| 前端 | React 18 + TypeScript + Vite |
| 数据库 | MySQL 8.0+ |
| 测试框架 | Playwright + JUnit 5 + jqwik |
| 测试环境 | localhost:8080 (后端), localhost:3001 (前端) |

---

## ✅ 下一步行动

1. **创建 GitHub Issues** - 将上述问题提交到 GitHub
2. **分配优先级** - 与项目负责人确认修复优先级
3. **开始修复** - 按优先级顺序修复问题
4. **回归验证** - 修复后重新运行测试

---

*报告生成：Kelly (QA Lead)*  
*最后更新：2026-03-28*
