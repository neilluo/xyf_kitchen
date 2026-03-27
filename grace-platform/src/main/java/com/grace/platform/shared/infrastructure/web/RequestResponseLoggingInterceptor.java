package com.grace.platform.shared.infrastructure.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;

@Component
public class RequestResponseLoggingInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RequestResponseLoggingInterceptor.class);
    private static final String START_TIME_ATTR = "requestStartTime";
    private static final int MAX_BODY_LOG_LENGTH = 1024;  // Body 最多记录 1KB

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) {
        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());

        if (log.isInfoEnabled()) {
            String queryString = request.getQueryString();
            log.info("[REQ] {} {} {}",
                     request.getMethod(),
                     request.getRequestURI(),
                     queryString != null ? "?" + queryString : "");
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        Long startTime = (Long) request.getAttribute(START_TIME_ATTR);
        long elapsed = startTime != null ? System.currentTimeMillis() - startTime : -1;

        // 记录请求 Body（DEBUG 级别）
        if (log.isDebugEnabled() && request instanceof ContentCachingRequestWrapper wrapper) {
            byte[] body = wrapper.getContentAsByteArray();
            if (body.length > 0) {
                try {
                    String bodyStr = new String(body, wrapper.getCharacterEncoding());
                    String truncated = bodyStr.length() > MAX_BODY_LOG_LENGTH
                        ? bodyStr.substring(0, MAX_BODY_LOG_LENGTH) + "...(truncated)"
                        : bodyStr;
                    log.debug("[REQ-BODY] {}", truncated);
                } catch (Exception e) {
                    log.debug("[REQ-BODY] <unable to read body>");
                }
            }
        }

        if (ex != null) {
            log.error("[RES] {} {} | status={} | {}ms | error={}",
                      request.getMethod(), request.getRequestURI(),
                      response.getStatus(), elapsed, ex.getMessage());
        } else {
            log.info("[RES] {} {} | status={} | {}ms",
                     request.getMethod(), request.getRequestURI(),
                     response.getStatus(), elapsed);
        }
    }
}
