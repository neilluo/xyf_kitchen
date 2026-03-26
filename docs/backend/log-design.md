# 日志设计规范（Logging Design）

> 依赖文档：[01-project-scaffolding.md](./01-project-scaffolding.md)、[09-infrastructure-config.md](./09-infrastructure-config.md)
> 适用范围：Grace 平台全部后端模块

---

## 1. 技术选型

| 组件 | 选择 | 说明 |
|------|------|------|
| 日志门面 | SLF4J 2.x | Spring Boot 3 默认集成 |
| 日志实现 | Logback 1.4+ | Spring Boot 3 默认实现 |
| 链路追踪 | MDC（Mapped Diagnostic Context） | SLF4J/Logback 原生支持，无额外依赖 |
| 日志滚动 | Logback `TimeBasedRollingPolicy` | 按天滚动 |

---

## 2. Trace ID 链路追踪

### 2.1 设计目标

每一次 HTTP 请求分配一个全局唯一的 `traceId`，贯穿整个请求生命周期的所有日志行。用于：

- 从日志中快速定位一次完整请求的所有处理过程
- 关联同一请求在不同层（Controller → Service → Mapper）的日志
- 返回给前端用于问题反馈（通过 Response Header）

### 2.2 TraceId Filter 实现

使用 Servlet Filter（优先级最高）在请求入口生成 traceId，请求结束时清理：

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter implements Filter {

    private static final String TRACE_ID_KEY = "traceId";
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, 
                         FilterChain chain) throws IOException, ServletException {
        try {
            // 优先从请求头获取（支持上游传递），否则生成新的
            String traceId = null;
            if (request instanceof HttpServletRequest httpRequest) {
                traceId = httpRequest.getHeader(TRACE_ID_HEADER);
            }
            if (traceId == null || traceId.isBlank()) {
                traceId = generateTraceId();
            }

            // 放入 MDC，日志格式中通过 %X{traceId} 引用
            MDC.put(TRACE_ID_KEY, traceId);

            // 写入 Response Header，前端可用于问题反馈
            if (response instanceof HttpServletResponse httpResponse) {
                httpResponse.setHeader(TRACE_ID_HEADER, traceId);
            }

            chain.doFilter(request, response);
        } finally {
            // 必须清理，避免线程复用导致 traceId 串
            MDC.remove(TRACE_ID_KEY);
        }
    }

    private String generateTraceId() {
        // 格式：grc-{时间戳hex}-{随机4字节hex}，共 20 字符左右
        // 比纯 UUID 更短，且包含时间信息便于排序
        long timestamp = System.currentTimeMillis();
        byte[] random = new byte[4];
        ThreadLocalRandom.current().nextBytes(random);
        return "grc-" + Long.toHexString(timestamp) + "-" + HexFormat.of().formatHex(random);
    }
}
```

### 2.3 异步线程传递

领域事件或 `@Async` 异步任务需要手动传递 MDC 上下文：

```java
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("grace-async-");
        // 包装：自动传递 MDC 上下文到子线程
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.initialize();
        return executor;
    }
}

public class MdcTaskDecorator implements TaskDecorator {
    @Override
    public Runnable decorate(Runnable runnable) {
        // 捕获父线程的 MDC 上下文
        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        return () -> {
            try {
                if (contextMap != null) {
                    MDC.setContextMap(contextMap);
                }
                runnable.run();
            } finally {
                MDC.clear();
            }
        };
    }
}
```

---

## 3. 请求出入参日志

### 3.1 设计目标

自动记录每次 HTTP 请求的：
- **入参**：HTTP Method、URI、Query Parameters、Request Body（POST/PUT）
- **出参**：HTTP Status Code、Response Body 摘要、耗时（ms）

### 3.2 RequestResponseLoggingInterceptor 实现

```java
@Component
public class RequestResponseLoggingInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RequestResponseLoggingInterceptor.class);
    private static final String START_TIME_ATTR = "requestStartTime";
    private static final int MAX_BODY_LOG_LENGTH = 1024;  // Body 最多记录 1KB

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) {
        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());

        if (log.isInfoEnabled()) {
            String queryString = request.getQueryString();
            log.info("[REQ] {} {} {}",
                     request.getMethod(),
                     request.getRequestURI(),
                     queryString != null ? "?" + queryString : "");
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        Long startTime = (Long) request.getAttribute(START_TIME_ATTR);
        long elapsed = startTime != null ? System.currentTimeMillis() - startTime : -1;

        if (ex != null) {
            log.error("[RES] {} {} | status={} | {}ms | error={}",
                      request.getMethod(), request.getRequestURI(),
                      response.getStatus(), elapsed, ex.getMessage());
        } else {
            log.info("[RES] {} {} | status={} | {}ms",
                     request.getMethod(), request.getRequestURI(),
                     response.getStatus(), elapsed);
        }
    }
}
```

### 3.3 Request Body 日志（可选增强）

对于 POST/PUT 请求，需要包装 `HttpServletRequest` 以允许多次读取 Body：

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)  // 仅次于 TraceIdFilter
public class CachedBodyFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest httpRequest) {
            String method = httpRequest.getMethod();
            if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {
                ContentCachingRequestWrapper wrappedRequest = 
                    new ContentCachingRequestWrapper(httpRequest);
                chain.doFilter(wrappedRequest, response);
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
```

