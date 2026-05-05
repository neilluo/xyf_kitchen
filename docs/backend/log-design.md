# 日志排查索引（Debugger 入口）

> 用途：出问题时，按下表去对应文件定位。具体调试由 debugger agent 负责。

## 日志目录

- **业务日志（logback 产出）**：`logs/grace-platform/`
  路径由 `application.yml` 的 `grace-platform.log.path` 控制，默认 `./logs/grace-platform`，可通过环境变量 `GRACE_LOG_PATH` 覆盖（生产环境部署时用）。
- **启动日志（脚本 stdout）**：`bootstrap-logs/`
  `start.sh` 把后端 / 前端的 stdout/stderr 分别重定向到 `bootstrap-logs/backend.log` 和 `bootstrap-logs/frontend.log`，用于排查启动失败、依赖加载等问题。

> ⚠️ **IDE 直接 Run 时的注意事项**
>
> `./logs/grace-platform` 是相对路径，相对的是 **JVM 的工作目录（cwd）**，不是项目根。不同启动方式落点不同：
>
> | 启动方式 | JVM cwd | 实际日志位置 |
> |---|---|---|
> | `./start.sh` / `./restart.sh` | 项目根 | `<repo>/logs/grace-platform/` ✅ 推荐 |
> | IDEA / Qoder 直接 Run `GraceApplication` | `grace-platform/` | `<repo>/grace-platform/logs/grace-platform/` |
> | `mvn spring-boot:run`（在 `grace-platform/` 目录） | `grace-platform/` | `<repo>/grace-platform/logs/grace-platform/` |
>
> `start.sh` 通过 `export GRACE_LOG_PATH="$SCRIPT_DIR/logs/grace-platform"` 显式锚定到项目根，规避了这个问题。
>
> 如果你用 IDE 直接 Run 想统一到项目根，可以在 Run Configuration 里设：
> - **Working directory**：`$ProjectFileDir$`，或
> - **Environment variables**：`GRACE_LOG_PATH=$ProjectFileDir$/logs/grace-platform`

## 日志文件位置

| 现象 | 去哪个文件找 |
|------|------------|
| 任意请求日志（INFO+） | `logs/grace-platform/application.log`（当天全量），历史按天滚动到 `logs/grace-platform/YYYY-MM-DD/application.%i.log` |
| 报错 / 异常堆栈（WARN+） | `logs/grace-platform/error.log` |
| 请求出入参、耗时、状态码 | `logs/grace-platform/access.log` |
| LLM / YouTube / OSS / OpenCrawl / 异步任务摘要 | `logs/grace-platform/{llm,youtube,oss,opencrawl,task}-digest.log` |
| 服务启动失败、Spring Boot 启动报错 | `bootstrap-logs/backend.log` |

## 定位流程

1. 从前端 Response Header `X-EagleEye-TraceId` 拿到 traceId
2. `grep "<traceId>" logs/grace-platform/*.log` 串起完整调用链（含异步线程，TTL 自动透传 MDC）
3. 仅看错误：在 `logs/grace-platform/error.log` 里 grep 同一 traceId
4. 看入参/耗时：在 `logs/grace-platform/access.log` 里 grep 同一 traceId（pipe 分隔：`时间|traceId|rpcId|method|uri|status|rt|userIp|userAgent`）

## 快速调试工具

排查具体问题时,优先让 `grace-debug-agent` (Qoder) 自动串联日志 + 数据库,Agent 配置见 `.qoder/agents/grace-debug-agent.md`。

### 查询脚本

`./scripts/grace-db-query.sh` 提供只读的日志 / 数据库查询能力:

```bash
# 按 traceId 串联所有日志
./scripts/grace-db-query.sh trace <traceId>

# 按资源 ID 查详情
./scripts/grace-db-query.sh video <id>

# 常用视图
./scripts/grace-db-query.sh status        # 状态分布
./scripts/grace-db-query.sh recent [n]    # 最近 n 条

# 自定义 SQL (仅 SELECT)
./scripts/grace-db-query.sh "SELECT ... FROM ... WHERE ...;"
./scripts/grace-db-query.sh "SHOW TABLES;"
./scripts/grace-db-query.sh "SHOW COLUMNS FROM <table>;"
```

所有输出统一 JSON 结构: `{ success, rowCount, data: [...] }`。

### Debug API

脚本底层调用后端 `/api/debug/*`,也可直接 curl:

```bash
curl http://localhost:8000/api/debug/video/{id} | jq
curl http://localhost:8000/api/debug/video/status-distribution | jq
curl -X POST http://localhost:8000/api/debug/query \
  -H "Content-Type: application/json" \
  -d '{"query":"SELECT ..."}' | jq
```

**安全机制**: 代码层仅放行 SELECT,拦截 INSERT/UPDATE/DELETE/DROP/ALTER/CREATE/TRUNCATE 等写操作;所有查询落入 application.log。生产环境再加 Profile / IP 白名单 / 认证 Token。

### 排查顺序

```
日志 (发生了什么) → 数据库 (当前状态) → 代码 (业务约束) → 根因结论
```

日志缺失推断表:

| access.log | application.log | error.log | 含义 |
|------------|-----------------|-----------|------|
| ✅ | ❌ | ❌ | 请求在 Filter/Security 层被拦截,未进业务 |
| ✅ | ✅ | ❌ | 正常业务流程,按 status 判断 |
| ✅ | ✅ | ✅ | 业务层抛异常,看堆栈 |
| ❌ | —  | —  | traceId 无效或请求未到达后端 |

### 常见问题

- **脚本提示 "Backend debug endpoint not available"** → 后端未启动,跑 `./start.sh`
- **数据库连接失败** → 检查 `.env` 的 `MYSQL_HOST/USER/PASSWORD/DATABASE`
