package com.erp.dashboard.controller;

import com.erp.common.Result;
import com.erp.dashboard.service.DashboardService;
import com.erp.dashboard.vo.DashboardVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 仪表盘接口。
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "仪表盘")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/overview")
    @Operation(summary = "仪表盘总览", description = "三档金额（在录/待发货/应收）、本月回款、账龄分布、欠款 Top 客户")
    public Result<DashboardVO> overview() {
        return Result.success(dashboardService.overview());
    }
}
