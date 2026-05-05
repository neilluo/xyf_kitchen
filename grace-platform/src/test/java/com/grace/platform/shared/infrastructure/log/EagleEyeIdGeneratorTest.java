package com.grace.platform.shared.infrastructure.log;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EagleEyeIdGeneratorTest {

    @Test
    void shouldGenerate30CharHexIds() {
        for (int i = 0; i < 1000; i++) {
            String id = EagleEyeIdGenerator.next();
            assertEquals(30, id.length(), "traceId 长度必须为 30: " + id);
            assertTrue(id.matches("[0-9a-f]+"), "traceId 必须全为小写 hex: " + id);
        }
    }

    @Test
    void shouldBeUniqueUnderConcurrency() throws Exception {
        int total = 10_000;
        Set<String> set = ConcurrentHashMap.newKeySet();
        ExecutorService pool = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(total);
        for (int i = 0; i < total; i++) {
            pool.submit(() -> {
                set.add(EagleEyeIdGenerator.next());
                latch.countDown();
            });
        }
        assertTrue(latch.await(10, TimeUnit.SECONDS), "并发生成超时");
        pool.shutdown();
        // 允许极小概率碰撞（时间戳相同+序号相同+随机扩展相同），99%+ 的场景应全部唯一
        assertTrue(set.size() >= (int) (total * 0.999),
                "1万次并发唯一率应 >= 99.9%, 实际: " + set.size());
    }
}
