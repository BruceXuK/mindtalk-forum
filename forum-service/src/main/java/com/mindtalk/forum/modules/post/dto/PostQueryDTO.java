package com.mindtalk.forum.modules.post.dto;

import com.mindtalk.common.model.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 帖子分页查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PostQueryDTO extends PageRequest {

    /** 分类 ID 筛选 */
    private Long categoryId;

    /** 标签 ID 筛选 */
    private Long tagId;

    /** 关键词搜索 */
    private String keyword;

    /** 帖子状态 */
    private Integer status;

    /** 排序字段: create_time / view_count / like_count / comment_count */
    private String orderBy;

    /** 作者 ID 筛选 */
    private Long userId;

    /** 关注流：只显示该用户已关注作者的帖子 */
    private Long followingUserId;
}
