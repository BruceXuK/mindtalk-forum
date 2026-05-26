package com.mindtalk.forum.modules.series.controller;

import com.mindtalk.common.model.PageResult;
import com.mindtalk.common.model.Result;
import com.mindtalk.forum.modules.series.dto.CreateSeriesDTO;
import com.mindtalk.forum.modules.series.dto.UpdateSeriesDTO;
import com.mindtalk.forum.modules.series.service.SeriesService;
import com.mindtalk.forum.modules.series.vo.PostSeriesContextVO;
import com.mindtalk.forum.modules.series.vo.SeriesDetailVO;
import com.mindtalk.forum.modules.series.vo.SeriesVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "系列管理")
@RestController
@RequestMapping("/series")
@RequiredArgsConstructor
public class SeriesController {

    private final SeriesService seriesService;

    @Operation(summary = "创建系列")
    @PostMapping
    public Result<SeriesDetailVO> create(@Valid @RequestBody CreateSeriesDTO dto) {
        Long userId = getCurrentUserId();
        return Result.ok(seriesService.createSeries(userId, dto));
    }

    @Operation(summary = "编辑系列")
    @PutMapping("/{id}")
    public Result<SeriesDetailVO> update(@PathVariable Long id,
                                         @Valid @RequestBody UpdateSeriesDTO dto) {
        Long userId = getCurrentUserId();
        return Result.ok(seriesService.updateSeries(userId, id, dto));
    }

    @Operation(summary = "删除系列")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        seriesService.deleteSeries(userId, id);
        return Result.ok();
    }

    @Operation(summary = "系列详情")
    @GetMapping("/{id}")
    public Result<SeriesDetailVO> detail(@PathVariable Long id) {
        return Result.ok(seriesService.getSeriesDetail(id));
    }

    @Operation(summary = "获取我的系列列表")
    @GetMapping("/my")
    public Result<List<SeriesVO>> mySeries() {
        Long userId = getCurrentUserId();
        return Result.ok(seriesService.getMySeries(userId));
    }

    @Operation(summary = "获取用户公开系列列表")
    @GetMapping("/user/{userId}")
    public Result<PageResult<SeriesVO>> userSeries(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(seriesService.getUserSeries(userId, page, size));
    }

    @Operation(summary = "添加帖子到系列")
    @PostMapping("/{id}/posts")
    public Result<Void> addPost(@PathVariable Long id,
                                @RequestBody Map<String, Long> body) {
        Long userId = getCurrentUserId();
        Long postId = body.get("postId");
        seriesService.addPost(userId, id, postId);
        return Result.ok();
    }

    @Operation(summary = "从系列移除帖子")
    @DeleteMapping("/{id}/posts/{postId}")
    public Result<Void> removePost(@PathVariable Long id,
                                   @PathVariable Long postId) {
        Long userId = getCurrentUserId();
        seriesService.removePost(userId, id, postId);
        return Result.ok();
    }

    @Operation(summary = "获取帖子所属系列及上下文")
    @GetMapping("/by-post/{postId}")
    public Result<PostSeriesContextVO> postSeriesContext(@PathVariable Long postId) {
        return Result.ok(seriesService.getPostSeriesContext(postId));
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
