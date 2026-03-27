package com.grace.platform.dashboard.interfaces.rest;

import com.grace.platform.dashboard.application.DashboardQueryService;
import com.grace.platform.dashboard.application.dto.DashboardOverviewResponse;
import com.grace.platform.shared.application.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dashboard 控制器
 * 提供仪表盘概览数据查询接口
 *
 * @author Grace Platform Team
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardQueryService dashboardQueryService;

    public DashboardController(DashboardQueryService dashboardQueryService) {
        this.dashboardQueryService = dashboardQueryService;
    }

    /**
     * A1. GET /api/dashboard/overview
     * 聚合查询仪表盘全部概览数据
     *
     * @param dateRange 时间范围过滤：7d / 30d / 90d / all，默认为 30d
     * @return 仪表盘概览数据
     */
    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<DashboardOverviewResponse>> getOverview(
            @RequestParam(defaultValue = "30d") String dateRange) {
        DashboardOverviewResponse overview = dashboardQueryService.getOverview(dateRange);
        return ResponseEntity.ok(ApiResponse.success(overview));
    }
}
