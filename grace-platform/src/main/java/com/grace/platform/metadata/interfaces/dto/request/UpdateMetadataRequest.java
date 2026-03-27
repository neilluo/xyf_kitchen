package com.grace.platform.metadata.interfaces.dto.request;

import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 更新元数据请求
 * <p>
 * 用于用户编辑元数据的请求体。
 * 对应 API C2 的请求字段。
 * 所有字段均为可选，null 表示不更新该字段。
 * </p>
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
public record UpdateMetadataRequest(
        @Size(max = 100, message = "标题不能超过100字符")
        String title,

        @Size(max = 5000, message = "描述不能超过5000字符")
        String description,

        @Size(min = 5, max = 15, message = "标签数量需在5-15之间")
        List<String> tags
) {
    /**
     * 创建更新元数据请求
     *
     * @param title       新标题，可选，≤ 100 字符
     * @param description 新描述，可选，≤ 5000 字符
     * @param tags        新标签列表，可选，5-15 个
     */
    public UpdateMetadataRequest {
        // Record compact constructor - validation happens in domain layer
    }
}
