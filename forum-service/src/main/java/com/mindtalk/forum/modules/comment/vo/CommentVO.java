package com.mindtalk.forum.modules.comment.vo;

import com.mindtalk.forum.modules.user.vo.UserVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentVO {

    private Long id;

    private Long postId;

    /** 评论者 */
    private UserVO user;

    /** 父评论 ID */
    private Long parentId;

    /** 被回复的用户 */
    private UserVO replyTo;

    /** 评论内容 */
    private String content;

    /** 点赞数 */
    private Integer likeCount;

    /** 当前用户是否已点赞 */
    private Boolean isLiked;

    /** 子回复列表（仅一级评论有） */
    private List<CommentVO> replies;

    /** 回复总数 */
    private Integer replyCount;

    private LocalDateTime createTime;
}
