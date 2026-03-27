package com.grace.platform.video.infrastructure.persistence;

import com.grace.platform.video.domain.Video;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * Video 数据访问 Mapper 接口。
 * <p>
 * 定义 Video 聚合根的数据库操作，由 MyBatis 实现。
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
@Mapper
public interface VideoMapper {

    /**
     * 根据 ID 查询视频。
     *
     * @param id 视频 ID
     * @return 视频对象，未找到返回 null
     */
    Video findById(@Param("id") String id);

    /**
     * 条件分页查询视频列表。
     *
     * @param status 状态（可为 null 表示不限）
     * @param keyword 关键词（模糊匹配文件名）
     * @param startDate 开始日期（可为 null）
     * @param endDate 结束日期（可为 null）
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 视频列表
     */
    List<Video> findByCondition(@Param("status") String status,
                                @Param("keyword") String keyword,
                                @Param("startDate") LocalDate startDate,
                                @Param("endDate") LocalDate endDate,
                                @Param("offset") int offset,
                                @Param("limit") int limit);

    /**
     * 统计符合条件的视频数量。
     *
     * @param status 状态（可为 null 表示不限）
     * @param keyword 关键词（模糊匹配文件名）
     * @param startDate 开始日期（可为 null）
     * @param endDate 结束日期（可为 null）
     * @return 视频数量
     */
    long countByCondition(@Param("status") String status,
                          @Param("keyword") String keyword,
                          @Param("startDate") LocalDate startDate,
                          @Param("endDate") LocalDate endDate);

    /**
     * 按状态列表统计视频数量。
     *
     * @param statuses 状态列表
     * @return 符合条件的视频数量
     */
    long countByStatusIn(@Param("statuses") List<String> statuses);

    /**
     * 查询最近创建的 5 条视频。
     *
     * @return 视频列表（按创建时间降序）
     */
    List<Video> findTop5ByCreatedAtDesc();

    /**
     * 插入新视频。
     *
     * @param video 视频对象
     */
    void insert(Video video);

    /**
     * 更新视频信息。
     *
     * @param video 视频对象
     */
    void update(Video video);
}
