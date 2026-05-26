package com.mindtalk.forum.modules.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 发表评论请求
 */
@Data
public class CreateCommentDTO {

    @NotNull(message = "帖子 ID 不能为空")
    private Long postId;

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 5000, message = "评论内容最长 5000 个字符")
    private String content;

    /** 父评论 ID（回复时传入，一级评论不传） */
    private Long parentId;

    /** 被回复的用户 ID */
    private Long replyToId;

    /** @提及的用户 ID 列表 */
    private List<Long> mentionedUserIds;
}
