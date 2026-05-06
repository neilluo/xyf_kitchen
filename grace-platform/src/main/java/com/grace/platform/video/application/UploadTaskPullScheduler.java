package com.grace.platform.video.application;

import com.grace.platform.video.domain.UploadTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UploadTaskPullScheduler {
    private static final Logger log = LoggerFactory.getLogger(UploadTaskPullScheduler.class);
    private final UploadTaskRepository taskRepository;
    private final VideoUploadPipelineExecutor executor;

    @Value("${grace.upload-task.pull.orphan-threshold-s:60}")
    private int orphanSec;

    @Value("${grace.upload-task.pull.zombie-threshold-s:120}")
    private int zombieSec;

    public UploadTaskPullScheduler(UploadTaskRepository taskRepository, VideoUploadPipelineExecutor executor) {
        this.taskRepository = taskRepository;
        this.executor = executor;
    }

    @Scheduled(fixedDelayString = "${grace.upload-task.pull.scan-interval-ms:30000}")
    public void scan() {
        int reset = taskRepository.resetZombies(zombieSec);
        if (reset > 0) {
            log.warn("pull scheduler reset {} zombies", reset);
        }
        List<String> orphanIds = taskRepository.findOrphanPendingIds(orphanSec);
        for (String id : orphanIds) {
            log.info("pull scheduler picking up orphan task {}", id);
            executor.runOnce(id);
        }
    }
}
