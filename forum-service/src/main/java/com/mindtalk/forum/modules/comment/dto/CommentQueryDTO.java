package com.mindtalk.forum.modules.comment.dto;

import com.mindtalk.common.model.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 评论分页查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CommentQueryDTO extends PageRequest {

    /** 帖子 ID（必填） */
    private Long postId;

    /** 排序字段: create_time / like_count */
    private String orderBy;
}
