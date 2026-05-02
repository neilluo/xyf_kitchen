# 日志排查索引（Debugger 入口）

> 用途：出问题时，按下表去对应文件定位。具体调试由 debugger agent 负责。

## 日志文件位置

| 现象 | 去哪个文件找 |
|------|------------|
| 任意请求异常 | `logs/grace-platform.log`（当天全量），历史按天滚动为 `grace-platform.YYYY-MM-DD.log` |
| 报错 / 异常堆栈 | `logs/grace-platform-error.log` |
| 请求出入参、耗时、状态码 | `logs/grace-platform-access.log` |

## 定位流程

1. 从前端 Response Header `X-Trace-Id` 拿到 traceId
2. `grep "<traceId>" logs/grace-platform*.log` 串起完整调用链（含异步线程）
3. 仅看错误：在 `grace-platform-error.log` 里 grep 同一 traceId
4. 看入参/耗时：在 `grace-platform-access.log` 里 grep 同一 traceId
