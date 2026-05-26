package com.mindtalk.forum.modules.admin.controller;

import com.mindtalk.common.model.Result;
import com.mindtalk.forum.modules.admin.dto.PermissionUpdateDTO;
import com.mindtalk.forum.modules.admin.service.AdminService;
import com.mindtalk.forum.modules.admin.vo.PermissionTreeVO;
import com.mindtalk.forum.modules.admin.vo.PermissionVO;
import com.mindtalk.forum.modules.admin.vo.RoleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "管理端-权限管理")
@RestController
@RequestMapping("/admin/roles")
@RequiredArgsConstructor
public class AdminRoleController {

    private final AdminService adminService;

    @Operation(summary = "角色列表")
    @GetMapping
    public Result<List<RoleVO>> listRoles() {
        return Result.ok(adminService.getAllRoles());
    }

    @Operation(summary = "获取角色权限")
    @GetMapping("/{id}/permissions")
    public Result<List<PermissionVO>> getRolePermissions(@PathVariable Long id) {
        return Result.ok(adminService.getRolePermissions(id));
    }

    @Operation(summary = "更新角色权限")
    @PutMapping("/{id}/permissions")
    public Result<Void> updateRolePermissions(@PathVariable Long id,
                                              @Valid @RequestBody PermissionUpdateDTO dto) {
        Long adminId = getCurrentAdminId();
        adminService.updateRolePermissions(adminId, id, dto);
        return Result.ok();
    }

    @Operation(summary = "权限树")
    @GetMapping("/permissions/tree")
    public Result<List<PermissionTreeVO>> getPermissionTree() {
        return Result.ok(adminService.getPermissionTree());
    }

    private Long getCurrentAdminId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Long.valueOf(auth.getPrincipal().toString());
    }
}
