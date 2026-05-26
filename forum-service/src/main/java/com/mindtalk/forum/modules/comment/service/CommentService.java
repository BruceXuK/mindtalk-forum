package com.mindtalk.forum.modules.comment.service;

import com.mindtalk.common.model.PageResult;
import com.mindtalk.forum.modules.comment.dto.CommentQueryDTO;
import com.mindtalk.forum.modules.comment.dto.CreateCommentDTO;
import com.mindtalk.forum.modules.comment.vo.CommentVO;

import java.util.List;

/**
 * 评论服务接口
 */
public interface CommentService {

    /** 发表评论/回复 */
    CommentVO createComment(Long userId, CreateCommentDTO dto);

    /** 分页查询评论 */
    PageResult<CommentVO> getCommentPage(CommentQueryDTO query, Long currentUserId);

    /** 查询父评论的所有子回复 */
    List<CommentVO> getReplies(Long parentId, Long currentUserId);

    /** 点赞/取消点赞评论 */
    void likeComment(Long userId, Long commentId);

    /** 删除评论 */
    void deleteComment(Long userId, Long commentId);
}
