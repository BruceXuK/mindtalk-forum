package com.mindtalk.forum.modules.comment.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 点赞实体（通用：POST / COMMENT）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("likes")
public class Like {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 点赞用户 ID */
    private Long userId;

    /** 目标类型：POST / COMMENT */
    private String targetType;

    /** 目标 ID */
    private Long targetId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
