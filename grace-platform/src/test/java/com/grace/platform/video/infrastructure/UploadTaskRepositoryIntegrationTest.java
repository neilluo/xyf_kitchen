package com.grace.platform.video.infrastructure;

import com.grace.platform.GracePlatformApplication;
import com.grace.platform.video.domain.UploadTask;
import com.grace.platform.video.domain.UploadTaskRepository;
import com.grace.platform.video.domain.UploadTaskStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = GracePlatformApplication.class)
@ActiveProfiles("test")
class UploadTaskRepositoryIntegrationTest {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:mysql://localhost:13306/grace_test?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true");
        registry.add("spring.datasource.username", () -> "test");
        registry.add("spring.datasource.password", () -> "test");
    }

    @Autowired
    private UploadTaskRepository uploadTaskRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("save → findById 能读回所有字段（包括 payload、extInfo Map）")
    void saveAndFindByIdRoundTrip() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("fileName", "test.mp4");
        payload.put("fileSize", 1024);

        Map<String, Object> extInfo = new LinkedHashMap<>();
        extInfo.put("traceId", "trace-123");

        UploadTask task = UploadTask.createInitial("vid_001", payload, extInfo);
        uploadTaskRepository.save(task);

        Optional<UploadTask> found = uploadTaskRepository.findById(task.getId());

        assertThat(found).isPresent();
        UploadTask t = found.get();
        assertThat(t.getId()).isEqualTo(task.getId());
        assertThat(t.getVideoId()).isEqualTo("vid_001");
        assertThat(t.getCurrentStep()).isEqualTo(task.getCurrentStep());
        assertThat(t.getStatus()).isEqualTo(UploadTaskStatus.PENDING);
        assertThat(t.getAttempts()).isEqualTo(0);
        assertThat(t.getMaxAttempts()).isEqualTo(5);
        assertThat(t.getPayload()).containsEntry("fileName", "test.mp4");
        assertThat(t.getExtInfo()).containsEntry("traceId", "trace-123");
    }

    @Test
    @DisplayName("claim 首次成功返回 1；重复 claim 返回 0")
    void claimIsAtomic() {
        UploadTask task = UploadTask.createInitial("vid_002", null, null);
        uploadTaskRepository.save(task);

        // 首次 claim 成功
        int first = uploadTaskRepository.claim(task.getId(), "instance-1");
        assertThat(first).isEqualTo(1);

        // 验证状态已变更
        UploadTask claimed = uploadTaskRepository.findById(task.getId()).orElseThrow();
        assertThat(claimed.getStatus()).isEqualTo(UploadTaskStatus.RUNNING);
        assertThat(claimed.getAttempts()).isEqualTo(1);

        // 重复 claim 失败
        int second = uploadTaskRepository.claim(task.getId(), "instance-2");
        assertThat(second).isEqualTo(0);
    }

    @Test
    @DisplayName("resetZombies 将 heartbeat 超时的 RUNNING 行重置为 PENDING")
    void resetZombiesResetsStaleRunningTasks() {
        UploadTask task = UploadTask.createInitial("vid_003", null, null);
        uploadTaskRepository.save(task);

        // 先 claim 使其变为 RUNNING
        uploadTaskRepository.claim(task.getId(), "instance-1");

        // 用 JdbcTemplate 将 heartbeat_at 设为 3 分钟前
        jdbcTemplate.update(
            "UPDATE upload_task SET heartbeat_at = DATE_SUB(NOW(), INTERVAL 180 SECOND) WHERE id = ?",
            task.getId()
        );

        // resetZombies 阈值 120 秒应命中
        int affected = uploadTaskRepository.resetZombies(120);
        assertThat(affected).isGreaterThanOrEqualTo(1);

        // 验证状态已重置
        UploadTask reset = uploadTaskRepository.findById(task.getId()).orElseThrow();
        assertThat(reset.getStatus()).isEqualTo(UploadTaskStatus.PENDING);
        assertThat(reset.getOwnerInstanceId()).isNull();
    }

    @Test
    @DisplayName("findOrphanPendingIds 能找到 updated_at 在阈值外的 PENDING 行")
    void findOrphanPendingIdsDetectsStalePending() {
        UploadTask task = UploadTask.createInitial("vid_004", null, null);
        uploadTaskRepository.save(task);

        // 用 JdbcTemplate 将 updated_at 设为 2 分钟前
        jdbcTemplate.update(
            "UPDATE upload_task SET updated_at = DATE_SUB(NOW(), INTERVAL 120 SECOND) WHERE id = ?",
            task.getId()
        );

        // 阈值 60 秒应命中
        List<String> orphans = uploadTaskRepository.findOrphanPendingIds(60);
        assertThat(orphans).contains(task.getId());

        // 阈值 300 秒不应命中
        List<String> notOrphans = uploadTaskRepository.findOrphanPendingIds(300);
        assertThat(notOrphans).doesNotContain(task.getId());
    }
}
