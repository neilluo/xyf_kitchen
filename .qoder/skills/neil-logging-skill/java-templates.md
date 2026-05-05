# Java 代码模板

六个核心类的完整模板，全部使用占位符 `{base}`（项目基础包，如 `com.example.app`）。
直接复制后 sed 替换占位符即可编译运行。

**占位符总览**：

| 占位符 | 含义 | 示例 |
|--------|------|------|
| `{base}` | 项目基础包 | `com.example.app` |
| `{EXTERNAL_NAME}` | 外部服务名小写 | `openai` / `oss` / `payment` |
| `{EXTERNAL_NAME_UPPER}` | 外部服务名大写 | `OPENAI` / `OSS` / `PAYMENT` |
| `{externalMethod}` | Java 方法名（驼峰） | `openai` / `oss` / `payment` |

---

## 1. EagleEyeIdGenerator.java

```java
package {base}.log;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 生成 30 位 EagleEye 风格 traceId。
 * 格式：IP(8hex) + 时间戳(13位十进制ms) + 自增序号(4位) + pid末2位(2位) + 随机扩展(3位)
 */
public final class EagleEyeIdGenerator {

    private static final String IP_PREFIX;
    private static final String PID_SUFFIX;
    private static final AtomicInteger SEQ = new AtomicInteger(0);

    static {
        IP_PREFIX = resolveIpHex();
        PID_SUFFIX = resolvePidHex();
    }

    private EagleEyeIdGenerator() {}

    public static String next() {
        long ts = System.currentTimeMillis();                       // 13
        int  seq = SEQ.getAndUpdate(x -> (x + 1) % 10000);          // 4
        int  ext = (int) (Math.random() * 0xFFF);                   // 3
        return IP_PREFIX
                + ts
                + String.format("%04d", seq)
                + PID_SUFFIX
                + String.format("%03x", ext);
    }

    private static String resolveIpHex() {
        try {
            byte[] ip = InetAddress.getLocalHost().getAddress();
            StringBuilder sb = new StringBuilder(8);
            for (byte b : ip) sb.append(String.format("%02x", b & 0xFF));
            return sb.toString();
        } catch (UnknownHostException e) {
            return "7f000001"; // 127.0.0.1 降级
        }
    }

    private static String resolvePidHex() {
        try {
            String name = ManagementFactory.getRuntimeMXBean().getName();
            int pid = Integer.parseInt(name.split("@")[0]);
            return String.format("%02x", pid & 0xFF);
        } catch (Exception e) {
            return "00";
        }
    }
}
```

---

## 2. TraceContext.java

```java
package {base}.log;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * Trace 上下文工具。封装 MDC 读写与 rpcId 层级推进。
 * 所有方法内部防御式编程，不外抛。
 */
public final class TraceContext {

    public static final String MDC_TRACE_ID = "traceId";
    public static final String MDC_RPC_ID   = "rpcId";
    public static final String MDC_APP_NAME = "appName";

    private static final String HEADER_TRACE_ID = "X-EagleEye-TraceId";
    private static final String HEADER_RPC_ID   = "X-EagleEye-RpcId";

    // 每层 span 的子自增计数
    private static final ThreadLocal<Integer> CHILD_COUNTER = ThreadLocal.withInitial(() -> 0);

    private TraceContext() {}

    /** 入口调用：读取或生成 trace，写入 MDC，返回 traceId */
    public static String startTrace(HttpServletRequest req, String appName) {
        try {
            String tid = Optional.ofNullable(req.getHeader(HEADER_TRACE_ID))
                                 .filter(StringUtils::hasText)
                                 .orElseGet(EagleEyeIdGenerator::next);
            String rid = Optional.ofNullable(req.getHeader(HEADER_RPC_ID))
                                 .filter(StringUtils::hasText)
                                 .orElse("0");
            MDC.put(MDC_TRACE_ID, tid);
            MDC.put(MDC_RPC_ID, rid);
            MDC.put(MDC_APP_NAME, appName);
            CHILD_COUNTER.set(0);
            return tid;
        } catch (Exception e) {
            // 降级：空 trace 但不阻断主链路
            return "";
        }
    }

    /** 进入一层子调用；返回父 rpcId 供调用方在 finally 里 popSpan 恢复 */
    public static String pushSpan() {
        try {
            String parent = Optional.ofNullable(MDC.get(MDC_RPC_ID)).orElse("0");
            int seq = CHILD_COUNTER.get() + 1;
            CHILD_COUNTER.set(seq);
            String child = parent + "." + seq;
            MDC.put(MDC_RPC_ID, child);
            return parent;
        } catch (Exception e) {
            return "0";
        }
    }

    public static void popSpan(String parent) {
        try {
            MDC.put(MDC_RPC_ID, parent == null ? "0" : parent);
        } catch (Exception ignored) {}
    }

    public static void clear() {
        try {
            MDC.clear();
            CHILD_COUNTER.remove();
        } catch (Exception ignored) {}
    }

    public static String currentTraceId() {
        return Optional.ofNullable(MDC.get(MDC_TRACE_ID)).orElse("");
    }

    public static String currentRpcId() {
        return Optional.ofNullable(MDC.get(MDC_RPC_ID)).orElse("0");
    }
}
```

