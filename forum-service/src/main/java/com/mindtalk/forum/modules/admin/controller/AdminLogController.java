package com.mindtalk.forum.modules.admin.controller;

import com.mindtalk.common.model.PageResult;
import com.mindtalk.common.model.Result;
import com.mindtalk.forum.modules.admin.service.AdminService;
import com.mindtalk.forum.modules.admin.vo.AdminLogVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "操作日志")
@RestController
@RequestMapping("/admin/logs")
@RequiredArgsConstructor
public class AdminLogController {

    private final AdminService adminService;

    @Operation(summary = "分页查询操作日志")
    @GetMapping
    public Result<PageResult<AdminLogVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long adminId,
            @RequestParam(required = false) String action) {
        return Result.ok(adminService.getLogs(page, size, adminId, action));
    }
}
