package com.grace.platform.shared.infrastructure.async;

import com.alibaba.ttl.threadpool.TtlExecutors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Arrays;
import java.util.concurrent.Executor;

/**
 * Spring {@code @Async} 默认线程池配置。
 * <p>使用 TTL（Transmittable ThreadLocal）包装线程池，
 * MDC（含 traceId/rpcId）自动从父线程透传到子线程，线程池复用也不串号。
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(AsyncConfig.class);

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("grace-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(5);
        executor.initialize();
        return TtlExecutors.getTtlExecutor(executor.getThreadPoolExecutor());
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) ->
                logger.error("Async method {}.{} threw uncaught exception, params={}",
                        method.getDeclaringClass().getSimpleName(),
                        method.getName(),
                        Arrays.toString(params),
                        throwable);
    }
}
