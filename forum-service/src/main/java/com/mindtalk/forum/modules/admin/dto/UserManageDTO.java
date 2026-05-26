package com.mindtalk.forum.modules.admin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class UserManageDTO {

    @NotNull(message = "状态不能为空")
    private Integer status;

    private List<Long> roleIds;
}
