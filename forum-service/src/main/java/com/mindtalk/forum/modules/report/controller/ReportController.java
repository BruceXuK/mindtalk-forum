package com.mindtalk.forum.modules.report.controller;

import com.mindtalk.common.model.Result;
import com.mindtalk.forum.modules.admin.dto.CreateReportDTO;
import com.mindtalk.forum.modules.admin.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Tag(name = "举报")
@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final AdminService adminService;

    @Operation(summary = "提交举报")
    @PostMapping
    public Result<Void> create(@Valid @RequestBody CreateReportDTO dto) {
        Long userId = getCurrentUserId();
        adminService.createReport(userId, dto);
        return Result.ok();
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Long.valueOf(auth.getPrincipal().toString());
    }
}