在 Interceptor 中读取缓存的 Body：

```java
// 在 afterCompletion 中获取 Request Body
if (request instanceof ContentCachingRequestWrapper wrapper) {
    byte[] body = wrapper.getContentAsByteArray();
    if (body.length > 0) {
        String bodyStr = new String(body, wrapper.getCharacterEncoding());
        String truncated = bodyStr.length() > MAX_BODY_LOG_LENGTH 
            ? bodyStr.substring(0, MAX_BODY_LOG_LENGTH) + "...(truncated)" 
            : bodyStr;
        log.debug("[REQ-BODY] {}", truncated);
    }
}
```

### 3.4 敏感字段脱敏

以下字段在日志中 **禁止** 明文输出：

| 字段 | 脱敏规则 | 示例 |
|------|---------|------|
| `password` | 完全遮蔽 | `***` |
| `accessToken` / `refreshToken` | 保留前 6 位 | `ya29.a0***` |
| `apiKey` | 保留前缀 | `grc_***` |
| `encryptionKey` | 完全遮蔽 | `***` |

脱敏通过 Jackson 序列化注解实现（用于 Body 日志）：

```java
@JsonSerialize(using = MaskedSerializer.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Masked {
    int keepPrefix() default 0;  // 保留前缀字符数
}

public class MaskedSerializer extends JsonSerializer<String> {
    // ... 根据 @Masked 注解的 keepPrefix 值进行脱敏
}
```

### 3.5 注册 Interceptor

```java
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final RequestResponseLoggingInterceptor loggingInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loggingInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/health");  // 健康检查不记录
    }
}
```

---

## 4. 日志格式规范

### 4.1 统一格式

```
时间戳 [线程名] 日志级别 traceId loggerName - 消息体
```

具体 Pattern：

```
%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%X{traceId:-NO_TRACE}] %logger{40} - %msg%n
```

**示例输出：**

```
2026-03-26 14:22:31.456 [http-nio-8080-exec-1] INFO  [grc-18e3f5a2b0c-1a2b3c4d] c.g.p.s.i.w.RequestResponseLoggingInterceptor - [REQ] POST /api/videos/upload/init
2026-03-26 14:22:31.460 [http-nio-8080-exec-1] DEBUG [grc-18e3f5a2b0c-1a2b3c4d] c.g.p.video.application.VideoUploadService - Initializing upload session: fileName=demo.mp4, fileSize=104857600
2026-03-26 14:22:31.485 [http-nio-8080-exec-1] INFO  [grc-18e3f5a2b0c-1a2b3c4d] c.g.p.s.i.w.RequestResponseLoggingInterceptor - [RES] POST /api/videos/upload/init | status=200 | 29ms
```

### 4.2 格式字段说明

| 字段 | Pattern | 说明 |
|------|---------|------|
| 时间戳 | `%d{yyyy-MM-dd HH:mm:ss.SSS}` | 精确到毫秒 |
| 线程名 | `%thread` | 用于识别并发请求 |
| 日志级别 | `%-5level` | 左对齐，5 字符宽度 |
| Trace ID | `%X{traceId:-NO_TRACE}` | MDC 中的 traceId，无则显示 NO_TRACE |
| Logger 名 | `%logger{40}` | 限制 40 字符，自动缩写包名 |
| 消息体 | `%msg` | 实际日志内容 |

---

## 5. 日志滚动策略（按天滚动）

### 5.1 Logback 配置文件

