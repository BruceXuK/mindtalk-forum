package com.mindtalk.forum.modules.reading.controller;

import com.mindtalk.common.model.PageResult;
import com.mindtalk.common.model.Result;
import com.mindtalk.forum.modules.reading.service.ReadingHistoryService;
import com.mindtalk.forum.modules.reading.vo.ReadingHistoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Tag(name = "阅读历史")
@RestController
@RequestMapping("/reading-history")
@RequiredArgsConstructor
public class ReadingHistoryController {

    private final ReadingHistoryService readingHistoryService;

    @Operation(summary = "获取阅读历史列表")
    @GetMapping
    public Result<PageResult<ReadingHistoryVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = getCurrentUserId();
        return Result.ok(readingHistoryService.getList(userId, page, size));
    }

    @Operation(summary = "记录阅读历史")
    @PostMapping("/{postId}")
    public Result<Void> record(@PathVariable Long postId) {
        Long userId = getCurrentUserId();
        readingHistoryService.record(userId, postId);
        return Result.ok();
    }

    @Operation(summary = "删除单条阅读历史")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        readingHistoryService.delete(userId, id);
        return Result.ok();
    }

    @Operation(summary = "清空阅读历史")
    @DeleteMapping
    public Result<Void> clearAll() {
        Long userId = getCurrentUserId();
        readingHistoryService.clearAll(userId);
        return Result.ok();
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long userId) {
            return userId;
        }
        throw new RuntimeException("未登录");
    }
}
