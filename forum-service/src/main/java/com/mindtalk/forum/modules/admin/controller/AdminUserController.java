package com.mindtalk.forum.modules.admin.controller;

import com.mindtalk.common.model.PageResult;
import com.mindtalk.common.model.Result;
import com.mindtalk.forum.modules.admin.dto.RoleAssignDTO;
import com.mindtalk.forum.modules.admin.dto.UserManageDTO;
import com.mindtalk.forum.modules.admin.service.AdminService;
import com.mindtalk.forum.modules.admin.vo.AdminUserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "管理端-用户管理")
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminService adminService;

    @Operation(summary = "用户列表")
    @GetMapping
    public Result<PageResult<AdminUserVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(adminService.getUserPage(keyword, status, page, size));
    }

    @Operation(summary = "用户详情")
    @GetMapping("/{id}")
    public Result<AdminUserVO> detail(@PathVariable Long id) {
        return Result.ok(adminService.getUserDetail(id));
    }

    @Operation(summary = "修改用户状态（封禁/解封）")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @Valid @RequestBody UserManageDTO dto) {
        Long adminId = getCurrentAdminId();
        adminService.updateUserStatus(adminId, id, dto);
        return Result.ok();
    }

    @Operation(summary = "分配用户角色")
    @PutMapping("/{id}/roles")
    public Result<Void> assignRoles(@PathVariable Long id,
                                    @Valid @RequestBody RoleAssignDTO dto) {
        Long adminId = getCurrentAdminId();
        adminService.assignUserRoles(adminId, id, dto);
        return Result.ok();
    }

    @Operation(summary = "重置用户密码")
    @PostMapping("/{id}/reset-password")
    public Result<Map<String, String>> resetPassword(@PathVariable Long id) {
        Long adminId = getCurrentAdminId();
        String newPassword = adminService.resetUserPassword(adminId, id);
        return Result.ok(Map.of("password", newPassword));
    }

    @Operation(summary = "批量封禁用户")
    @PostMapping("/batch-ban")
    public Result<Void> batchBan(@RequestBody Map<String, List<Long>> body) {
        adminService.batchUpdateUserStatus(body.get("ids"), 0);
        return Result.ok();
    }

    @Operation(summary = "批量解封用户")
    @PostMapping("/batch-unban")
    public Result<Void> batchUnban(@RequestBody Map<String, List<Long>> body) {
        adminService.batchUpdateUserStatus(body.get("ids"), 1);
        return Result.ok();
    }

    private Long getCurrentAdminId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Long.valueOf(auth.getPrincipal().toString());
    }
}
