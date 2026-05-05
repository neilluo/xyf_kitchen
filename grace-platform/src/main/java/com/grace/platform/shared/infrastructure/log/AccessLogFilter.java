package com.grace.platform.shared.infrastructure.log;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 紧跟 {@link EagleEyeFilter} 之后执行，写 access.log。
 * 每次 HTTP 请求产出一行 pipe 分隔摘要：method|uri|status|rt|userIp|userAgent。
 * 健康检查端点可在此扩展排除。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class AccessLogFilter extends OncePerRequestFilter {

    private static final Logger ACCESS = LoggerFactory.getLogger("ACCESS_LOG");

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse resp,
                                    FilterChain chain) throws ServletException, IOException {
        // 健康检查不落 access 日志（避免噪音）
        String uri = req.getRequestURI();
        if (uri != null && uri.equals("/api/health")) {
            chain.doFilter(req, resp);
            return;
        }

        long start = System.currentTimeMillis();
        int status = 200;
        try {
            chain.doFilter(req, resp);
            status = resp.getStatus();
        } catch (Exception e) {
            status = 500;
            throw e;
        } finally {
            try {
                long rt = System.currentTimeMillis() - start;
                ACCESS.info("{}|{}|{}|{}|{}|{}",
                        safe(req.getMethod()),
                        safe(req.getRequestURI()),
                        status,
                        rt,
                        safe(req.getRemoteAddr()),
                        safe(req.getHeader("User-Agent")));
            } catch (Exception ignored) {
                // 日志写失败不影响响应
            }
        }
    }

    private String safe(String s) {
        if (s == null) {
            return "-";
        }
        return s.replace("|", "\\|");
    }
}
