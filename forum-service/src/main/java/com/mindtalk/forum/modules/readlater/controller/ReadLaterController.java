package com.mindtalk.forum.modules.readlater.controller;

import com.mindtalk.common.model.PageResult;
import com.mindtalk.common.model.Result;
import com.mindtalk.forum.modules.readlater.service.ReadLaterService;
import com.mindtalk.forum.modules.readlater.vo.ReadLaterVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "稍后阅读")
@RestController
@RequestMapping("/read-later")
@RequiredArgsConstructor
public class ReadLaterController {

    private final ReadLaterService readLaterService;

    @Operation(summary = "获取稍后阅读列表")
    @GetMapping
    public Result<PageResult<ReadLaterVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = getCurrentUserId();
        return Result.ok(readLaterService.getList(userId, page, size));
    }

    @Operation(summary = "添加到稍后阅读")
    @PostMapping("/{postId}")
    public Result<Void> add(@PathVariable Long postId) {
        Long userId = getCurrentUserId();
        readLaterService.add(userId, postId);
        return Result.ok();
    }

    @Operation(summary = "检查是否已标记")
    @GetMapping("/{postId}/status")
    public Result<Map<String, Boolean>> checkStatus(@PathVariable Long postId) {
        Long userId = getCurrentUserIdOrNull();
        boolean bookmarked = userId != null && readLaterService.isBookmarked(userId, postId);
        return Result.ok(Map.of("bookmarked", bookmarked));
    }

    @Operation(summary = "从稍后阅读移除")
    @DeleteMapping("/{postId}")
    public Result<Void> remove(@PathVariable Long postId) {
        Long userId = getCurrentUserId();
        readLaterService.remove(userId, postId);
        return Result.ok();
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