---

## 3. EagleEyeFilter.java

```java
package {base}.log;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * HTTP 入口 trace 建立 Filter。必须在所有其他 Filter 之前执行。
 * 任何日志模块异常都不阻断请求。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class EagleEyeFilter extends OncePerRequestFilter {

    @Value("${spring.application.name:app}")
    private String appName;

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse resp,
                                    FilterChain chain) throws ServletException, IOException {
        String tid = "";
        try {
            tid = TraceContext.startTrace(req, appName);
            if (!tid.isEmpty()) {
                resp.setHeader("X-EagleEye-TraceId", tid);
            }
        } catch (Exception e) {
            System.err.println("[EagleEyeFilter] trace init failed: " + e.getMessage());
        }
        try {
            chain.doFilter(req, resp);
        } finally {
            TraceContext.clear();
        }
    }
}
```

---

## 4. AccessLogFilter.java

```java
package {base}.log;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 紧跟 EagleEyeFilter 之后执行，写 access.log。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class AccessLogFilter extends OncePerRequestFilter {

    private static final Logger ACCESS = LoggerFactory.getLogger("ACCESS_LOG");

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse resp,
                                    FilterChain chain) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        int status = 200;
        try {
            chain.doFilter(req, resp);
            status = resp.getStatus();
        } catch (Exception e) {
            status = 500;
            throw e;
        } finally {
            try {
                long rt = System.currentTimeMillis() - start;
                // 格式：method|uri|status|rt|userIp|userAgent
                ACCESS.info("{}|{}|{}|{}|{}|{}",
                        safe(req.getMethod()),
                        safe(req.getRequestURI()),
                        status,
                        rt,
                        safe(req.getRemoteAddr()),
                        safe(req.getHeader("User-Agent")));
            } catch (Exception ignored) {
                // 日志写失败不影响响应
            }
        }
    }

    private String safe(String s) {
        if (s == null) return "-";
        return s.replace("|", "\\|");
    }
}
```

---

## 5. DigestLogger.java

每个外部服务一个静态方法，字段顺序自定义但必须稳定（落盘后要能被脚本解析）。
模板给出 **1 个通用外部服务** (`{EXTERNAL_NAME}`) 与 **1 个任务 digest** 的示例；
有多个外部服务时，复制 `{externalMethod}(...)` 方法与对应 `Logger` 常量即可。

```java
package {base}.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Digest 日志工具类。每种外部调用/业务事件一行 pipe 分隔摘要。
 *
 * 所有方法内部全量 try/catch，日志异常绝不外抛。
 * 为每个外部服务新增一个静态方法 + 对应 Logger 常量。
 */
public final class DigestLogger {

    // 每个外部服务声明一个 logger，name 与 logback-spring.xml 中的 <logger name=...> 对应
    private static final Logger {EXTERNAL_NAME_UPPER} = LoggerFactory.getLogger("{EXTERNAL_NAME_UPPER}_DIGEST");
    private static final Logger TASK                  = LoggerFactory.getLogger("TASK_DIGEST");

    private DigestLogger() {}

    /**
     * {EXTERNAL_NAME} 调用摘要。字段顺序：api|resource|cost(ms)|status|errCode|bizId
     * 字段可按实际业务调整，但顺序一旦落地不得再改（兼容离线脚本）。
     */
    public static void {externalMethod}(String api, String resource, long costMs,
                                        String status, String errCode, String bizId) {
        try {
            {EXTERNAL_NAME_UPPER}.info("{}|{}|{}|{}|{}|{}",
                    esc(api), esc(resource), costMs,
                    esc(status), esc(errCode), esc(bizId));
        } catch (Exception e) {
            System.err.println("[DigestLogger.{externalMethod}] " + e.getMessage());
        }
    }

    /**
     * 任务状态变更摘要。字段顺序：taskId|bizType|oldStatus|newStatus|progress
     */
    public static void task(String taskId, String bizType,
                            String oldStatus, String newStatus, int progress) {
        try {
            TASK.info("{}|{}|{}|{}|{}",
                    esc(taskId), esc(bizType),
                    esc(oldStatus), esc(newStatus), progress);
        } catch (Exception e) {
            System.err.println("[DigestLogger.task] " + e.getMessage());
        }
    }

    /** null → "-"，含 | 的值转义 */
    private static String esc(String s) {
        if (s == null || s.isEmpty()) return "-";
        return s.replace("|", "\\|");
    }
}
```

