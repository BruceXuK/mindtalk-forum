package com.mindtalk.forum.modules.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateReportDTO {

    @NotBlank(message = "举报目标类型不能为空")
    private String targetType;

    @NotNull(message = "举报目标 ID 不能为空")
    private Long targetId;

    @NotBlank(message = "举报原因不能为空")
    @Size(max = 50, message = "举报原因最长 50 个字符")
    private String reason;

    @Size(max = 500, message = "描述最长 500 个字符")
    private String description;
}
