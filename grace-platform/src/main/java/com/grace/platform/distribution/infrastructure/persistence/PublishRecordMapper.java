package com.grace.platform.distribution.infrastructure.persistence;

import com.grace.platform.distribution.domain.PublishRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * PublishRecord 数据访问 Mapper 接口。
 * <p>
 * 定义 PublishRecord 聚合根的数据库操作，由 MyBatis 实现。
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
@Mapper
public interface PublishRecordMapper {

    /**
     * 根据 ID 查询发布记录。
     *
     * @param id 发布记录 ID
     * @return 发布记录对象，未找到返回 null
     */
    PublishRecord findById(@Param("id") String id);

    /**
     * 根据视频 ID 查询所有发布记录。
     *
     * @param videoId 视频 ID
     * @return 发布记录列表
     */
    List<PublishRecord> findByVideoId(@Param("videoId") String videoId);

    /**
     * 根据上传任务 ID 查询发布记录。
     *
     * @param uploadTaskId 上传任务 ID
     * @return 发布记录对象，未找到返回 null
     */
    PublishRecord findByUploadTaskId(@Param("uploadTaskId") String uploadTaskId);

    /**
     * 根据状态查询发布记录。
     *
     * @param status 发布状态
     * @return 发布记录列表
     */
    List<PublishRecord> findByStatus(@Param("status") String status);

    /**
     * 根据状态统计发布记录数量。
     *
     * @param status 发布状态
     * @return 发布记录数量
     */
    long countByStatus(@Param("status") String status);

    /**
     * 根据状态列表统计发布记录数量。
     *
     * @param statuses 状态列表
     * @return 发布记录数量
     */
    long countByStatusIn(@Param("statuses") List<String> statuses);

    /**
     * 插入新发布记录。
     *
     * @param record 发布记录对象
     */
    void insert(PublishRecord record);

    /**
     * 更新发布记录信息。
     *
     * @param record 发布记录对象
     */
    void update(PublishRecord record);
}
