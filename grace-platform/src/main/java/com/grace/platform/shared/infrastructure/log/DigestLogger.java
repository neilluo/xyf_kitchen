package com.grace.platform.shared.infrastructure.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Digest 日志工具类。每种外部调用 / 业务事件一行 pipe 分隔摘要。
 *
 * <p>所有方法内部全量 try/catch，日志异常绝不外抛（主链路保护）。
 *
 * <p>Logger name 必须与 logback-spring.xml 中的 &lt;logger name=...&gt; 一致：
 * <ul>
 *   <li>LLM_DIGEST       → llm-digest.log</li>
 *   <li>YOUTUBE_DIGEST   → youtube-digest.log</li>
 *   <li>OSS_DIGEST       → oss-digest.log</li>
 *   <li>OPENCRAWL_DIGEST → opencrawl-digest.log</li>
 *   <li>TASK_DIGEST      → task-digest.log</li>
 * </ul>
 *
 * <p>字段顺序一旦落地就不得再改（兼容离线脚本 / 数仓）。
 */
public final class DigestLogger {

    private static final Logger LLM = LoggerFactory.getLogger("LLM_DIGEST");
    private static final Logger YOUTUBE = LoggerFactory.getLogger("YOUTUBE_DIGEST");
    private static final Logger OSS = LoggerFactory.getLogger("OSS_DIGEST");
    private static final Logger OPENCRAWL = LoggerFactory.getLogger("OPENCRAWL_DIGEST");
    private static final Logger TASK = LoggerFactory.getLogger("TASK_DIGEST");
    private static final Logger FALLBACK = LoggerFactory.getLogger("DIGEST_FALLBACK");

    private DigestLogger() {
    }

    /**
     * LLM 调用摘要。字段：api|model|tokens|costMs|status|errCode|bizId
     */
    public static void llm(String api, String model, int tokens, long costMs,
                           String status, String errCode, String bizId) {
        try {
            LLM.info("{}|{}|{}|{}|{}|{}|{}",
                    esc(api), esc(model), tokens, costMs,
                    esc(status), esc(errCode), esc(bizId));
        } catch (Exception e) {
            FALLBACK.error("[DigestLogger.llm] {}", e.getMessage(), e);
        }
    }

    /**
     * YouTube API 调用摘要。字段：api|videoId|costMs|status|errCode|channelId
     */
    public static void youtube(String api, String videoId, long costMs,
                               String status, String errCode, String channelId) {
        try {
            YOUTUBE.info("{}|{}|{}|{}|{}|{}",
                    esc(api), esc(videoId), costMs,
                    esc(status), esc(errCode), esc(channelId));
        } catch (Exception e) {
            FALLBACK.error("[DigestLogger.youtube] {}", e.getMessage(), e);
        }
    }

    /**
     * Aliyun OSS / STS 调用摘要。字段：api|bucket|objectKey|sizeBytes|costMs|status|errCode
     */
    public static void oss(String api, String bucket, String objectKey, long sizeBytes,
                           long costMs, String status, String errCode) {
        try {
            OSS.info("{}|{}|{}|{}|{}|{}|{}",
                    esc(api), esc(bucket), esc(objectKey), sizeBytes,
                    costMs, esc(status), esc(errCode));
        } catch (Exception e) {
            FALLBACK.error("[DigestLogger.oss] {}", e.getMessage(), e);
        }
    }

    /**
     * OpenCrawl API 调用摘要。字段：api|channelUrl|method|costMs|status|errCode|resultUrl
     */
    public static void opencrawl(String api, String channelUrl, String method, long costMs,
                                 String status, String errCode, String resultUrl) {
        try {
            OPENCRAWL.info("{}|{}|{}|{}|{}|{}|{}",
                    esc(api), esc(channelUrl), esc(method), costMs,
                    esc(status), esc(errCode), esc(resultUrl));
        } catch (Exception e) {
            FALLBACK.error("[DigestLogger.opencrawl] {}", e.getMessage(), e);
        }
    }

    /**
     * 任务状态变更摘要。字段：taskId|bizType|oldStatus|newStatus|progress
     */
    public static void task(String taskId, String bizType,
                            String oldStatus, String newStatus, int progress) {
        try {
            TASK.info("{}|{}|{}|{}|{}",
                    esc(taskId), esc(bizType),
                    esc(oldStatus), esc(newStatus), progress);
        } catch (Exception e) {
            FALLBACK.error("[DigestLogger.task] {}", e.getMessage(), e);
        }
    }

    /** null / 空 → "-"；含 | 的值转义 */
    private static String esc(String s) {
        if (s == null || s.isEmpty()) {
            return "-";
        }
        return s.replace("|", "\\|");
    }
}
