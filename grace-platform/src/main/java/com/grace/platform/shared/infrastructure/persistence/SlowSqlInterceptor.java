package com.grace.platform.shared.infrastructure.persistence;

import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.plugin.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Statement;
import java.util.Properties;

/**
 * MyBatis 慢 SQL 拦截器
 * 执行时间超过阈值的 SQL 记录 WARN 日志
 */
@Intercepts({
    @Signature(type = StatementHandler.class, method = "update", args = {Statement.class}),
    @Signature(type = StatementHandler.class, method = "query", args = {Statement.class}),
    @Signature(type = StatementHandler.class, method = "batch", args = {Statement.class})
})
public class SlowSqlInterceptor implements Interceptor {

    private static final Logger log = LoggerFactory.getLogger(SlowSqlInterceptor.class);

    // 慢 SQL 阈值（毫秒），可通过配置覆盖
    private long slowThresholdMs = 1000;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        long startTime = System.currentTimeMillis();
        try {
            return invocation.proceed();
        } finally {
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed > slowThresholdMs) {
                // 获取执行的 SQL 信息
                Object target = invocation.getTarget();
                if (target instanceof StatementHandler statementHandler) {
                    String mappedStatementId = statementHandler.getBoundSql().getSql();
                    log.warn("[SLOW-SQL] {}ms | mapper={}", elapsed, mappedStatementId);
                } else {
                    log.warn("[SLOW-SQL] {}ms | mapper=<unknown>", elapsed);
                }
            }
        }
    }

    @Override
    public void setProperties(Properties properties) {
        String threshold = properties.getProperty("slowThresholdMs");
        if (threshold != null) {
            try {
                this.slowThresholdMs = Long.parseLong(threshold);
            } catch (NumberFormatException e) {
                // 使用默认值
            }
        }
    }
}
