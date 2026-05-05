package com.grace.platform.shared.infrastructure.log;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * HTTP 入口 trace 建立 Filter。必须在所有其他 Filter 之前执行。
 * 任何日志模块异常都不阻断请求。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class EagleEyeFilter extends OncePerRequestFilter {

    @Value("${spring.application.name:grace-platform}")
    private String appName;

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse resp,
                                    FilterChain chain) throws ServletException, IOException {
        String tid = "";
        try {
            tid = TraceContext.startTrace(req, appName);
            if (tid != null && !tid.isEmpty()) {
                resp.setHeader(TraceContext.HEADER_TRACE_ID, tid);
            }
        } catch (Exception e) {
            // 日志初始化失败不阻断业务
            System.err.println("[EagleEyeFilter] trace init failed: " + e.getMessage());
        }
        try {
            chain.doFilter(req, resp);
        } finally {
            TraceContext.clear();
        }
    }
}
