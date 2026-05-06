package com.grace.platform.video.infrastructure.persistence;

import com.grace.platform.video.domain.UploadTask;
import com.grace.platform.video.domain.UploadTaskRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * UploadTask 仓储实现类。
 * <p>
 * 基于 MyBatis 实现 UploadTask 聚合根的持久化操作。
 * MyBatis 直接映射到领域对象，无需 Entity ↔ Domain 转换。
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
@Repository
public class UploadTaskRepositoryImpl implements UploadTaskRepository {

    private final UploadTaskMapper uploadTaskMapper;

    public UploadTaskRepositoryImpl(UploadTaskMapper uploadTaskMapper) {
        this.uploadTaskMapper = uploadTaskMapper;
    }

    @Override
    public UploadTask save(UploadTask task) {
        // 判断是否是新任务：createdAt 等于 updatedAt 表示是新创建的任务
        // 因为 UploadTask.createInitial() 预生成 ID，不能通过 id == null 判断
        boolean isNew = task.getCreatedAt().equals(task.getUpdatedAt());
        if (isNew) {
            uploadTaskMapper.insert(task);
        } else {
            uploadTaskMapper.update(task);
        }
        return task;
    }

    @Override
    public Optional<UploadTask> findById(String id) {
        UploadTask task = uploadTaskMapper.findById(id);
        return Optional.ofNullable(task);
    }

    @Override
    public Optional<UploadTask> findByVideoId(String videoId) {
        UploadTask task = uploadTaskMapper.findByVideoId(videoId);
        return Optional.ofNullable(task);
    }

    @Override
    public int claim(String id, String instanceId) {
        return uploadTaskMapper.claim(id, instanceId);
    }

    @Override
    public int touchHeartbeat(String id) {
        return uploadTaskMapper.touchHeartbeat(id);
    }

    @Override
    public List<String> findOrphanPendingIds(int orphanSeconds) {
        return uploadTaskMapper.findOrphanPendingIds(orphanSeconds);
    }

    @Override
    public int resetZombies(int zombieSeconds) {
        return uploadTaskMapper.resetZombies(zombieSeconds);
    }
}