创建 `src/main/resources/logback-spring.xml`：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration scan="true" scanPeriod="30 seconds">

    <!-- ==================== 属性定义 ==================== -->
    <property name="LOG_DIR" value="${LOG_DIR:-./logs}" />
    <property name="APP_NAME" value="grace-platform" />
    <property name="LOG_PATTERN" 
              value="%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%X{traceId:-NO_TRACE}] %logger{40} - %msg%n" />

    <!-- ==================== 控制台输出 ==================== -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>${LOG_PATTERN}</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <!-- ==================== 全量日志文件（按天滚动） ==================== -->
    <appender name="FILE_ALL" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_DIR}/${APP_NAME}.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <!-- 按天滚动，文件名包含日期 -->
            <fileNamePattern>${LOG_DIR}/${APP_NAME}.%d{yyyy-MM-dd}.log</fileNamePattern>
            <!-- 保留 30 天 -->
            <maxHistory>30</maxHistory>
            <!-- 总大小上限 10GB -->
            <totalSizeCap>10GB</totalSizeCap>
        </rollingPolicy>
        <encoder>
            <pattern>${LOG_PATTERN}</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <!-- ==================== ERROR 级别单独文件（按天滚动） ==================== -->
    <appender name="FILE_ERROR" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_DIR}/${APP_NAME}-error.log</file>
        <filter class="ch.qos.logback.classic.filter.LevelFilter">
            <level>ERROR</level>
            <onMatch>ACCEPT</onMatch>
            <onMismatch>DENY</onMismatch>
        </filter>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${LOG_DIR}/${APP_NAME}-error.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>60</maxHistory>
            <totalSizeCap>5GB</totalSizeCap>
        </rollingPolicy>
        <encoder>
            <pattern>${LOG_PATTERN}</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <!-- ==================== 请求出入参专用文件（按天滚动） ==================== -->
    <appender name="FILE_ACCESS" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_DIR}/${APP_NAME}-access.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${LOG_DIR}/${APP_NAME}-access.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>15</maxHistory>
            <totalSizeCap>5GB</totalSizeCap>
        </rollingPolicy>
        <encoder>
            <pattern>${LOG_PATTERN}</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <!-- ==================== Logger 配置 ==================== -->

    <!-- 请求出入参日志 → 同时写入 access 文件 -->
    <logger name="com.grace.platform.shared.infrastructure.web.RequestResponseLoggingInterceptor"
            level="INFO" additivity="true">
        <appender-ref ref="FILE_ACCESS" />
    </logger>

    <!-- MyBatis SQL 日志（开发环境 DEBUG，生产 INFO） -->
    <springProfile name="dev">
        <logger name="com.grace.platform" level="DEBUG" />
        <!-- 开发环境输出 MyBatis SQL -->
        <logger name="org.apache.ibatis" level="DEBUG" />
    </springProfile>

    <springProfile name="prod">
        <logger name="com.grace.platform" level="INFO" />
        <logger name="org.apache.ibatis" level="WARN" />
    </springProfile>

    <!-- ==================== Root Logger ==================== -->
    <root level="INFO">
        <appender-ref ref="CONSOLE" />
        <appender-ref ref="FILE_ALL" />
        <appender-ref ref="FILE_ERROR" />
    </root>

</configuration>
```

### 5.2 日志文件目录结构

```
logs/
├── grace-platform.log                  # 当天全量日志
├── grace-platform.2026-03-25.log       # 昨天全量日志
├── grace-platform.2026-03-24.log       # 前天全量日志
├── grace-platform-error.log            # 当天 ERROR 日志
├── grace-platform-error.2026-03-25.log # 昨天 ERROR 日志
├── grace-platform-access.log           # 当天请求出入参日志
└── grace-platform-access.2026-03-25.log# 昨天请求出入参日志
```

### 5.3 滚动策略参数汇总

| 日志文件 | 滚动周期 | 保留天数 | 总大小上限 |
|---------|---------|---------|-----------|
| 全量日志 `*.log` | 每天 | 30 天 | 10 GB |
| ERROR 日志 `*-error.log` | 每天 | 60 天 | 5 GB |
| Access 日志 `*-access.log` | 每天 | 15 天 | 5 GB |

---

## 6. 各层日志级别规范

### 6.1 日志级别使用原则

| 级别 | 用途 | 示例 |
|------|------|------|
| `ERROR` | 需要立即关注的异常，影响业务功能 | 外部服务调用失败、数据库连接失败、加密解密异常 |
| `WARN` | 潜在问题，暂不影响业务但需关注 | 重试操作、配置降级使用默认值、即将过期的 Token |
| `INFO` | 关键业务流程节点 | 请求出入参、领域事件发布/消费、状态变更 |
| `DEBUG` | 开发调试信息 | SQL 参数、中间计算结果、条件分支走向 |

### 6.2 各层日志规范

| 层 | 包 | 生产级别 | 日志内容 |
|----|-----|---------|---------|
| Controller | `*.interfaces.rest` | INFO | 由 Interceptor 统一处理，Controller 内不重复记录出入参 |
| Application Service | `*.application` | INFO | 业务操作开始/完成、领域事件发布 |
| Domain Service | `*.domain.service` | INFO | 业务规则命中、策略选择 |
| Domain Event | `*.domain.event` | INFO | 事件发布与消费 |
| Infrastructure | `*.infrastructure` | INFO | 外部服务调用结果、重试情况 |
| MyBatis Mapper | `org.apache.ibatis` | WARN(prod) | 生产环境关闭 SQL 日志，开发环境 DEBUG |

### 6.3 日志编写规范

```java
// GOOD: 使用参数化日志（SLF4J 延迟求值，避免无谓字符串拼接）
log.info("Video uploaded: videoId={}, fileName={}, fileSize={}", 
         videoId, fileName, fileSize);

