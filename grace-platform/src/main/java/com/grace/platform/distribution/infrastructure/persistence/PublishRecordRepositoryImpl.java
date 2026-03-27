package com.grace.platform.distribution.infrastructure.persistence;

import com.grace.platform.distribution.domain.PublishRecord;
import com.grace.platform.distribution.domain.PublishRecordRepository;
import com.grace.platform.distribution.domain.PublishStatus;
import com.grace.platform.shared.domain.id.PublishRecordId;
import com.grace.platform.shared.domain.id.VideoId;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * PublishRecord 仓储实现类。
 * <p>
 * 基于 MyBatis 实现 PublishRecord 聚合根的持久化操作。
 * MyBatis 直接映射到领域对象，无需 Entity ↔ Domain 转换。
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
@Repository
public class PublishRecordRepositoryImpl implements PublishRecordRepository {

    private final PublishRecordMapper publishRecordMapper;

    public PublishRecordRepositoryImpl(PublishRecordMapper publishRecordMapper) {
        this.publishRecordMapper = publishRecordMapper;
    }

    @Override
    public PublishRecord save(PublishRecord record) {
        if (record.getId() == null) {
            // 新增
            publishRecordMapper.insert(record);
        } else {
            // 更新
            publishRecordMapper.update(record);
        }
        return record;
    }

    @Override
    public Optional<PublishRecord> findById(PublishRecordId id) {
        PublishRecord record = publishRecordMapper.findById(id.value());
        return Optional.ofNullable(record);
    }

    @Override
    public List<PublishRecord> findByVideoId(VideoId videoId) {
        return publishRecordMapper.findByVideoId(videoId.value());
    }

    @Override
    public Optional<PublishRecord> findByUploadTaskId(String uploadTaskId) {
        PublishRecord record = publishRecordMapper.findByUploadTaskId(uploadTaskId);
        return Optional.ofNullable(record);
    }

    @Override
    public List<PublishRecord> findByStatus(PublishStatus status) {
        return publishRecordMapper.findByStatus(status.name());
    }

    @Override
    public Map<PublishStatus, Long> countGroupByStatus() {
        // 获取所有状态值
        List<String> allStatuses = java.util.Arrays.stream(PublishStatus.values())
                .map(PublishStatus::name)
                .collect(Collectors.toList());

        Map<PublishStatus, Long> result = new HashMap<>();

        // 初始化所有状态为 0
        for (PublishStatus status : PublishStatus.values()) {
            result.put(status, 0L);
        }

        // 批量查询各状态数量
        List<String> statusNames = publishRecordMapper.countByStatusIn(allStatuses) > 0
                ? allStatuses
                : java.util.Collections.emptyList();

        // 逐个查询各状态数量
        for (PublishStatus status : PublishStatus.values()) {
            long count = publishRecordMapper.countByStatus(status.name());
            result.put(status, count);
        }

        return result;
    }
}
