package com.mindtalk.forum.modules.subscription.controller;

import com.mindtalk.common.model.Result;
import com.mindtalk.forum.modules.subscription.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "订阅管理")
@RestController
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @Operation(summary = "订阅标签")
    @PostMapping("/tags/{tagId}")
    public Result<Void> subscribeTag(@PathVariable Long tagId) {
        Long userId = getCurrentUserId();
        subscriptionService.subscribeTag(userId, tagId);
        return Result.ok();
    }

    @Operation(summary = "取消标签订阅")
    @DeleteMapping("/tags/{tagId}")
    public Result<Void> unsubscribeTag(@PathVariable Long tagId) {
        Long userId = getCurrentUserId();
        subscriptionService.unsubscribeTag(userId, tagId);
        return Result.ok();
    }

    @Operation(summary = "检查标签订阅状态")
    @GetMapping("/tags/{tagId}/status")
    public Result<Map<String, Boolean>> checkTagStatus(@PathVariable Long tagId) {
        Long userId = getCurrentUserIdOrNull();
        boolean subscribed = userId != null && subscriptionService.isTagSubscribed(userId, tagId);
        return Result.ok(Map.of("subscribed", subscribed));
    }

    @Operation(summary = "获取已订阅标签 ID 列表")
    @GetMapping("/tags/my")
    public Result<List<Long>> myTagIds() {
        Long userId = getCurrentUserId();
        return Result.ok(subscriptionService.getSubscribedTagIds(userId));
    }

    @Operation(summary = "订阅分类")
    @PostMapping("/categories/{categoryId}")
    public Result<Void> subscribeCategory(@PathVariable Long categoryId) {
        Long userId = getCurrentUserId();
        subscriptionService.subscribeCategory(userId, categoryId);
        return Result.ok();
    }

    @Operation(summary = "取消分类订阅")
    @DeleteMapping("/categories/{categoryId}")
    public Result<Void> unsubscribeCategory(@PathVariable Long categoryId) {
        Long userId = getCurrentUserId();
        subscriptionService.unsubscribeCategory(userId, categoryId);
        return Result.ok();
    }

    @Operation(summary = "检查分类订阅状态")
    @GetMapping("/categories/{categoryId}/status")
    public Result<Map<String, Boolean>> checkCategoryStatus(@PathVariable Long categoryId) {
        Long userId = getCurrentUserIdOrNull();
        boolean subscribed = userId != null && subscriptionService.isCategorySubscribed(userId, categoryId);
        return Result.ok(Map.of("subscribed", subscribed));
    }

    @Operation(summary = "获取已订阅分类 ID 列表")
    @GetMapping("/categories/my")
    public Result<List<Long>> myCategoryIds() {
        Long userId = getCurrentUserId();
        return Result.ok(subscriptionService.getSubscribedCategoryIds(userId));
    }

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
