package com.grace.platform.video.domain;

import java.util.List;
import java.util.Optional;

/**
 * 上传任务仓储接口。
 * <p>
 * 定义 UploadTask 聚合根的持久化操作，由基础设施层实现。
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
public interface UploadTaskRepository {

    /**
     * 保存上传任务（新增或更新）。
     *
     * @param task 上传任务聚合根
     * @return 保存后的上传任务
     */
    UploadTask save(UploadTask task);

    /**
     * 根据 ID 查询上传任务。
     *
     * @param id 任务 ID
     * @return 上传任务 Optional 包装
     */
    Optional<UploadTask> findById(String id);

    /**
     * 根据视频 ID 查询上传任务。
     *
     * @param videoId 视频 ID
     * @return 上传任务 Optional 包装
     */
    Optional<UploadTask> findByVideoId(String videoId);

    /**
     * 原子 claim：status PENDING→RUNNING + attempts++。
     *
     * @param id         任务 ID
     * @param instanceId 执行实例 ID
     * @return 影响行数，1 = 成功
     */
    int claim(String id, String instanceId);

    /**
     * 心跳更新。
     *
     * @param id 任务 ID
     * @return 影响行数
     */
    int touchHeartbeat(String id);

    /**
     * 扫描 PENDING 但超过 orphanSeconds 未被消费的任务 ID。
     *
     * @param orphanSeconds 孤儿阈值（秒）
     * @return 孤儿任务 ID 列表
     */
    List<String> findOrphanPendingIds(int orphanSeconds);

    /**
     * 扫描 RUNNING 但 heartbeat 超过 zombieSeconds 的任务，重置为 PENDING。
     *
     * @param zombieSeconds 僵尸阈值（秒）
     * @return 受影响行数
     */
    int resetZombies(int zombieSeconds);
}
