package com.grace.platform.video.infrastructure.persistence;

import com.grace.platform.video.domain.UploadSession;
import com.grace.platform.video.domain.UploadSessionRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * UploadSession 仓储实现类。
 * <p>
 * 基于 MyBatis 实现 UploadSession 实体的持久化操作。
 * MyBatis 直接映射到领域对象，无需 Entity ↔ Domain 转换。
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
@Repository
public class UploadSessionRepositoryImpl implements UploadSessionRepository {

    private final UploadSessionMapper uploadSessionMapper;

    public UploadSessionRepositoryImpl(UploadSessionMapper uploadSessionMapper) {
        this.uploadSessionMapper = uploadSessionMapper;
    }

    @Override
    public UploadSession save(UploadSession session) {
        // 查询是否存在
        UploadSession existing = uploadSessionMapper.findById(session.getUploadId());
        if (existing == null) {
            // 新增
            uploadSessionMapper.insert(session);
        } else {
            // 更新
            uploadSessionMapper.update(session);
        }
        return session;
    }

    @Override
    public Optional<UploadSession> findById(String uploadId) {
        UploadSession session = uploadSessionMapper.findById(uploadId);
        return Optional.ofNullable(session);
    }

    @Override
    public void deleteExpiredSessions() {
        uploadSessionMapper.deleteExpiredSessions(LocalDateTime.now());
    }
}
