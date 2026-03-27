package com.grace.platform.metadata.application.command;

import java.util.List;

/**
 * 更新元数据命令
 * <p>
 * 用于封装用户编辑元数据的请求参数。
 * 对应 API C2 的请求体字段。
 * </p>
 */
public record UpdateMetadataCommand(
        String title,
        String description,
        List<String> tags
) {
    /**
     * 创建更新元数据命令
     * <p>
     * 所有字段均为可选，null 表示不更新该字段。
     * </p>
     *
     * @param title       新标题，可选，≤ 100 字符
     * @param description 新描述，可选，≤ 5000 字符
     * @param tags        新标签列表，可选，5-15 个
     */
    public UpdateMetadataCommand {
        // Record compact constructor - validation happens in domain layer
    }
}
