package com.grace.platform.video.infrastructure.persistence;

import com.grace.platform.video.domain.UploadSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * UploadSession 数据访问 Mapper 接口。
 * <p>
 * 定义上传会话实体的数据库操作，由 MyBatis 实现。
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
@Mapper
public interface UploadSessionMapper {

    /**
     * 根据 uploadId 查询上传会话。
     *
     * @param uploadId 上传会话 ID
     * @return 上传会话对象，未找到返回 null
     */
    UploadSession findById(@Param("uploadId") String uploadId);

    /**
     * 插入新上传会话。
     *
     * @param session 上传会话对象
     */
    void insert(UploadSession session);

    /**
     * 更新上传会话信息。
     *
     * @param session 上传会话对象
     */
    void update(UploadSession session);

    /**
     * 删除所有已过期的上传会话。
     *
     * @param now 当前时间
     * @return 删除的记录数
     */
    int deleteExpiredSessions(@Param("now") LocalDateTime now);
}
