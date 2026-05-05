package com.grace.platform.shared.infrastructure.log;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TraceContextTest {

    @AfterEach
    void cleanup() {
        TraceContext.clear();
    }

    @Test
    void rpcIdShouldNestHierarchically() {
        MDC.put(TraceContext.MDC_RPC_ID, "0");

        String p1 = TraceContext.pushSpan();
        assertEquals("0.1", MDC.get(TraceContext.MDC_RPC_ID));

        String p2 = TraceContext.pushSpan();
        assertEquals("0.1.2", MDC.get(TraceContext.MDC_RPC_ID),
                "同一线程 CHILD_COUNTER 递增，第二次 push 的序号应为 2");

        TraceContext.popSpan(p2);
        assertEquals("0.1", MDC.get(TraceContext.MDC_RPC_ID));

        TraceContext.popSpan(p1);
        assertEquals("0", MDC.get(TraceContext.MDC_RPC_ID));
    }

    @Test
    void clearShouldResetEverything() {
        MDC.put(TraceContext.MDC_TRACE_ID, "abc");
        MDC.put(TraceContext.MDC_RPC_ID, "0.5");
        TraceContext.clear();
        assertEquals("", TraceContext.currentTraceId());
        assertEquals("0", TraceContext.currentRpcId());
    }
}
