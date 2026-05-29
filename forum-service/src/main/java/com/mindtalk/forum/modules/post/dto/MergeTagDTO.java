package com.mindtalk.forum.modules.post.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 标签合并请求
 */
@Data
public class MergeTagDTO {

    @NotEmpty(message = "源标签不能为空")
    private List<Long> sourceIds;

    @NotNull(message = "目标标签不能为空")
    private Long targetId;
}
