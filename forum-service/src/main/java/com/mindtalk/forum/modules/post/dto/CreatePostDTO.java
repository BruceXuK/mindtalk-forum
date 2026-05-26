package com.mindtalk.forum.modules.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 发帖请求
 */
@Data
public class CreatePostDTO {

    @NotBlank(message = "标题不能为空")
    @Size(min = 1, max = 200, message = "标题长度 1-200 个字符")
    private String title;

    @NotBlank(message = "内容不能为空")
    private String content;

    /** 纯文本内容（用于 ES 搜索，前端可传 Markdown 剥离后的文本） */
    private String contentText;

    /** 分类 ID */
    private Long categoryId;

    /** 标签 ID 列表 */
    private List<Long> tagIds;

    /** 状态：0-草稿 1-发布（默认发布） */
    private Integer status;

    /** @提及的用户 ID 列表 */
    private List<Long> mentionedUserIds;
}
