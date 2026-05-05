---
name: neil-logging-skill
description: 为 Spring Boot + 前端（axios）工程落地阿里 EagleEye（鹰眼）风格的全链路日志体系。用于：初始化全新工程的日志栈、升级现有工程到阿里风格、审计日志链路完整性、修复 traceId 跨线程丢失等问题。当用户提到"日志体系""trace 追踪""阿里风格日志""EagleEye""MDC 跨线程""鹰眼""logback 规范"或希望统一工程 log 风格时触发。
---

# Alibaba-Style Logging Harness

一个可复用的日志体系模板。解决四件事：

1. **统一格式** — 对齐阿里 EagleEye 行格式与文件拆分
2. **全链路 trace** — 前端 axios → 后端 Filter → 异步线程池 → 外部调用，traceId 不丢
3. **可审计** — 5 类日志文件按职责隔离，任一 traceId 可跨文件 grep 反查
4. **主链路保护** — 日志写入全异步化 + 应用层兜底，日志异常绝不拖垮业务

## When to Use

触发场景：

- 用户说"为项目加上日志体系""对齐阿里/鹰眼风格""要有 trace 追踪链"
- 现有工程的日志缺 traceId / 跨线程丢失 / 没有文件拆分 / 同步 IO 阻塞
- 审计：检查项目 log 是否符合阿里内部实践
- 升级：从 Spring Boot 默认 logback 升级到生产级配置

**不适用**：

- 已经有完整 ELK/SLS 投递链路的项目（本 Skill 聚焦文件日志 + 进程内 MDC）
- 纯 Node/Python/Go 栈（本 Skill 以 Java + Spring Boot + Logback 为主）

## Mode Detection

扫描项目根判断模式：

- **无 `logback-spring.xml`** + 只有 `log.info(...)` 零散调用 → **Init mode**
- **已有 `logback-spring.xml`** 但无 MDC/trace → **Upgrade mode**
- **有 trace 但跨异步丢失** → **Fix mode**（只补 TtlExecutor + Filter）
- 用户明确指定模式 → 按用户指示

---

## Core Architecture（一眼看懂）

```
浏览器 axios ──X-EagleEye-TraceId──▶ EagleEyeFilter ──▶ MDC
                                                        │
                                      ┌─────────────────┴─────────────────┐
                                      ▼                                   ▼
                          同步业务（@Slf4j 自动带）          异步（TtlExecutor 透传 MDC）
                                      │                                   │
                                      └──────────┬────────────────────────┘
                                                 ▼
                                       Logback AsyncAppender × 5
                                                 │
                                                 ▼
                              ~/logs/{appName}/  5 个独立日志文件
```

### 四大支柱

| 支柱 | 关键实现 | 产物 |
|------|----------|------|
| **TraceId 30位格式** | `EagleEyeIdGenerator`：IP(8) + ms时间戳(13) + 自增(4) + pid(2) + rand(3) | 1 个工具类 |
| **RpcId 层级** | `TraceContext.pushSpan()/popSpan()`，基于 ThreadLocal 计数 | 1 个工具类 |
| **跨线程传递** | 引入 `com.alibaba:transmittable-thread-local` + `TtlExecutors` 包装线程池 | 1 个 Bean |
| **异步+隔离** | 每个 FileAppender 外包 `AsyncAppender`；DigestLogger/Filter 全量 try/catch | logback-spring.xml |

### 5 个日志文件

| 文件 | 作用 | 丢弃策略 |
|------|------|----------|
| `application.log` | 主日志，INFO+（除 digest） | 允许丢 INFO |
| `error.log` | WARN/ERROR + 堆栈 | **零丢弃** |
| `access.log` | 每次 HTTP 出入口摘要 | 允许丢 |
| `{external}-digest.log` | 每次外部服务调用（如 LLM/OSS/支付等）摘要，可多个 | **零丢弃** |
| `task-digest.log` | 异步任务状态变更摘要（若有异步任务） | **零丢弃** |

### 统一行格式

```
%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%X{traceId},%X{rpcId}] [%thread] %logger{36} - %msg%n
```

digest 行格式为 pipe 分隔：`时间|traceId|rpcId|业务字段1|业务字段2|...`

---

## Process（6 步流程）

### Step 1: 项目扫描

收集：

- `appName` — 从 `application.yml:spring.application.name` 或 `pom.xml:artifactId`
- 外部调用点 — 客户端类（如 `*Client.java`）、`RestTemplate`/`WebClient` 使用处
- 异步场景 — `CompletableFuture.supplyAsync`、`@Async`、自定义 `ThreadPoolTaskExecutor`
- 前端 axios 实例位置 — 通常在 `services/` 或 `api/` 目录

询问用户：

1. `appName` 确认（影响日志目录名）
2. 外部服务有哪些，digest 文件怎么命名（如 `openai-digest.log` / `oss-digest.log` / `payment-digest.log`，可多个）
3. 是否有异步任务日志需求（决定是否创建 `task-digest.log`）

### Step 2: 新增依赖

`pom.xml` 新增：

```xml
<dependency>
  <groupId>com.alibaba</groupId>
  <artifactId>transmittable-thread-local</artifactId>
  <version>2.14.5</version>
</dependency>
```

