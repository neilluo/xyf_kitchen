package com.grace.platform.distribution.domain;

import com.grace.platform.shared.domain.id.PublishRecordId;
import com.grace.platform.shared.domain.id.VideoId;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 发布记录仓储接口
 * <p>
 * 定义 PublishRecord 聚合根的持久化操作，由基础设施层实现。
 * </p>
 */
public interface PublishRecordRepository {

    /**
     * 保存发布记录（新增或更新）
     *
     * @param record 发布记录
     * @return 保存后的发布记录
     */
    PublishRecord save(PublishRecord record);

    /**
     * 根据 ID 查询发布记录
     *
     * @param id 发布记录 ID
     * @return 发布记录 Optional 包装
     */
    Optional<PublishRecord> findById(PublishRecordId id);

    /**
     * 根据视频 ID 查询所有发布记录
     *
     * @param videoId 视频 ID
     * @return 发布记录列表
     */
    List<PublishRecord> findByVideoId(VideoId videoId);

    /**
     * 根据上传任务 ID 查询发布记录
     *
     * @param uploadTaskId 平台上传任务 ID
     * @return 发布记录 Optional 包装
     */
    Optional<PublishRecord> findByUploadTaskId(String uploadTaskId);

    /**
     * 根据状态查询发布记录
     *
     * @param status 发布状态
     * @return 发布记录列表
     */
    List<PublishRecord> findByStatus(PublishStatus status);

    /**
     * 按状态分组统计发布记录数量
     *
     * @return 状态到数量的映射
     */
    Map<PublishStatus, Long> countGroupByStatus();
}
