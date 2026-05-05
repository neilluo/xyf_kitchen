package com.grace.platform.shared.infrastructure.log;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Optional;

/**
 * Trace 上下文工具。封装 MDC 读写与 rpcId 层级推进。
 * 所有方法内部防御式编程，不外抛（日志异常不得拖垮业务）。
 */
public final class TraceContext {

    public static final String MDC_TRACE_ID = "traceId";
    public static final String MDC_RPC_ID = "rpcId";
    public static final String MDC_APP_NAME = "appName";

    public static final String HEADER_TRACE_ID = "X-EagleEye-TraceId";
    public static final String HEADER_RPC_ID = "X-EagleEye-RpcId";

    /** 每层 span 的子自增计数（ThreadLocal 随 TTL 一并传递） */
    private static final ThreadLocal<Integer> CHILD_COUNTER = ThreadLocal.withInitial(() -> 0);

    private TraceContext() {
    }

    /** 入口调用：读取或生成 trace，写入 MDC，返回 traceId */
    public static String startTrace(HttpServletRequest req, String appName) {
        try {
            String tid = Optional.ofNullable(req.getHeader(HEADER_TRACE_ID))
                    .filter(StringUtils::hasText)
                    .orElseGet(EagleEyeIdGenerator::next);
            String rid = Optional.ofNullable(req.getHeader(HEADER_RPC_ID))
                    .filter(StringUtils::hasText)
                    .orElse("0");
            MDC.put(MDC_TRACE_ID, tid);
            MDC.put(MDC_RPC_ID, rid);
            MDC.put(MDC_APP_NAME, appName);
            CHILD_COUNTER.set(0);
            return tid;
        } catch (Exception e) {
            // 降级：空 trace 但不阻断主链路
            return "";
        }
    }

    /** 进入一层子调用；返回父 rpcId 供调用方在 finally 里 popSpan 恢复 */
    public static String pushSpan() {
        try {
            String parent = Optional.ofNullable(MDC.get(MDC_RPC_ID)).orElse("0");
            int seq = CHILD_COUNTER.get() + 1;
            CHILD_COUNTER.set(seq);
            String child = parent + "." + seq;
            MDC.put(MDC_RPC_ID, child);
            return parent;
        } catch (Exception e) {
            return "0";
        }
    }

    public static void popSpan(String parent) {
        try {
            MDC.put(MDC_RPC_ID, parent == null ? "0" : parent);
        } catch (Exception ignored) {
            // 日志降级，不外抛
        }
    }

    public static void clear() {
        try {
            MDC.clear();
            CHILD_COUNTER.remove();
        } catch (Exception ignored) {
            // 日志降级，不外抛
        }
    }

    public static String currentTraceId() {
        return Optional.ofNullable(MDC.get(MDC_TRACE_ID)).orElse("");
    }

    public static String currentRpcId() {
        return Optional.ofNullable(MDC.get(MDC_RPC_ID)).orElse("0");
    }

    /**
     * 当 MDC 中无 traceId 时，从给定 extInfo（DB 中持久化的 ext_info）中恢复。
     * <p>用于定时任务、回调等无 HTTP 上游 trace 的入口，避免日志链路断裂。
     * <p>已有 traceId 时不覆盖。
     *
     * @param extInfo 视频/业务对象的扩展字段
     * @return true 表示发生了回填
     */
    public static boolean restoreIfAbsent(Map<String, Object> extInfo) {
        try {
            if (extInfo == null || extInfo.isEmpty()) {
                return false;
            }
            String existing = MDC.get(MDC_TRACE_ID);
            if (existing != null && !existing.isBlank()) {
                return false;
            }
            Object tid = extInfo.get(MDC_TRACE_ID);
            if (tid == null || !StringUtils.hasText(tid.toString())) {
                return false;
            }
            MDC.put(MDC_TRACE_ID, tid.toString());
            Object rid = extInfo.get(MDC_RPC_ID);
            MDC.put(MDC_RPC_ID, rid != null && StringUtils.hasText(rid.toString()) ? rid.toString() : "0");
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
