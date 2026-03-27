package com.grace.platform.usersettings.infrastructure.acl;

import com.grace.platform.usersettings.application.dto.ConnectedAccountResponse;

import java.util.List;

/**
 * 已连接账户查询服务（跨上下文 ACL）
 * <p>
 * 只读查询 Distribution 上下文的 OAuthToken 数据，返回已连接账户信息。
 * </p>
 */
public interface ConnectedAccountQueryService {

    /**
     * 查询已连接账户列表
     *
     * @return 已连接账户列表
     */
    List<ConnectedAccountResponse> queryConnectedAccounts();

    /**
     * 断开平台连接
     *
     * @param platform 平台标识
     */
    void disconnectPlatform(String platform);
}
