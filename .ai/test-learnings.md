# 测试学习笔记

**最后更新:** 2026-03-29 15:07

---

## 测试陷阱

### 2026-03-29 - 关键帧提取方案选择

**问题:** 3 帧方案 vs 10 秒方案

**发现:**
- 3 帧方案（开头/中间/结尾）
  - 成本：¥0.15-0.25
  - 覆盖：29% 步骤
  - 优点：便宜快速
  - 缺点：长视频覆盖不足

- 10 秒方案（每 10 秒 1 帧）
  - 成本：¥0.55-0.75（110 秒视频）
  - 覆盖：100% 步骤
  - 优点：覆盖完整，不遗漏关键内容
  - 缺点：成本高 3 倍

**决策:** 使用 10 秒方案（质量优先）

**教训:** 测试方案选择需要权衡成本和质量，不能只看成本。对于教学视频，完整覆盖更重要。

---

### 2026-03-29 - 多模态 API 调用语言问题

**问题:** LLM 返回纯英文而非中文

**发现:**
- qwen3.5-plus 默认输出英文（YouTube 优化）
- 3 帧方案输出中英文混合
- 10 秒方案（11 帧）输出纯英文

**原因分析:**
- 图片数量多时，LLM 倾向于英文（token 效率高）
- SYSTEM_PROMPT 未明确指定语言

**解决方案:**
```java
SYSTEM_PROMPT = "生成中英文双语元数据，标题包含中英文，描述优先英文..."
```

**教训:** 多模态模型有默认行为，需要明确指定输出格式和语言偏好。

---

### 2026-03-29 - FFmpeg 帧提取参数

**问题:** 原方案只提取前 30 秒

**原命令:**
```bash
ffmpeg -i video.mp4 -vf "fps=1/10" -vframes 3 frame_%03d.jpg
```

**问题:**
- `fps=1/10` + `-vframes 3` = 只提取前 3 帧（0s, 10s, 20s）
- 110 秒视频只覆盖前 30 秒

**修正方案:**
```bash
# 方案 1: 无帧数限制
ffmpeg -i video.mp4 -vf "fps=1/10" -q:v 2 frame_%03d.jpg

# 方案 2: Java 动态计算时间点
double timestamp = (duration / (frameCount - 1)) * i;
// 110 秒，3 帧 → [0s, 55s, 110s]
```

**教训:** FFmpeg 参数组合需要仔细验证，`-vframes` 会限制总帧数而非均匀分布。

---

### 2026-03-29 - Token 成本估算

**问题:** 多模态 API 成本超出预期

**发现:**
- 图片 Token: ~2,000 token/张
- 11 帧视频：22,462 token（图片）+ 722 token（文本）
- 成本：¥0.55-0.75/次

**成本优化方案:**
1. 限制最大帧数（如最多 20 帧）
2. 降低图片质量（-q:v 5 instead of -q:v 2）
3. 混合方案（短视频 10 秒/帧，长视频 20 秒/帧）

**教训:** 多模态 API 成本随图片数量线性增长，需要设置上限控制成本。

---

### 2026-03-29 - 测试报告标准化

**问题:** 测试报告格式不统一

**发现:**
- 不同测试使用不同格式
- 缺少性能指标记录
- 缺少成本分析

**解决方案:**
创建 `test-results/TEMPLATE.md` 标准模板，包含：
- 测试结果总览
- 详细测试步骤
- 性能指标
- 成本分析
- 问题记录
- 建议

**教训:** 标准化测试报告便于对比分析和历史追溯。

---

## 测试最佳实践

### 1. 测试启动流程

```markdown
1. 加载项目 AGENTS.md（了解技术栈、测试策略）
2. 检查 .ai/progress.md（了解最近进度）
3. 检查 .ai/test-progress.md（了解测试历史）
4. 检查 .ai/test-learnings.md（避免重复踩坑）
5. 查看 git log（了解最新变更）
6. 制定测试计划
7. 执行测试
8. 生成报告
9. 更新 test-progress.md
```

### 2. 测试命令选择

| 变更内容 | 测试类型 | 命令 |
|---------|---------|------|
| 领域逻辑 | 属性测试 | `mvn test -Dtest="*PropertyTest"` |
| 边界条件 | 单元测试 | `mvn test -Dtest="*UnitTest"` |
| API/仓储 | 集成测试 | `mvn test -Dtest="*IntegrationTest"` |
| 前端组件 | Lint + 类型检查 | `npm run lint && npx tsc --noEmit` |
| 完整流程 | E2E 测试 | `npx playwright test` |

### 3. 测试报告命名

格式：`YYYY-MM-DD-[测试主题]-REPORT.md`

示例：
- `2026-03-29-MULTIMODAL-TEST-REPORT.md`
- `2026-03-29-10S-SCHEME-TEST-REPORT.md`
- `2026-03-29-FULL-REGRESSION-REPORT.md`

---

## 环境相关

### Testcontainers 依赖 Docker
- 集成测试需要 Docker 运行
- 若 Docker 不可用，跳过集成测试
- 命令：`mvn test -Dtest="*PropertyTest,*UnitTest"`

### Java 21 Preview Features
- `--enable-preview` 需要配置在 compiler 和 surefire 插件
- jqwik 属性测试可能需要额外配置

### 多模态 API
- 模型：qwen3.5-plus
- Base URL: https://dashscope.aliyuncs.com/compatible-mode/v1
- 超时设置：120 秒（图片多时响应慢）
