package com.grace.platform.shared.domain;

/**
 * 分页请求参数领域对象。
 * <p>
 * 用于向仓储层传递分页参数，不依赖任何框架。
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
public record PageRequest(
    int page,
    int pageSize
) {

    /**
     * 默认分页大小。
     */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /**
     * 最大分页大小。
     */
    public static final int MAX_PAGE_SIZE = 100;

    /**
     * 创建分页请求，使用默认页大小。
     *
     * @param page 页码（从 0 开始）
     * @return 分页请求
     */
    public static PageRequest of(int page) {
        return new PageRequest(normalizePage(page), DEFAULT_PAGE_SIZE);
    }

    /**
     * 创建分页请求。
     *
     * @param page     页码（从 0 开始）
     * @param pageSize 页大小
     * @return 分页请求
     */
    public static PageRequest of(int page, int pageSize) {
        return new PageRequest(normalizePage(page), normalizePageSize(pageSize));
    }

    private static int normalizePage(int page) {
        return Math.max(0, page);
    }

    private static int normalizePageSize(int pageSize) {
        if (pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    /**
     * 计算偏移量。
     *
     * @return 数据库查询偏移量
     */
    public int offset() {
        return page * pageSize;
    }
}
