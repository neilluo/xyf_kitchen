package com.grace.platform.video.domain;

import com.grace.platform.shared.domain.PageRequest;
import com.grace.platform.shared.domain.PaginatedResult;
import com.grace.platform.shared.domain.id.VideoId;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 视频仓储接口。
 * <p>
 * 定义 Video 聚合根的持久化操作，由基础设施层实现。
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
public interface VideoRepository {

    /**
     * 保存视频（新增或更新）。
     *
     * @param video 视频聚合根
     * @return 保存后的视频
     */
    Video save(Video video);

    /**
     * 根据 ID 查询视频。
     *
     * @param id 视频 ID
     * @return 视频 Optional 包装
     */
    Optional<Video> findById(VideoId id);

    /**
     * 条件分页查询视频列表。
     *
     * @param keyword     关键词（模糊匹配文件名）
     * @param statuses    状态列表（可为 null 表示不限）
     * @param startDate   开始日期（可为 null）
     * @param endDate     结束日期（可为 null）
     * @param pageRequest 分页请求参数
     * @return 视频分页结果
     */
    PaginatedResult<Video> findByCondition(String keyword, List<VideoStatus> statuses,
                                           LocalDate startDate, LocalDate endDate, PageRequest pageRequest);

    /**
     * 统计视频总数。
     *
     * @return 视频总数
     */
    long count();

    /**
     * 按状态列表统计视频数量。
     *
     * @param statuses 状态列表
     * @return 符合条件的视频数量
     */
    long countByStatuses(List<VideoStatus> statuses);

    /**
     * 查询最近创建的 5 条视频。
     *
     * @return 视频列表（按创建时间降序）
     */
    List<Video> findTop5ByOrderByCreatedAtDesc();
}
