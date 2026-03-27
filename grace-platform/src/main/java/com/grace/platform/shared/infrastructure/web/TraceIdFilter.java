package com.grace.platform.shared.infrastructure.web;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HexFormat;
import java.util.concurrent.ThreadLocalRandom;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter implements Filter {

    private static final String TRACE_ID_KEY = "traceId";
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        try {
            // 优先从请求头获取（支持上游传递），否则生成新的
            String traceId = null;
            if (request instanceof HttpServletRequest httpRequest) {
                traceId = httpRequest.getHeader(TRACE_ID_HEADER);
            }
            if (traceId == null || traceId.isBlank()) {
                traceId = generateTraceId();
            }

            // 放入 MDC，日志格式中通过 %X{traceId} 引用
            MDC.put(TRACE_ID_KEY, traceId);

            // 写入 Response Header，前端可用于问题反馈
            if (response instanceof HttpServletResponse httpResponse) {
                httpResponse.setHeader(TRACE_ID_HEADER, traceId);
            }

            chain.doFilter(request, response);
        } finally {
            // 必须清理，避免线程复用导致 traceId 串
            MDC.remove(TRACE_ID_KEY);
        }
    }

    private String generateTraceId() {
        // 格式：grc-{时间戳hex}-{随机4字节hex}，共 20 字符左右
        // 比纯 UUID 更短，且包含时间信息便于排序
        long timestamp = System.currentTimeMillis();
        byte[] random = new byte[4];
        ThreadLocalRandom.current().nextBytes(random);
        return "grc-" + Long.toHexString(timestamp) + "-" + HexFormat.of().formatHex(random);
    }
}
