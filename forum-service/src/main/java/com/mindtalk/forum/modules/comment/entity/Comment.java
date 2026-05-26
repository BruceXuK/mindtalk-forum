package com.mindtalk.forum.modules.comment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 评论实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("comments")
public class Comment {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属帖子 ID */
    private Long postId;

    /** 评论者 ID */
    private Long userId;

    /** 父评论 ID（NULL 表示一级评论） */
    private Long parentId;

    /** 被回复的用户 ID */
    private Long replyToId;

    /** 评论内容 */
    private String content;

    /** 点赞数（冗余） */
    private Integer likeCount;

    /** 状态：1-正常 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
