package com.mindtalk.forum.modules.admin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PostAuditDTO {

    @NotNull(message = "状态不能为空")
    private Integer status;

    private String rejectReason;
}
