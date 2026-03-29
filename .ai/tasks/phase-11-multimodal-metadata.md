# Phase 11: 多模态元数据生成升级

> Issue: #30 - 升级到 qwen3.5-plus 多模态模型，实现视频内容理解
> 参考: `docs/backend/04-context-metadata.md`
> 产出: VideoFrameExtractor + 多模态 LLM 支持 + MetadataGenerationService 升级

## 背景与目标

### 当前问题
- 使用 qwen-plus 文本模型，只能基于文件名推测内容
- 文件名不准确时元数据偏差大（如 `DSC_0001.mp4`）
- 无法识别食材、烹饪步骤等关键信息

### 升级目标
- 使用 qwen3.5-plus 多模态模型
- 提取视频关键帧作为视觉输入
- 基于实际视频内容生成准确元数据

---

## 任务列表

### P11-01: 更新 LLM 配置支持多模态模型

- **参考文档**: `docs/backend/09-infrastructure-config.md` §H (配置项)
- **产出文件**:
  - `.env.example` (新增 QWEN_MODEL 默认值)
  - `grace-platform/src/main/java/com/grace/platform/metadata/infrastructure/llm/LlmProperties.java` (支持模型切换)
- **验证命令**: `mvn clean compile`
- **依赖**: 无
- **状态**: [ ]
- **注意**: 
  - 默认模型改为 `qwen3.5-plus`
  - 保持向后兼容，支持配置切换回 `qwen-plus`

---

### P11-02: 创建 VideoFrameExtractor 视频帧提取器

- **参考文档**: Issue #30 技术方案
- **产出文件**:
  - `grace-platform/src/main/java/com/grace/platform/video/domain/VideoFrameExtractor.java` (接口)
  - `grace-platform/src/main/java/com/grace/platform/video/infrastructure/file/VideoFrameExtractorImpl.java` (实现)
  - `grace-platform/src/main/java/com/grace/platform/video/domain/ImageFrame.java` (值对象)
- **验证命令**: `mvn clean compile`
- **依赖**: P1-05 (VideoFileInspector 已有 FFmpeg 调用经验)
- **状态**: [ ]
- **注意**:
  - 使用 FFmpeg 提取关键帧: `ffmpeg -i video.mp4 -vf "select='eq(n,0)+eq(n,100)+eq(n,200)'" -vsync vfr frames/%d.jpg`
  - 提取 3 张关键帧（开头、中间、结尾）
  - 转换为 Base64 编码供 API 使用
  - 临时文件清理

---

### P11-03: 更新 LlmRequest 支持多模态输入

- **参考文档**: Issue #30 技术方案
- **产出文件**:
  - `grace-platform/src/main/java/com/grace/platform/metadata/infrastructure/llm/LlmRequest.java` (新增 imageFrames 字段)
- **验证命令**: `mvn clean compile`
- **依赖**: P11-02
- **状态**: [ ]
- **注意**:
  - 添加 `List<ImageFrame> imageFrames` 字段（可为 null）
  - 保持向后兼容，纯文本请求时 imageFrames 为 null

---

### P11-04: 更新 QwenLlmServiceAdapter 支持多模态 API

- **参考文档**: Issue #30 技术方案、阿里云 DashScope 多模态 API 文档
- **产出文件**:
  - `grace-platform/src/main/java/com/grace/platform/metadata/infrastructure/llm/QwenLlmServiceAdapter.java`
- **验证命令**: `mvn clean compile`
- **依赖**: P11-03
- **状态**: [ ]
- **注意**:
  - 检测是否有 imageFrames，决定调用文本 API 还是多模态 API
  - 多模态 API 请求格式:
    ```json
    {
      "model": "qwen3.5-plus",
      "messages": [
        {
          "role": "user",
          "content": [
            {"type": "text", "text": "prompt..."},
            {"type": "image_url", "image_url": {"url": "data:image/jpeg;base64,..."}}
          ]
        }
      ]
    }
    ```

---

### P11-05: 更新 MetadataGenerationServiceImpl 集成视频帧

- **参考文档**: Issue #30 技术方案、`docs/backend/04-context-metadata.md` §G.2
- **产出文件**:
  - `grace-platform/src/main/java/com/grace/platform/metadata/infrastructure/llm/MetadataGenerationServiceImpl.java`
- **验证命令**: `mvn clean compile`
- **依赖**: P11-04
- **状态**: [ ]
- **注意**:
  - 注入 VideoFrameExtractor
  - 修改 generate() 方法流程:
    1. 提取视频关键帧
    2. 构建多模态 prompt
    3. 调用 LLM（传入视频帧）
    4. 解析返回的元数据
  - 更新 SYSTEM_PROMPT 说明可以"看到视频内容"

---

### P11-06: 创建 VideoFrameExtractor 单元测试

