package com.mindtalk.forum.modules.admin.controller;

import com.mindtalk.common.model.PageResult;
import com.mindtalk.common.model.Result;
import com.mindtalk.forum.modules.admin.dto.ReportHandleDTO;
import com.mindtalk.forum.modules.admin.service.AdminService;
import com.mindtalk.forum.modules.admin.vo.ReportVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理端-举报处理")
@RestController
@RequestMapping("/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final AdminService adminService;

    @Operation(summary = "举报列表")
    @GetMapping
    public Result<PageResult<ReportVO>> list(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String targetType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(adminService.getReportPage(status, targetType, page, size));
    }

    @Operation(summary = "举报详情")
    @GetMapping("/{id}")
    public Result<ReportVO> detail(@PathVariable Long id) {
        return Result.ok(adminService.getReportDetail(id));
    }

    @Operation(summary = "处理举报")
    @PutMapping("/{id}/handle")
    public Result<Void> handle(@PathVariable Long id,
                               @Valid @RequestBody ReportHandleDTO dto) {
        Long adminId = getCurrentAdminId();
        adminService.handleReport(adminId, id, dto);
        return Result.ok();
    }

    private Long getCurrentAdminId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Long.valueOf(auth.getPrincipal().toString());
    }
}
