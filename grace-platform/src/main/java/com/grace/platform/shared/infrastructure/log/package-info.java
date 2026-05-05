/**
 * EagleEye 风格全链路日志基础设施。
 *
 * <p>职责：
 * <ul>
 *   <li>{@link com.grace.platform.shared.infrastructure.log.EagleEyeIdGenerator} 生成 30 位 traceId</li>
 *   <li>{@link com.grace.platform.shared.infrastructure.log.TraceContext} MDC + rpcId 层级管理</li>
 *   <li>{@link com.grace.platform.shared.infrastructure.log.EagleEyeFilter} HTTP 入口 trace 建立</li>
 *   <li>{@link com.grace.platform.shared.infrastructure.log.AccessLogFilter} access.log 写入</li>
 *   <li>{@link com.grace.platform.shared.infrastructure.log.DigestLogger} 外部调用 / 任务 digest 工具</li>
 *   <li>{@link com.grace.platform.shared.infrastructure.log.TtlExecutorConfig} TTL 包装的异步执行器</li>
 * </ul>
 */
package com.grace.platform.shared.infrastructure.log;
