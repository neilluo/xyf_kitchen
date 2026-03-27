package com.grace.platform.video.infrastructure.persistence;

import com.grace.platform.shared.domain.PageRequest;
import com.grace.platform.shared.domain.PaginatedResult;
import com.grace.platform.shared.domain.id.VideoId;
import com.grace.platform.video.domain.Video;
import com.grace.platform.video.domain.VideoRepository;
import com.grace.platform.video.domain.VideoStatus;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Video 仓储实现类。
 * <p>
 * 基于 MyBatis 实现 Video 聚合根的持久化操作。
 * MyBatis 直接映射到领域对象，无需 Entity ↔ Domain 转换。
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
@Repository
public class VideoRepositoryImpl implements VideoRepository {

    private final VideoMapper videoMapper;

    public VideoRepositoryImpl(VideoMapper videoMapper) {
        this.videoMapper = videoMapper;
    }

    @Override
    public Video save(Video video) {
        if (video.getId() == null) {
            // 新增
            videoMapper.insert(video);
        } else {
            // 更新
            videoMapper.update(video);
        }
        return video;
    }

    @Override
    public Optional<Video> findById(VideoId id) {
        Video video = videoMapper.findById(id.value());
        return Optional.ofNullable(video);
    }

    @Override
    public PaginatedResult<Video> findByCondition(String keyword, List<VideoStatus> statuses,
                                                   LocalDate startDate, LocalDate endDate, PageRequest pageRequest) {
        // 转换状态列表为字符串（取第一个状态用于单状态查询，多状态时需要在 XML 中处理）
        String status = null;
        if (statuses != null && !statuses.isEmpty() && statuses.size() == 1) {
            status = statuses.get(0).name();
        }

        // 计算分页参数
        int offset = pageRequest.page() * pageRequest.pageSize();
        int limit = pageRequest.pageSize();

        // 查询列表
        List<Video> videos = videoMapper.findByCondition(status, keyword, startDate, endDate, offset, limit);

        // 查询总数
        long total = videoMapper.countByCondition(status, keyword, startDate, endDate);

        return PaginatedResult.of(videos, total, pageRequest.page(), pageRequest.pageSize());
    }

    @Override
    public long count() {
        return videoMapper.countByCondition(null, null, null, null);
    }

    @Override
    public long countByStatuses(List<VideoStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return 0;
        }
        List<String> statusStrings = statuses.stream()
            .map(VideoStatus::name)
            .collect(Collectors.toList());
        return videoMapper.countByStatusIn(statusStrings);
    }

    @Override
    public List<Video> findTop5ByOrderByCreatedAtDesc() {
        return videoMapper.findTop5ByCreatedAtDesc();
    }
}