// BAD: 字符串拼接（即使日志级别不满足也会执行拼接）
log.info("Video uploaded: videoId=" + videoId + ", fileName=" + fileName);

// GOOD: 异常日志带完整堆栈
log.error("Failed to publish video: videoId={}, platform={}", 
          videoId, platform, exception);

// BAD: 吞掉异常堆栈
log.error("Failed to publish video: " + exception.getMessage());

// GOOD: DEBUG 级别包裹判断（避免复杂对象的 toString 开销）
if (log.isDebugEnabled()) {
    log.debug("Metadata generation result: {}", metadata.toDetailString());
}
```

---

## 7. 领域事件日志

所有领域事件的发布和消费都需要记录日志：

```java
// 事件发布方
log.info("[EVENT-PUBLISH] type={}, payload={}", 
         event.getClass().getSimpleName(), event);

// 事件消费方
log.info("[EVENT-CONSUME] type={}, handler={}", 
         event.getClass().getSimpleName(), this.getClass().getSimpleName());
```

**日志标签前缀约定：**

| 前缀 | 用途 | 示例 |
|------|------|------|
| `[REQ]` | 请求入参 | `[REQ] POST /api/videos/upload/init` |
| `[RES]` | 请求出参 | `[RES] POST /api/videos/upload/init \| status=200 \| 29ms` |
| `[REQ-BODY]` | 请求 Body | `[REQ-BODY] {"fileName":"demo.mp4",...}` |
| `[EVENT-PUBLISH]` | 领域事件发布 | `[EVENT-PUBLISH] type=VideoUploadedEvent` |
| `[EVENT-CONSUME]` | 领域事件消费 | `[EVENT-CONSUME] type=VideoUploadedEvent` |
| `[SLOW-SQL]` | 慢 SQL 告警 | `[SLOW-SQL] 1523ms \| mapper=VideoMapper.xml` |
| `[EXT-CALL]` | 外部服务调用 | `[EXT-CALL] YouTube API upload start` |
| `[EXT-RESP]` | 外部服务响应 | `[EXT-RESP] YouTube API upload \| 200 \| 3421ms` |
| `[RETRY]` | 重试操作 | `[RETRY] attempt=2/3 \| YouTube API quota exceeded` |

---

## 8. 完整组件注册清单

| 组件 | 类型 | 包路径 | 作用 |
|------|------|--------|------|
| `TraceIdFilter` | Servlet Filter | `com.grace.platform.shared.infrastructure.web` | 生成/传递 traceId |
| `CachedBodyFilter` | Servlet Filter | `com.grace.platform.shared.infrastructure.web` | 缓存 Request Body |
| `RequestResponseLoggingInterceptor` | HandlerInterceptor | `com.grace.platform.shared.infrastructure.web` | 记录请求出入参 |
| `MdcTaskDecorator` | TaskDecorator | `com.grace.platform.shared.infrastructure.async` | 异步线程 MDC 传递 |
| `AsyncConfig` | Configuration | `com.grace.platform.shared.infrastructure.async` | 异步线程池配置 |
| `SlowSqlInterceptor` | MyBatis Interceptor | `com.grace.platform.shared.infrastructure.persistence` | 慢 SQL 检测 |
| `WebMvcConfig` | Configuration | `com.grace.platform.shared.infrastructure.web` | 注册 Interceptor |
| `logback-spring.xml` | XML Config | `src/main/resources/` | Logback 主配置文件 |

---

## 9. 问题排查 Checklist

当需要通过日志定位问题时，按以下步骤操作：

```
1. 获取 traceId
   └── 来源：前端报错时 Response Header 中的 X-Trace-Id
              或用户反馈提供的 traceId

2. 搜索全量日志
   └── grep "grc-18e3f5a2b0c-1a2b3c4d" logs/grace-platform.log
       grep "grc-18e3f5a2b0c-1a2b3c4d" logs/grace-platform.2026-03-25.log

3. 查看请求出入参
   └── grep "grc-18e3f5a2b0c-1a2b3c4d" logs/grace-platform-access.log

4. 检查 ERROR 日志
   └── grep "grc-18e3f5a2b0c-1a2b3c4d" logs/grace-platform-error.log

5. 关联异步操作
   └── 同一个 traceId 在异步线程中也会保留（通过 MdcTaskDecorator）

6. 检查慢 SQL
   └── grep "\[SLOW-SQL\]" logs/grace-platform.log | grep "2026-03-26"
```