- **参考文档**: `docs/backend/10-testing-strategy.md`
- **产出文件**:
  - `grace-platform/src/test/java/com/grace/platform/video/infrastructure/file/VideoFrameExtractorTest.java`
- **验证命令**: `mvn test -Dtest=VideoFrameExtractorTest`
- **依赖**: P11-02
- **状态**: [ ]
- **注意**:
  - 测试 FFmpeg 命令构建
  - 测试关键帧提取逻辑
  - Mock FFmpeg 调用

---

### P11-07: 更新 MetadataGenerationService 单元测试

- **参考文档**: `docs/backend/10-testing-strategy.md`
- **产出文件**:
  - `grace-platform/src/test/java/com/grace/platform/metadata/infrastructure/llm/MetadataGenerationServiceTest.java`
- **验证命令**: `mvn test -Dtest=MetadataGenerationServiceTest`
- **依赖**: P11-05
- **状态**: [ ]
- **注意**:
  - 测试多模态输入场景
  - 测试纯文本回退场景
  - Mock VideoFrameExtractor

---

### P11-08: 更新 MetadataPropertyTest

- **参考文档**: `docs/backend/10-testing-strategy.md`
- **产出文件**:
  - `grace-platform/src/test/java/com/grace/platform/metadata/MetadataPropertyTest.java`
- **验证命令**: `mvn test -Dtest=MetadataPropertyTest`
- **依赖**: P11-05
- **状态**: [ ]
- **注意**:
  - 测试元数据生成准确性（基于视频帧）

---

### P11-09: 全量编译与测试

- **参考文档**: `AGENTS.md` §Testing Quick Reference
- **产出**: 无
- **验证命令**: `mvn clean compile && mvn test -Dtest="*UnitTest,*PropertyTest"`
- **依赖**: P11-01 ~ P11-08
- **状态**: [ ]
- **注意**:
  - 确保所有测试通过
  - 确保无编译警告

---

### P11-10: 更新文档

- **参考文档**: `AGENTS.md` §Documentation Sync
- **产出文件**:
  - `docs/backend/04-context-metadata.md` (更新 LLM 实现章节)
  - `docs/backend/09-infrastructure-config.md` (更新配置项)
- **验证命令**: 文档审查
- **依赖**: P11-09
- **状态**: [ ]
- **注意**:
  - 添加多模态实现说明
  - 更新配置项说明
  - 添加成本评估说明

---

## 技术方案详情

### 架构图

```
┌─────────────────────────────────────────────────────────────┐
│                    MetadataGenerationServiceImpl            │
│  ┌─────────────────┐    ┌──────────────────────────────┐   │
│  │ VideoFrameExtractor│   │ QwenLlmServiceAdapter       │   │
│  │  (FFmpeg)       │───▶│  (多模态 API)               │   │
│  └─────────────────┘    └──────────────────────────────┘   │
│           │                           │                     │
│           ▼                           ▼                     │
│    ImageFrame[]               LlmRequest                    │
│    (Base64)                   (text + images)               │
└─────────────────────────────────────────────────────────────┘
```

### 关键设计决策

1. **模型切换策略**
   - 通过配置 `QWEN_MODEL` 切换
   - 默认 `qwen3.5-plus`，可回退到 `qwen-plus`

2. **关键帧提取策略**
   - 提取 3 帧：开头(0%)、中间(50%)、结尾(100%)
   - 使用 FFmpeg `select` 滤镜
   - 转换为 JPEG Base64

3. **成本优化**
   - 仅对需要元数据生成的视频调用多模态
   - 缓存已生成的元数据
   - 支持配置开关（纯文本模式）

### 风险与缓解

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| 成本增加 50-100 倍 | 高 | 配置开关，按需启用 |
| FFmpeg 依赖 | 中 | 添加 FFmpeg 安装检查 |
| API 响应时间增加 | 中 | 异步处理，超时重试 |
| 多模态 API 不稳定 | 中 | 降级到纯文本模式 |

---

## 验收标准

- [ ] 配置更新为 `qwen3.5-plus`
- [ ] VideoFrameExtractor 实现并测试通过
- [ ] LlmRequest 支持多模态输入
- [ ] QwenLlmServiceAdapter 支持多模态 API
- [ ] MetadataGenerationServiceImpl 集成视频帧
- [ ] 单元测试全部通过
- [ ] 属性测试全部通过
- [ ] 文档同步更新

---

## 成本评估

| 模型 | 单次成本 | 月成本估算(1000次) |
|------|----------|-------------------|
| qwen-plus (文本) | ~¥0.001 | ~¥1 |
| qwen3.5-plus (多模态) | ~¥0.05-0.10 | ~¥50-100 |

**建议**: 通过配置开关控制，小规模测试后逐步推广

---

**创建时间**: 2026-03-29
**负责人**: Holly
**相关 Issue**: #30
