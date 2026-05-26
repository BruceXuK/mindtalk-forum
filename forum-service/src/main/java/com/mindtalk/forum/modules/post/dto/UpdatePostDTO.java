package com.mindtalk.forum.modules.post.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 编辑帖子请求
 */
@Data
public class UpdatePostDTO {

    @Size(min = 1, max = 200, message = "标题长度 1-200 个字符")
    private String title;

    private String content;

    private String contentText;

    private Long categoryId;

    private List<Long> tagIds;
}
