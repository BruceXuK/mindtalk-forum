package com.mindtalk.forum.modules.series.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateSeriesDTO {

    @NotBlank(message = "系列标题不能为空")
    @Size(min = 1, max = 200)
    private String title;

    private String description;

    private String coverUrl;

    /** 初始帖子 ID 列表（可选） */
    private List<Long> postIds;
}
