package com.mindtalk.forum.modules.badge.controller;

import com.mindtalk.common.model.Result;
import com.mindtalk.forum.modules.badge.service.BadgeService;
import com.mindtalk.forum.modules.badge.vo.BadgeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "勋章系统")
@RestController
@RequestMapping("/badges")
@RequiredArgsConstructor
public class BadgeController {

    private final BadgeService badgeService;

    @Operation(summary = "获取我的勋章")
    @GetMapping("/my")
    public Result<List<BadgeVO>> myBadges() {
        Long userId = getCurrentUserId();
        return Result.ok(badgeService.getUserBadges(userId));
    }

    @Operation(summary = "获取用户勋章")
    @GetMapping("/user/{userId}")
    public Result<List<BadgeVO>> userBadges(@PathVariable Long userId) {
        return Result.ok(badgeService.getUserBadges(userId));
    }

    private Long getCurrentUserId() {
        var auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long userId) {
            return userId;
        }
        throw new RuntimeException("未登录");
    }
}
