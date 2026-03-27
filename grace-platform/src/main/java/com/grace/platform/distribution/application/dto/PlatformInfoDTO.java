package com.grace.platform.distribution.application.dto;

/**
 * 平台信息 DTO
 * <p>
 * 对应 API D5 响应的 data 字段数组元素。
 * 包含平台元数据和 OAuth 授权状态。
 * </p>
 *
 * @param platform    平台标识（如 "youtube"）
 * @param displayName 显示名称（如 "YouTube"）
 * @param authorized  是否已完成 OAuth 授权
 * @param authExpired 授权是否过期
 * @param enabled     平台是否可用
 */
public record PlatformInfoDTO(
    String platform,
    String displayName,
    boolean authorized,
    boolean authExpired,
    boolean enabled
) {
}
