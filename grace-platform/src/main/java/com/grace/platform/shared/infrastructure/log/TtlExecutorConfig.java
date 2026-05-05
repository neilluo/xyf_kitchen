package com.grace.platform.shared.infrastructure.log;

import com.alibaba.ttl.threadpool.TtlExecutors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * TTL（Transmittable ThreadLocal）包装的线程池，提供给业务异步场景使用。
 *
 * <p>用法：
 * <pre>
 *   &#64;Autowired &#64;Qualifier("traceExecutor")
 *   private Executor traceExecutor;
 *
 *   CompletableFuture.supplyAsync(() -&gt; doSomething(), traceExecutor);
 * </pre>
 *
 * <p>MDC（含 traceId/rpcId/appName）会被自动捕获到子线程，线程池复用也不会串号。
 */
@Configuration
public class TtlExecutorConfig {

    @Bean(name = "traceExecutor")
    public Executor traceExecutor() {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(4);
        exec.setMaxPoolSize(16);
        exec.setQueueCapacity(100);
        exec.setThreadNamePrefix("grace-trace-");
        exec.setWaitForTasksToCompleteOnShutdown(true);
        exec.setAwaitTerminationSeconds(5);
        exec.initialize();
        return TtlExecutors.getTtlExecutor(exec.getThreadPoolExecutor());
    }
}
