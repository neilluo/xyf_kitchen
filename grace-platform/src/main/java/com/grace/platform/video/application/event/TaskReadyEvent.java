package com.grace.platform.video.application.event;

/**
 * 任务就绪事件，用于链式推进异步上传流水线。
 */
public class TaskReadyEvent {

    private final String taskId;

    public TaskReadyEvent(String taskId) {
        this.taskId = taskId;
    }

    public String getTaskId() {
        return taskId;
    }
}
