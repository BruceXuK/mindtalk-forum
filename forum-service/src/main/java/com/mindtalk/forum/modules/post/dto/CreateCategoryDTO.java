package com.mindtalk.forum.modules.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新增分类请求
 */
@Data
public class CreateCategoryDTO {

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 50, message = "分类名称最长 50 个字符")
    private String name;

    @Size(max = 200, message = "描述最长 200 个字符")
    private String description;

    @Size(max = 200, message = "图标 URL 最长 200 个字符")
    private String icon;

    private Integer sortOrder;
}
