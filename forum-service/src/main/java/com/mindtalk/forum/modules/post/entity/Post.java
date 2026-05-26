package com.mindtalk.forum.modules.post.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 帖子实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("posts")
public class Post {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 标题 */
    private String title;

    /** 正文（Markdown/HTML） */
    private String content;

    /** 纯文本（ES 搜索用） */
    private String contentText;

    /** 作者 ID */
    private Long authorId;

    /** 分类 ID */
    private Long categoryId;

    /** 是否置顶 */
    private Boolean isPinned;

    /** 是否加精 */
    private Boolean isFeatured;

    /** 浏览量 */
    private Integer viewCount;

    /** 点赞数（冗余） */
    private Integer likeCount;

    /** 评论数（冗余） */
    private Integer commentCount;

    /** 收藏数（冗余） */
    private Integer collectCount;

    /** 状态：1-正常 2-审核中 */
    private Integer status;

    /** 置顶时间 */
    private LocalDateTime pinnedAt;

    /** 置顶过期时间（NULL 表示永久） */
    private LocalDateTime pinnedUntil;

    /** 加精时间 */
    private LocalDateTime featuredAt;

    /** 加精过期时间（NULL 表示永久） */
    private LocalDateTime featuredUntil;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
