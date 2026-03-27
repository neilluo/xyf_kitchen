package com.grace.platform.shared.domain;

import java.util.List;

/**
 * 分页查询结果领域对象。
 * <p>
 * 用于仓储层返回分页查询结果，不依赖任何框架。
 *
 * @param <T> 数据项类型
 * @author Grace Platform Team
 * @since 1.0.0
 */
public record PaginatedResult<T>(
    List<T> items,
    long total,
    int page,
    int pageSize,
    int totalPages
) {

    /**
     * 创建分页结果。
     *
     * @param items    当前页数据项
     * @param total    总记录数
     * @param page     当前页码（从 0 开始）
     * @param pageSize 每页大小
     * @param <T>      数据项类型
     * @return 分页结果
     */
    public static <T> PaginatedResult<T> of(List<T> items, long total, int page, int pageSize) {
        int totalPages = pageSize > 0 ? (int) Math.ceil((double) total / pageSize) : 0;
        return new PaginatedResult<>(items, total, page, pageSize, totalPages);
    }

    /**
     * 创建空分页结果。
     *
     * @param page     当前页码
     * @param pageSize 每页大小
     * @param <T>      数据项类型
     * @return 空分页结果
     */
    public static <T> PaginatedResult<T> empty(int page, int pageSize) {
        return new PaginatedResult<>(List.of(), 0, page, pageSize, 0);
    }
}
