package com.grace.platform.video;

import com.grace.platform.video.domain.UploadTask;
import com.grace.platform.video.domain.UploadTaskStatus;
import com.grace.platform.video.domain.UploadTaskStep;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for UploadTask domain entity.
 */
class UploadTaskUnitTest {

    @Test
    @DisplayName("createInitial 应创建 PENDING_MERGE 状态的初始任务")
    void createInitial_shouldCreatePendingMergeTask() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("uploadId", "test-upload-id");
        Map<String, Object> extInfo = new LinkedHashMap<>();

        UploadTask task = UploadTask.createInitial("vid_123", payload, extInfo);

        assertThat(task.getId()).startsWith("task_");
        assertThat(task.getVideoId()).isEqualTo("vid_123");
        assertThat(task.getCurrentStep()).isEqualTo(UploadTaskStep.PENDING_MERGE);
        assertThat(task.getStatus()).isEqualTo(UploadTaskStatus.PENDING);
        assertThat(task.getAttempts()).isZero();
        assertThat(task.getMaxAttempts()).isEqualTo(5);
        assertThat(task.getPayload()).containsEntry("uploadId", "test-upload-id");
    }

    @Test
    @DisplayName("markRunning 应推进到 RUNNING 状态和对应 RUNNING step")
    void markRunning_shouldAdvanceToRunningStep() {
        UploadTask task = UploadTask.createInitial("vid_123", null, null);
        task.markRunning("instance-1");

        assertThat(task.getStatus()).isEqualTo(UploadTaskStatus.RUNNING);
        assertThat(task.getCurrentStep()).isEqualTo(UploadTaskStep.RUNNING_MERGE);
        assertThat(task.getOwnerInstanceId()).isEqualTo("instance-1");
        assertThat(task.getHeartbeatAt()).isNotNull();
    }

    @Test
    @DisplayName("advanceToNextPending 应推进到下一 PENDING 步骤")
    void advanceToNextPending_shouldAdvanceToNextStep() {
        UploadTask task = UploadTask.createInitial("vid_123", null, null);
        task.markRunning("instance-1");
        task.advanceToNextPending();

        assertThat(task.getCurrentStep()).isEqualTo(UploadTaskStep.PENDING_INSPECT);
        assertThat(task.getStatus()).isEqualTo(UploadTaskStatus.PENDING);
        assertThat(task.getAttempts()).isZero();
        assertThat(task.getOwnerInstanceId()).isNull();
        assertThat(task.getHeartbeatAt()).isNull();
    }

    @Test
    @DisplayName("advanceToNextPending 从 RUNNING_CLEANUP 应推进到 DONE")
    void advanceToNextPending_fromCleanup_shouldComplete() {
        UploadTask task = UploadTask.createInitial("vid_123", null, null);
        // 模拟推进到最后一个步骤
        task.markRunning("instance-1");
        task.advanceToNextPending(); // PENDING_INSPECT
        task.markRunning("instance-1");
        task.advanceToNextPending(); // PENDING_OSS
        task.markRunning("instance-1");
        task.advanceToNextPending(); // PENDING_METADATA
        task.markRunning("instance-1");
        task.advanceToNextPending(); // PENDING_CLEANUP
        task.markRunning("instance-1");
        task.advanceToNextPending(); // DONE

        assertThat(task.getCurrentStep()).isEqualTo(UploadTaskStep.DONE);
        assertThat(task.getStatus()).isEqualTo(UploadTaskStatus.DONE);
    }

    @Test
    @DisplayName("markFailed 在 attempts < maxAttempts 时应回滚到 PENDING")
    void markFailed_withRetriesRemaining_shouldRollbackToPending() {
        UploadTask task = UploadTask.createInitial("vid_123", null, null);
        task.markRunning("instance-1");

        // 模拟 claim 中 attempts+1
        simulateClaim(task);

        task.markFailed(new RuntimeException("Test error"));

        assertThat(task.getStatus()).isEqualTo(UploadTaskStatus.PENDING);
        assertThat(task.getCurrentStep()).isEqualTo(UploadTaskStep.PENDING_MERGE);
        assertThat(task.getLastError()).contains("Test error");
    }

    @Test
    @DisplayName("markFailed 在 attempts >= maxAttempts 时应进入终态 FAILED")
    void markFailed_atMaxAttempts_shouldFailPermanently() {
        UploadTask task = UploadTask.createInitial("vid_123", null, null);
        task.markRunning("instance-1");

        // 模拟 attempts 达到 maxAttempts
        task.markFailed(new RuntimeException("Test error"));
        // markFailed 不会 attempts++，所以需要手动设置为 maxAttempts
        // 这里通过反射或修改测试逻辑来模拟
        // 简化处理：直接创建已耗尽 attempts 的 task
        UploadTask exhaustedTask = UploadTask.createInitial("vid_123", null, null);
        exhaustedTask.markRunning("instance-1");
        // 手动设置 attempts 为 maxAttempts（模拟 claim 已多次 +1）
        setAttempts(exhaustedTask, 5);

        exhaustedTask.markFailed(new RuntimeException("Final error"));

        assertThat(exhaustedTask.getStatus()).isEqualTo(UploadTaskStatus.FAILED);
        assertThat(exhaustedTask.getCurrentStep()).isEqualTo(UploadTaskStep.RUNNING_MERGE);
        assertThat(exhaustedTask.getLastError()).contains("Final error");
    }

    @Test
    @DisplayName("touchHeartbeat 应更新心跳时间")
    void touchHeartbeat_shouldUpdateHeartbeat() {
        UploadTask task = UploadTask.createInitial("vid_123", null, null);
        task.markRunning("instance-1");

        var beforeHeartbeat = task.getHeartbeatAt();
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        task.touchHeartbeat();

        assertThat(task.getHeartbeatAt()).isAfterOrEqualTo(beforeHeartbeat);
    }

    @Test
    @DisplayName("resetForManualRetry 应重置到 PENDING_METADATA")
    void resetForManualRetry_shouldResetToPendingMetadata() {
        UploadTask task = UploadTask.createInitial("vid_123", null, null);
        task.markRunning("instance-1");
        task.markFailed(new RuntimeException("LLM error"));

        task.resetForManualRetry();

        assertThat(task.getCurrentStep()).isEqualTo(UploadTaskStep.PENDING_METADATA);
        assertThat(task.getStatus()).isEqualTo(UploadTaskStatus.PENDING);
        assertThat(task.getAttempts()).isZero();
        assertThat(task.getLastError()).isNull();
    }

    @Test
    @DisplayName("UploadTaskStep.toRunning 应正确转换")
    void stepToRunning_shouldConvertCorrectly() {
        assertThat(UploadTaskStep.PENDING_MERGE.toRunning()).isEqualTo(UploadTaskStep.RUNNING_MERGE);
        assertThat(UploadTaskStep.PENDING_INSPECT.toRunning()).isEqualTo(UploadTaskStep.RUNNING_INSPECT);
        assertThat(UploadTaskStep.PENDING_OSS.toRunning()).isEqualTo(UploadTaskStep.RUNNING_OSS);
        assertThat(UploadTaskStep.PENDING_METADATA.toRunning()).isEqualTo(UploadTaskStep.RUNNING_METADATA);
        assertThat(UploadTaskStep.PENDING_CLEANUP.toRunning()).isEqualTo(UploadTaskStep.RUNNING_CLEANUP);
    }

    @Test
    @DisplayName("UploadTaskStep.nextPending 应正确推进")
    void stepNextPending_shouldAdvanceCorrectly() {
        assertThat(UploadTaskStep.RUNNING_MERGE.nextPending()).isEqualTo(UploadTaskStep.PENDING_INSPECT);
        assertThat(UploadTaskStep.RUNNING_INSPECT.nextPending()).isEqualTo(UploadTaskStep.PENDING_OSS);
        assertThat(UploadTaskStep.RUNNING_OSS.nextPending()).isEqualTo(UploadTaskStep.PENDING_METADATA);
        assertThat(UploadTaskStep.RUNNING_METADATA.nextPending()).isEqualTo(UploadTaskStep.PENDING_CLEANUP);
        assertThat(UploadTaskStep.RUNNING_CLEANUP.nextPending()).isEqualTo(UploadTaskStep.DONE);
    }

    @Test
    @DisplayName("UploadTaskStep.isPending 和 isRunning 应正确判断")
    void stepPredicates_shouldReturnCorrectly() {
        assertThat(UploadTaskStep.PENDING_MERGE.isPending()).isTrue();
        assertThat(UploadTaskStep.PENDING_MERGE.isRunning()).isFalse();
        assertThat(UploadTaskStep.RUNNING_MERGE.isPending()).isFalse();
        assertThat(UploadTaskStep.RUNNING_MERGE.isRunning()).isTrue();
        assertThat(UploadTaskStep.DONE.isPending()).isFalse();
        assertThat(UploadTaskStep.DONE.isRunning()).isFalse();
    }

    // ==================== Helper Methods ====================

    private void simulateClaim(UploadTask task) {
        // claim SQL 中 attempts=attempts+1，这里通过反射模拟
        setAttempts(task, 1);
    }

    private void setAttempts(UploadTask task, int attempts) {
        try {
            var field = UploadTask.class.getDeclaredField("attempts");
            field.setAccessible(true);
            field.setInt(task, attempts);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
