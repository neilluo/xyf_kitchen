package com.grace.platform.usersettings.infrastructure.acl;

import com.grace.platform.distribution.domain.OAuthToken;
import com.grace.platform.distribution.domain.OAuthTokenRepository;
import com.grace.platform.usersettings.application.dto.ConnectedAccountResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 已连接账户查询服务实现（跨上下文 ACL）
 * <p>
 * 只读查询 Distribution 上下文的 OAuthToken 数据，返回已连接账户信息。
 * 通过 ACL 模式隔离上下文边界，User & Settings 上下文不直接依赖 Distribution 的领域模型。
 * </p>
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
@Service
public class ConnectedAccountQueryServiceImpl implements ConnectedAccountQueryService {

    private final OAuthTokenRepository oAuthTokenRepository;

    // 已知平台列表（可配置化）
    private static final List<PlatformInfo> KNOWN_PLATFORMS = List.of(
        new PlatformInfo("youtube", "YouTube"),
        new PlatformInfo("weibo", "Weibo"),
        new PlatformInfo("bilibili", "Bilibili")
    );

    public ConnectedAccountQueryServiceImpl(OAuthTokenRepository oAuthTokenRepository) {
        this.oAuthTokenRepository = oAuthTokenRepository;
    }

    /**
     * 查询已连接账户列表
     * <p>
     * 策略：遍历已知平台，检查 OAuthToken 是否存在且未过期
     * </p>
     *
     * @return 已连接账户列表
     */
    @Override
    public List<ConnectedAccountResponse> queryConnectedAccounts() {
        List<OAuthToken> tokens = oAuthTokenRepository.findAll();
        Map<String, OAuthToken> tokenMap = tokens.stream()
            .collect(Collectors.toMap(OAuthToken::getPlatform, Function.identity()));

        return KNOWN_PLATFORMS.stream()
            .map(platform -> {
                OAuthToken token = tokenMap.get(platform.id());
                boolean authorized = token != null && !token.isExpired();
                return new ConnectedAccountResponse(
                    platform.id(),
                    platform.displayName(),
                    authorized,
                    authorized ? token.getPlatform() : null,
                    authorized ? token.getCreatedAt().toString() : null
                );
            })
            .toList();
    }

    /**
     * 断开平台连接 — 删除对应的 OAuthToken
     *
     * @param platform 平台标识
     */
    @Override
    public void disconnectPlatform(String platform) {
        oAuthTokenRepository.findByPlatform(platform)
            .ifPresent(token -> oAuthTokenRepository.deleteByPlatform(platform));
    }

    /**
     * 平台信息内部记录
     */
    private record PlatformInfo(String id, String displayName) {}
}
