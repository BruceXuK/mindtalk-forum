package com.mindtalk.forum.modules.user.controller;

import com.mindtalk.common.model.Result;
import com.mindtalk.forum.modules.user.dto.ChangePasswordDTO;
import com.mindtalk.forum.modules.user.dto.UpdateProfileDTO;
import com.mindtalk.forum.modules.user.service.UserService;
import com.mindtalk.forum.modules.user.vo.UserProfileVO;
import com.mindtalk.forum.modules.user.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户接口
 */
@Tag(name = "用户管理")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/me")
    public Result<UserVO> currentUser() {
        Long userId = getCurrentUserId();
        return Result.ok(userService.getCurrentUser(userId));
    }

    @Operation(summary = "修改密码")
    @PutMapping("/me/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        Long userId = getCurrentUserId();
        userService.changePassword(userId, dto);
        return Result.ok();
    }

    @Operation(summary = "更新个人资料")
    @PutMapping("/me/profile")
    public Result<UserVO> updateProfile(@Valid @RequestBody UpdateProfileDTO dto) {
        Long userId = getCurrentUserId();
        return Result.ok(userService.updateProfile(userId, dto));
    }

    @Operation(summary = "上传头像")
    @PostMapping("/me/avatar")
    public Result<UserVO> uploadAvatar(@RequestParam("file") MultipartFile file) {
        Long userId = getCurrentUserId();
        return Result.ok(userService.uploadAvatar(userId, file));
    }

    @Operation(summary = "关注用户")
    @PostMapping("/{id}/follow")
    public Result<Void> follow(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        userService.followUser(userId, id);
        return Result.ok();
    }

    @Operation(summary = "取消关注")
    @DeleteMapping("/{id}/follow")
    public Result<Void> unfollow(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        userService.unfollowUser(userId, id);
        return Result.ok();
    }

    @Operation(summary = "获取当前用户的关注列表")
    @GetMapping("/me/following")
    public Result<List<UserVO>> following(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "5") int size) {
        Long userId = getCurrentUserId();
        return Result.ok(userService.getFollowingList(userId, keyword, size));
    }

    @Operation(summary = "搜索用户（@提及用）")
    @GetMapping("/search")
    public Result<List<UserVO>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "10") int limit) {
        return Result.ok(userService.searchUsers(keyword, limit));
    }

    @Operation(summary = "查看用户主页")
    @GetMapping("/{id}/profile")
    public Result<UserProfileVO> profile(@PathVariable Long id) {
        Long currentUserId = getCurrentUserIdOrNull();
        return Result.ok(userService.getUserProfile(id, currentUserId));
    }

    // ──────────────────── 内部方法 ────────────────────

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long userId) {
            return userId;
        }
        throw new RuntimeException("未登录");
    }

    private Long getCurrentUserIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long userId) {
            return userId;
        }
        return null;
    }
}
