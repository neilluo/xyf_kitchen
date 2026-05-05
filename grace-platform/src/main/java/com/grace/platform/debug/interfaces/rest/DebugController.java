package com.grace.platform.debug.interfaces.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 调试专用 Controller (仅开发环境启用)。
 * <p>
 * 提供数据库只读查询接口,用于排查问题。
 * 生产环境应通过 Profile 或 Security 规则禁用。
 *
 * @author Grace Platform Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/debug")
public class DebugController {

    private static final Logger logger = LoggerFactory.getLogger(DebugController.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 执行只读 SQL 查询。
     * <p>
     * 安全限制:
     * - 仅允许 SELECT 语句
     * - 禁止 INSERT/UPDATE/DELETE/DROP/ALTER 等写操作
     * - 建议配合 Spring Security 限制 IP 访问
     *
     * @param request 查询请求
     * @return 查询结果
     */
    @PostMapping("/query")
    public Map<String, Object> executeReadOnlyQuery(@RequestBody QueryRequest request) {
        String sql = request.query();

        // 安全检查: 仅允许 SELECT
        if (!isValidReadOnlyQuery(sql)) {
            logger.warn("Blocked non-SELECT query: {}", sql);
            return Map.of(
                "success", false,
                "error", "Only SELECT queries are allowed",
                "data", List.of()
            );
        }

        logger.info("Debug query executed: {}", sql);

        try {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
            return Map.of(
                "success", true,
                "rowCount", results.size(),
                "data", results
            );
        } catch (Exception e) {
            logger.error("Query execution failed: {}", sql, e);
            return Map.of(
                "success", false,
                "error", e.getMessage(),
                "data", List.of()
            );
        }
    }

    /**
     * 查询视频状态分布 (快捷端点)。
     */
    @GetMapping("/video/status-distribution")
    public Map<String, Object> getVideoStatusDistribution() {
        String sql = "SELECT status, COUNT(*) as count FROM video GROUP BY status ORDER BY count DESC";
        
        try {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
            return Map.of(
                "success", true,
                "data", results
            );
        } catch (Exception e) {
            logger.error("Failed to get status distribution", e);
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }
    }

    /**
     * 根据 ID 查询视频详情。
     */
    @GetMapping("/video/{videoId}")
    public Map<String, Object> getVideoDetail(@PathVariable String videoId) {
        String sql = "SELECT * FROM video WHERE id = ?";
        
        try {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, videoId);
            if (results.isEmpty()) {
                return Map.of(
                    "success", false,
                    "error", "Video not found: " + videoId
                );
            }
            return Map.of(
                "success", true,
                "data", results.get(0)
            );
        } catch (Exception e) {
            logger.error("Failed to get video detail: {}", videoId, e);
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }
    }

    /**
     * 验证是否为只读查询。
     */
    private boolean isValidReadOnlyQuery(String sql) {
        if (sql == null || sql.isBlank()) {
            return false;
        }
        
        String normalized = sql.trim().toUpperCase();
        
        // 仅允许 SELECT
        if (!normalized.startsWith("SELECT")) {
            return false;
        }
        
        // 禁止危险关键词
        String[] dangerousKeywords = {
            "INSERT", "UPDATE", "DELETE", "DROP", "ALTER", 
            "CREATE", "TRUNCATE", "REPLACE", "MERGE"
        };
        
        for (String keyword : dangerousKeywords) {
            if (normalized.contains(keyword)) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * 查询请求 DTO。
     */
    public record QueryRequest(String query) {
    }
}
