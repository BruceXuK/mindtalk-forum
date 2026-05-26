package com.mindtalk.forum.modules.post.controller;

import com.mindtalk.common.model.PageResult;
import com.mindtalk.common.model.Result;
import com.mindtalk.forum.modules.post.dto.CreatePostDTO;
import com.mindtalk.forum.modules.post.dto.PostQueryDTO;
import com.mindtalk.forum.modules.post.dto.UpdatePostDTO;
import com.mindtalk.forum.modules.post.service.PostService;
import com.mindtalk.forum.modules.post.vo.PostDetailVO;
import com.mindtalk.forum.modules.post.vo.PostVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 帖子接口
 */
@Tag(name = "帖子管理")
@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @Operation(summary = "分页查询帖子列表")
    @GetMapping
    public Result<PageResult<PostVO>> list(@Valid PostQueryDTO query) {
        Long currentUserId = getCurrentUserIdOrNull();
        return Result.ok(postService.getPostPage(query, currentUserId));
    }

    @Operation(summary = "热门帖子")
    @GetMapping("/hot")
    public Result<List<PostVO>> hot(@RequestParam(defaultValue = "10") int limit) {
        return Result.ok(postService.getHotPosts(limit));
    }

    @Operation(summary = "关注动态流")
    @GetMapping("/following")
    public Result<PageResult<PostVO>> following(@Valid PostQueryDTO query) {
        Long userId = getCurrentUserId();
        query.setFollowingUserId(userId);
        return Result.ok(postService.getPostPage(query, userId));
    }

    @Operation(summary = "个性化推荐")
    @GetMapping("/recommended")
    public Result<List<PostVO>> recommended(@RequestParam(defaultValue = "10") int limit) {
        Long userId = getCurrentUserIdOrNull();
        if (userId == null) {
            return Result.ok(postService.getHotPosts(limit));
        }
        return Result.ok(postService.getRecommendedPosts(userId, limit));
    }

    @Operation(summary = "排行榜（周榜/月榜）")
    @GetMapping("/ranking")
    public Result<List<PostVO>> ranking(
            @RequestParam(defaultValue = "weekly") String period,
            @RequestParam(defaultValue = "20") int limit) {
        return Result.ok(postService.getRankingPosts(period, limit));
    }

    @Operation(summary = "相似帖子推荐")
    @GetMapping("/{id}/similar")
    public Result<List<PostVO>> similar(@PathVariable Long id,
                                         @RequestParam(defaultValue = "5") int limit) {
        return Result.ok(postService.getSimilarPosts(id, limit));
    }

    @Operation(summary = "帖子详情")
    @GetMapping("/{id}")
    public Result<PostDetailVO> detail(@PathVariable Long id) {
        Long currentUserId = getCurrentUserIdOrNull();
        return Result.ok(postService.getPostDetail(id, currentUserId));
    }

    @Operation(summary = "发帖")
    @PostMapping
    public Result<PostDetailVO> create(@Valid @RequestBody CreatePostDTO dto) {
        Long userId = getCurrentUserId();
        return Result.ok(postService.createPost(userId, dto));
    }

    @Operation(summary = "编辑帖子")
    @PutMapping("/{id}")
    public Result<PostDetailVO> update(@PathVariable Long id,
                                        @Valid @RequestBody UpdatePostDTO dto) {
        Long userId = getCurrentUserId();
        return Result.ok(postService.updatePost(userId, id, dto));
    }

    @Operation(summary = "删除帖子")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        postService.deletePost(userId, id);
        return Result.ok();
    }

    @Operation(summary = "点赞/取消点赞")
    @PostMapping("/{id}/like")
    public Result<Void> like(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        postService.likePost(userId, id);
        return Result.ok();
    }

    @Operation(summary = "收藏/取消收藏")
    @PostMapping("/{id}/collect")
    public Result<Void> collect(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        postService.collectPost(userId, id);
        return Result.ok();
    }

    @Operation(summary = "获取我的草稿")
    @GetMapping("/me/drafts")
    public Result<PageResult<PostVO>> myDrafts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = getCurrentUserId();
        return Result.ok(postService.getMyDrafts(userId, page, size));
    }

    @Operation(summary = "发布草稿")
    @PutMapping("/{id}/publish")
    public Result<PostDetailVO> publish(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        return Result.ok(postService.publishDraft(userId, id));
    }

    @Operation(summary = "记录浏览量")
    @PostMapping("/{id}/view")
    public Result<Void> view(@PathVariable Long id) {
        postService.recordView(id);
        return Result.ok();
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
