package com.mindtalk.forum.modules.comment.controller;

import com.mindtalk.common.model.PageResult;
import com.mindtalk.common.model.Result;
import com.mindtalk.forum.modules.comment.dto.CommentQueryDTO;
import com.mindtalk.forum.modules.comment.dto.CreateCommentDTO;
import com.mindtalk.forum.modules.comment.service.CommentService;
import com.mindtalk.forum.modules.comment.vo.CommentVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 评论接口
 */
@Tag(name = "评论管理")
@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "发表评论")
    @PostMapping
    public Result<CommentVO> create(@Valid @RequestBody CreateCommentDTO dto) {
        Long userId = getCurrentUserId();
        return Result.ok(commentService.createComment(userId, dto));
    }

    @Operation(summary = "分页查询评论列表")
    @GetMapping
    public Result<PageResult<CommentVO>> list(@Valid CommentQueryDTO query) {
        Long currentUserId = getCurrentUserIdOrNull();
        return Result.ok(commentService.getCommentPage(query, currentUserId));
    }

    @Operation(summary = "点赞/取消点赞评论")
    @PostMapping("/{id}/like")
    public Result<Void> like(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        commentService.likeComment(userId, id);
        return Result.ok();
    }

    @Operation(summary = "查询所有子回复")
    @GetMapping("/{id}/replies")
    public Result<List<CommentVO>> replies(@PathVariable Long id) {
        Long currentUserId = getCurrentUserIdOrNull();
        return Result.ok(commentService.getReplies(id, currentUserId));
    }

    @Operation(summary = "删除评论")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        commentService.deleteComment(userId, id);
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
