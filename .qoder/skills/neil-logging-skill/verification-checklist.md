# 8 项联调验收清单

落地完成后按序跑完。每项都要有明确的 **期望结果** 与 **失败处置**。

---

## 1. 基础打通（前后端 traceId 对齐）

**步骤**：
1. 启动后端 + 前端 dev 服务
2. 浏览器 DevTools → Network
3. 任意发起一次 API 请求

**期望**：
- Request Headers 有 `X-EagleEye-TraceId: <30位hex>` 与 `X-EagleEye-RpcId: 0`
- Response Headers 同名 header 值**完全相同**
- Console 打印 `[EagleEye] <traceId> ... <rt>ms`

**失败处置**：检查 `EagleEyeFilter` 是否被 Spring 扫描到、`@Order(HIGHEST_PRECEDENCE)` 是否生效。

---

## 2. 文件落地（5 类日志齐全）

**步骤**：
```bash
ls -lh ~/logs/{appName}/
```

**期望**：5 个文件齐全：
- `application.log`
- `error.log`
- `access.log`
- `{external}-digest.log`（如 `openai-digest.log`）
- `task-digest.log`

每个文件随便 `tail -n 5` 都有带 `[traceId,rpcId]` 或 pipe 分隔的行。

**失败处置**：检查 `logback-spring.xml` 是否被 Spring Boot 加载（`spring-` 前缀名才会被识别）；检查 `LOG_HOME` 解析是否正确。

---

## 3. grep 反查（跨文件命中）

**步骤**：
```bash
TID=<步骤 1 拿到的 traceId>
grep "$TID" ~/logs/{appName}/*.log
```

**期望**：至少在 `application.log`、`access.log`、`{external}-digest.log` 中都能命中。

**失败处置**：检查 digest logger 是否配置了对应的 `<logger>` 节点；检查业务代码是否真的调用了 `DigestLogger.xxx(...)`。

---

## 4. 异步跨线程（MDC 不丢）

**步骤**：发起一个会走 `CompletableFuture.supplyAsync(..., traceExecutor)` 的接口。

**期望**：
- 主线程与子线程日志的 `traceId` **相同**
- 子线程日志的 `rpcId` 是 `0.1`、`0.2` 等（不是 `0`）
- 多个并发子任务 rpcId 互不相同

**失败处置**：
- 若 traceId 变空 → `TtlExecutors.getTtlExecutor(...)` 没用上
- 若 rpcId 相同 → `pushSpan/popSpan` 未配对调用

---

## 5. rpcId 层级（父子可辨）

**步骤**：构造嵌套调用（外层接口 → 调 Service A → A 里再调外部服务）。

**期望**：日志中 rpcId 形如 `0` → `0.1` → `0.1.1`，层级肉眼可辨。

**失败处置**：确认 `TraceContext.pushSpan()` 在进入嵌套前、`popSpan()` 在退出后（try/finally）。

---

## 6. 滚动策略（文件切分）

**步骤**：
1. 临时把 `application.yml` 的 `max-file-size` 改为 `1MB`
2. 用 `ab` / `wrk` 狂压一批请求
3. 重启服务

**期望**：
- 看到 `application.0.log`、`application.1.log` 等按序号切分
- 跨天后出现 `~/logs/{appName}/YYYY-MM-DD/` 归档子目录
- 归档超过 `max-history-days` 的旧目录被自动删除

**失败处置**：检查 `SizeAndTimeBasedRollingPolicy` 的 `fileNamePattern` 是否包含 `%i`（否则不按大小切）。

---

## 7. 故障隔离（日志挂了业务不挂）

**步骤**：
```bash
chmod 000 ~/logs/{appName}/
```
然后正常发起业务请求。

**期望**：
- 接口仍返回 **200** 正常响应
- 响应体内容正确（不是 500 堆栈）
- 控制台可能看到 `[DigestLogger.xxx] ...` 降级输出，但主链路无影响

**恢复**：`chmod 755 ~/logs/{appName}/`

**失败处置**：检查 `DigestLogger`、`EagleEyeFilter`、`TraceContext` 的 try/catch 是否全量覆盖。

---

## 8. 异步写入（不阻塞业务）

**步骤**：写一段测试代码循环打 1000 条 INFO 日志，记录耗时：

```java
long t = System.currentTimeMillis();
for (int i = 0; i < 1000; i++) log.info("benchmark line {}", i);
System.out.println("1000 logs cost: " + (System.currentTimeMillis() - t) + "ms");
```

**期望**：耗时 **< 50ms**（证明走的是 AsyncAppender 队列而非同步 IO）。

若 > 500ms 基本可以判定同步写，需检查：
- 是否每个 `RollingFileAppender` 外面都包了 `AsyncAppender`
- `<root>` / `<logger>` 里引用的是否是 Async 版本（而非 File 版本）

---

## 验收通过标准

**8 项全部通过** 方可视为日志体系落地完成。
任一项失败都代表对应的设计目标（trace 完整性 / 故障隔离 / 性能隔离）未达标，必须修复后重测。
