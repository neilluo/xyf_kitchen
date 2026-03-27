package com.grace.platform.video.application.command;

import com.grace.platform.video.domain.VideoStatus;

import java.time.LocalDate;
import java.util.List;

/**
 * 视频列表查询命令。
 * <p>
 * 用于 {@code listVideos} 方法的参数封装。
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
public record VideoQueryCommand(
    String keyword,
    List<VideoStatus> statuses,
    LocalDate startDate,
    LocalDate endDate,
    int page,
    int pageSize,
    String sort,
    String order
) {
    public VideoQueryCommand {
        if (page < 1) {
            page = 1;
        }
        if (pageSize < 1) {
            pageSize = 20;
        }
        if (pageSize > 100) {
            pageSize = 100;
        }
        if (sort == null || sort.isBlank()) {
            sort = "createdAt";
        }
        if (order == null || order.isBlank()) {
            order = "desc";
        }
    }
}
