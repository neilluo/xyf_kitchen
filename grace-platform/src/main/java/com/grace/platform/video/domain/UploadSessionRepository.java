package com.grace.platform.video.domain;

import java.util.Optional;

/**
 * 上传会话仓储接口。
 * <p>
 * 定义 UploadSession 实体的持久化操作，由基础设施层实现。
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
public interface UploadSessionRepository {

    /**
     * 保存上传会话（新增或更新）。
     *
     * @param session 上传会话实体
     * @return 保存后的会话
     */
    UploadSession save(UploadSession session);

    /**
     * 根据 uploadId 查询上传会话。
     *
     * @param uploadId 上传会话 ID
     * @return 会话 Optional 包装
     */
    Optional<UploadSession> findById(String uploadId);

    /**
     * 删除所有已过期的上传会话。
     * <p>
     * 可由定时任务调用清理过期数据。
     */
    void deleteExpiredSessions();
}
