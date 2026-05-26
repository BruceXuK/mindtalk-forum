package com.mindtalk.forum.modules.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新增标签请求
 */
@Data
public class CreateTagDTO {

    @NotBlank(message = "标签名称不能为空")
    @Size(max = 50, message = "标签名称最长 50 个字符")
    private String name;

    @Size(max = 200, message = "描述最长 200 个字符")
    private String description;
}
