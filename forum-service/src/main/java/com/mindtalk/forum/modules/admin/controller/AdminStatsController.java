package com.mindtalk.forum.modules.admin.controller;

import com.mindtalk.common.model.Result;
import com.mindtalk.forum.modules.admin.dto.StatsQueryDTO;
import com.mindtalk.forum.modules.admin.service.AdminService;
import com.mindtalk.forum.modules.admin.vo.StatsOverviewVO;
import com.mindtalk.forum.modules.admin.vo.StatsTrendVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "管理端-统计分析")
@RestController
@RequestMapping("/admin/stats")
@RequiredArgsConstructor
public class AdminStatsController {

    private final AdminService adminService;

    @Operation(summary = "统计概览")
    @GetMapping("/overview")
    public Result<StatsOverviewVO> overview() {
        return Result.ok(adminService.getStatsOverview());
    }

    @Operation(summary = "趋势统计")
    @GetMapping("/trends")
    public Result<List<StatsTrendVO>> trends(@RequestParam(defaultValue = "7") int days) {
        return Result.ok(adminService.getStatsTrends(days));
    }

    @GetMapping("/category-distribution")
    public Result<List<Map<String, Object>>> categoryDistribution() {
        return Result.ok(adminService.getCategoryDistribution());
    }

    @GetMapping("/hourly-activity")
    public Result<List<Map<String, Object>>> hourlyActivity() {
        return Result.ok(adminService.getHourlyActivity());
    }
}