`application.yml` 新增：

```yaml
{appName}:
  log:
    path: ${user.home}/logs/{appName}
    max-file-size: 100MB
    max-history-days: 7
    total-size-cap: 2GB
```

### Step 3: 生成 logback-spring.xml

使用模板 [logback-template.xml](logback-template.xml)，替换 `{APP_NAME}` 与 `{EXTERNAL_NAME}`。

### Step 4: 生成 Java 组件

按 [java-templates.md](java-templates.md) 创建 6 个类：

| 类 | 包路径 | 作用 |
|----|--------|------|
| `EagleEyeIdGenerator` | `{base}.log` | 生成 30 位 traceId |
| `TraceContext` | `{base}.log` | MDC + rpcId 层级管理 |
| `EagleEyeFilter` | `{base}.log` | HTTP 入口 trace 建立 |
| `AccessLogFilter` | `{base}.log` | access.log 写入 |
| `DigestLogger` | `{base}.log` | digest 工具类（带兜底） |
| `TtlExecutorConfig` | `{base}.config` | TTL 包装线程池 Bean |

### Step 5: 修改业务代码

- **异步点**：`CompletableFuture.supplyAsync(..., traceExecutor)` 注入 `traceExecutor` Bean
- **外部调用点**：调用前后 `TraceContext.pushSpan()/popSpan()`，调用结束 `DigestLogger.external(...)` 打一行摘要
- **前端**：按 [frontend-template.md](frontend-template.md) 改 axios interceptor

### Step 6: 验收

按 [verification-checklist.md](verification-checklist.md) 跑完 8 项自检。

---

## 产物清单（模板化）

Init 模式下本 Skill 会创建/修改：

| 类别 | 文件 | 动作 |
|------|------|------|
| 配置 | `src/main/resources/logback-spring.xml` | create |
| 配置 | `src/main/resources/application.yml` | 追加 `{appName}.log.*` 块 |
| 依赖 | `pom.xml` | 追加 TTL 依赖 |
| 代码 | `{base-pkg}/log/EagleEyeIdGenerator.java` | create |
| 代码 | `{base-pkg}/log/TraceContext.java` | create |
| 代码 | `{base-pkg}/log/EagleEyeFilter.java` | create |
| 代码 | `{base-pkg}/log/AccessLogFilter.java` | create |
| 代码 | `{base-pkg}/log/DigestLogger.java` | create |
| 代码 | `{base-pkg}/config/TtlExecutorConfig.java` | create |
| 测试 | `src/test/.../log/EagleEyeIdGeneratorTest.java` | create |
| 测试 | `src/test/.../log/TraceContextTest.java` | create |
| 测试 | `src/test/.../log/DigestLoggerFormatTest.java` | create |
| 前端 | `frontend/src/services/*.js` 的 axios 实例 | interceptor 注入 |

---

## Key Design Principles

### 1. TraceId 只生成一次，全链路只读

入口 Filter 生成或继承，MDC 持有，后续任何地方都不许再 `new`。

### 2. RpcId 每跳递增，形成层级

`0` → `0.1` → `0.1.1`，肉眼可辨父子关系；由 `TraceContext.pushSpan()/popSpan()` 成对调用管理。

### 3. 业务代码零侵入

保持 `@Slf4j` + `log.info(...)` 写法，trace 由 Filter/TtlExecutor 透明注入 MDC。唯一需要侵入的是：

- 异步场景：`supplyAsync` 第 2 参数传 `traceExecutor`
- 外部调用：调用前后包一层 `pushSpan/popSpan` + digest 记录

### 4. 日志异常 ≠ 业务异常

- `AsyncAppender` 全包装，IO 异步化
- `DigestLogger`/`Filter`/`TraceContext` 所有对外方法 `try/catch`
- 日志目录不可写时，业务仍返回 200

### 5. 文件职责严格分离

digest logger 配 `additivity="false"`，不落主日志；error.log 通过 `ThresholdFilter` 只接 WARN+。

### 6. 丢弃分级

INFO/access 允许在队列满时丢，error/digest 永不丢。防护主链路的权衡。

---

## Further Reading

| 文档 | 内容 |
|------|------|
| [logback-template.xml](logback-template.xml) | 即用的 logback 配置完整模板 |
| [java-templates.md](java-templates.md) | 6 个 Java 类的完整代码模板（含占位符） |
| [frontend-template.md](frontend-template.md) | axios interceptor + console 打印模板 |
| [verification-checklist.md](verification-checklist.md) | 8 项联调验收清单 |

## Anti-Patterns（不要做）

- ❌ **不要**在业务方法签名里传 `traceId` 参数 — 应由 MDC 自动承载
- ❌ **不要**用 `ThreadLocal<String>` 自造 trace — 跨线程池必丢；必须用 TTL
- ❌ **不要**把 digest 日志和主日志混在一个 appender — 会污染主日志 grep 视图
- ❌ **不要**用同步 `FileAppender` — 生产磁盘抖动会阻塞业务
- ❌ **不要**在 `Filter` 里抛 `RuntimeException` 不捕获 — 日志初始化失败导致接口 500
- ❌ **不要**让 traceId 小于 30 位或自己发明格式 — 会破坏跨系统对齐