---

## 6. TtlExecutorConfig.java

```java
package {base}.config;

import com.alibaba.ttl.threadpool.TtlExecutors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 提供一个 TTL 包装的线程池，用于所有业务 CompletableFuture/异步任务。
 * MDC（含 traceId/rpcId）会被自动捕获到子线程，线程池复用也不会串号。
 */
@Configuration
public class TtlExecutorConfig {

    @Bean(name = "traceExecutor")
    public Executor traceExecutor() {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(8);
        exec.setMaxPoolSize(32);
        exec.setQueueCapacity(100);
        exec.setThreadNamePrefix("trace-async-");
        exec.setWaitForTasksToCompleteOnShutdown(true);
        exec.setAwaitTerminationSeconds(5);
        exec.initialize();
        return TtlExecutors.getTtlExecutor(exec.getThreadPoolExecutor());
    }
}
```

---

## 业务代码改造示例

### 异步场景

```java
// 改前
CompletableFuture.supplyAsync(() -> doSomething());

// 改后
@Autowired @Qualifier("traceExecutor")
private Executor traceExecutor;

CompletableFuture.supplyAsync(() -> doSomething(), traceExecutor);
```

### 外部调用场景（以任意外部服务 `{externalMethod}` 为例）

```java
public String callExternal(String bizId, String resource) {
    String parentRpc = TraceContext.pushSpan();
    long start = System.currentTimeMillis();
    String status = "OK";
    String errCode = "-";
    String returnId = null;
    try {
        returnId = httpClient.post(...);
        return returnId;
    } catch (Exception e) {
        status = "FAIL";
        errCode = e.getClass().getSimpleName();
        throw e;
    } finally {
        DigestLogger.{externalMethod}(
                "callExternal", resource,
                System.currentTimeMillis() - start,
                status, errCode, returnId);
        TraceContext.popSpan(parentRpc);
    }
}
```

---

## 单元测试模板

```java
// EagleEyeIdGeneratorTest.java
@Test
void shouldGenerate30CharIds() {
    for (int i = 0; i < 100; i++) {
        String id = EagleEyeIdGenerator.next();
        assertEquals(30, id.length());
        assertTrue(id.matches("[0-9a-f]+"));
    }
}

@Test
void shouldBeUniqueUnderConcurrency() throws Exception {
    Set<String> set = ConcurrentHashMap.newKeySet();
    ExecutorService pool = Executors.newFixedThreadPool(32);
    CountDownLatch latch = new CountDownLatch(10_000);
    for (int i = 0; i < 10_000; i++) {
        pool.submit(() -> { set.add(EagleEyeIdGenerator.next()); latch.countDown(); });
    }
    latch.await();
    assertEquals(10_000, set.size(), "1万次并发无重复");
    pool.shutdown();
}
```

```java
// TraceContextTest.java
@Test
void rpcIdShouldNest() {
    MDC.put("rpcId", "0");
    String p1 = TraceContext.pushSpan();     assertEquals("0.1", MDC.get("rpcId"));
    String p2 = TraceContext.pushSpan();     assertEquals("0.1.1", MDC.get("rpcId"));
    TraceContext.popSpan(p2);                assertEquals("0.1", MDC.get("rpcId"));
    TraceContext.popSpan(p1);                assertEquals("0", MDC.get("rpcId"));
}
```
