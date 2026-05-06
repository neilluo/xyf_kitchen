package com.grace.platform.video.application;

import com.grace.platform.video.application.event.TaskReadyEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class UploadTaskPushListener {
    private static final Logger log = LoggerFactory.getLogger(UploadTaskPushListener.class);
    private final VideoUploadPipelineExecutor executor;

    public UploadTaskPushListener(VideoUploadPipelineExecutor executor) {
        this.executor = executor;
    }

    @Async("uploadTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void on(TaskReadyEvent event) {
        log.info("push listener received TaskReadyEvent taskId={}", event.getTaskId());
        executor.runOnce(event.getTaskId());
    }
}
