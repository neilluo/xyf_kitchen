package com.grace.platform.video.infrastructure.persistence;

import com.grace.platform.video.domain.UploadTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * UploadTask 数据访问 Mapper 接口。
 * <p>
 * 定义 UploadTask 聚合根的数据库操作，由 MyBatis 实现。
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
@Mapper
public interface UploadTaskMapper {

    /**
     * 插入新上传任务。
     *
     * @param task 上传任务对象
     */
    void insert(UploadTask task);

    /**
     * 更新上传任务信息。
     *
     * @param task 上传任务对象
     */
    void update(UploadTask task);

    /**
     * 根据 ID 查询上传任务。
     *
     * @param id 任务 ID
     * @return 上传任务对象，未找到返回 null
     */
    UploadTask findById(@Param("id") String id);

    /**
     * 根据视频 ID 查询上传任务。
     *
     * @param videoId 视频 ID
     * @return 上传任务对象，未找到返回 null
     */
    UploadTask findByVideoId(@Param("videoId") String videoId);

    /**
     * 原子 claim：status PENDING→RUNNING + attempts++。
     *
     * @param id         任务 ID
     * @param instanceId 执行实例 ID
     * @return 影响行数
     */
    int claim(@Param("id") String id, @Param("instanceId") String instanceId);

    /**
     * 心跳更新。
     *
     * @param id 任务 ID
     * @return 影响行数
     */
    int touchHeartbeat(@Param("id") String id);

    /**
     * 扫描 PENDING 但超过 orphanSeconds 未被消费的任务 ID。
     *
     * @param orphanSeconds 孤儿阈值（秒）
     * @return 孤儿任务 ID 列表
     */
    List<String> findOrphanPendingIds(@Param("orphanSeconds") int orphanSeconds);

    /**
     * 扫描 RUNNING 但 heartbeat 超过 zombieSeconds 的任务，重置为 PENDING。
     *
     * @param zombieSeconds 僵尸阈值（秒）
     * @return 受影响行数
     */
    int resetZombies(@Param("zombieSeconds") int zombieSeconds);
}
