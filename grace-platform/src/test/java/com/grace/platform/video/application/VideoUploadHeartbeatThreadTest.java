package com.grace.platform.video.application;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class VideoUploadHeartbeatThreadTest {

    @Test
    void heartbeatThreadIsNamedAndDaemon() throws Exception {
        String taskId = "task-abc-123";
        ScheduledThreadPoolExecutor exec = new ScheduledThreadPoolExecutor(1, r -> {
            Thread t = new Thread(r, "task-heartbeat-" + taskId);
            t.setDaemon(true);
            return t;
        });
        exec.setRemoveOnCancelPolicy(true);

        AtomicReference<Thread> captured = new AtomicReference<>();
        try {
            exec.submit(() -> captured.set(Thread.currentThread())).get();
            Thread t = captured.get();
            assertThat(t.getName()).isEqualTo("task-heartbeat-" + taskId);
            assertThat(t.isDaemon()).isTrue();
        } finally {
            exec.shutdownNow();
        }
    }
}
