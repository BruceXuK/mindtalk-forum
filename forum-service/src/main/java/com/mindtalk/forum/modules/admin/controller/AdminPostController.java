package com.mindtalk.forum.modules.admin.controller;

import com.mindtalk.common.model.PageResult;
import com.mindtalk.common.model.Result;
import com.mindtalk.forum.modules.admin.dto.PostAuditDTO;
import com.mindtalk.forum.modules.admin.service.AdminService;
import com.mindtalk.forum.modules.post.vo.PostVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "管理端-帖子审核")
@RestController
@RequestMapping("/admin/posts")
@RequiredArgsConstructor
public class AdminPostController {

    private final AdminService adminService;

    @Operation(summary = "帖子列表")
    @GetMapping
    public Result<PageResult<PostVO>> list(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(adminService.getPostPage(status, keyword, page, size));
    }

    @Operation(summary = "审核帖子")
    @PutMapping("/{id}/status")
    public Result<Void> audit(@PathVariable Long id,
                              @Valid @RequestBody PostAuditDTO dto) {
        Long adminId = getCurrentAdminId();
        adminService.auditPost(adminId, id, dto);
        return Result.ok();
    }

    @Operation(summary = "删除帖子")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long adminId = getCurrentAdminId();
        adminService.forceDeletePost(adminId, id);
        return Result.ok();
    }

    @Operation(summary = "置顶/取消置顶")
    @PutMapping("/{id}/pin")
    public Result<Void> pin(@PathVariable Long id,
                            @RequestParam boolean pinned,
                            @RequestParam(required = false) Integer untilDays) {
        Long adminId = getCurrentAdminId();
        adminService.pinPost(adminId, id, pinned, untilDays);
        return Result.ok();
    }

    @Operation(summary = "加精/取消加精")
    @PutMapping("/{id}/feature")
    public Result<Void> feature(@PathVariable Long id,
                                @RequestParam boolean featured,
                                @RequestParam(required = false) Integer untilDays) {
        Long adminId = getCurrentAdminId();
        adminService.featurePost(adminId, id, featured, untilDays);
        return Result.ok();
    }

    @Operation(summary = "批量删除帖子")
    @PostMapping("/batch-delete")
    public Result<Void> batchDelete(@RequestBody Map<String, List<Long>> body) {
        adminService.batchDeletePosts(body.get("ids"));
        return Result.ok();
    }

    private Long getCurrentAdminId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Long.valueOf(auth.getPrincipal().toString());
    }
}
