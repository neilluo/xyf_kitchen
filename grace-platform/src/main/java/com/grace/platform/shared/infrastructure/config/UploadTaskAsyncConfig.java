package com.grace.platform.shared.infrastructure.config;

import com.alibaba.ttl.threadpool.TtlExecutors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步上传任务专用线程池。
 *
 * <p>使用 TTL 包装，MDC（含 traceId/rpcId）自动从父线程透传到子线程。
 * <p>{@code @EnableAsync} 和 {@code @EnableScheduling} 已分别在
 * {@code shared.infrastructure.async.AsyncConfig} 和
 * {@code GracePlatformApplication} 上声明，此处不重复。
 */
@Configuration
public class UploadTaskAsyncConfig {

    @Bean("uploadTaskExecutor")
    public Executor uploadTaskExecutor(
            @Value("${grace.upload-task.executor.core-pool-size:4}") int core,
            @Value("${grace.upload-task.executor.max-pool-size:8}") int max,
            @Value("${grace.upload-task.executor.queue-capacity:50}") int queue) {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(core);
        exec.setMaxPoolSize(max);
        exec.setQueueCapacity(queue);
        exec.setThreadNamePrefix("upload-task-");
        exec.setWaitForTasksToCompleteOnShutdown(true);
        exec.setAwaitTerminationSeconds(5);
        exec.initialize();
        return TtlExecutors.getTtlExecutor(exec.getThreadPoolExecutor());
    }
}
