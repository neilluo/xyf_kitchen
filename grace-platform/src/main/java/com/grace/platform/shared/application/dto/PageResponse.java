package com.grace.platform.shared.application.dto;

import java.util.List;

public record PageResponse<T>(
    List<T> items,
    long total,
    int page,
    int pageSize,
    int totalPages
) {
    /**
     * 手动分页构造：传入当前页数据、总记录数、页码和每页条数
     */
    public static <T> PageResponse<T> of(List<T> items, long total, int page, int pageSize) {
        int totalPages = (int) Math.ceil((double) total / pageSize);
        return new PageResponse<>(items, total, page, pageSize, totalPages);
    }
}
