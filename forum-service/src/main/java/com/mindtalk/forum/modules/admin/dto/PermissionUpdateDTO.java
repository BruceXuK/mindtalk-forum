package com.mindtalk.forum.modules.admin.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class PermissionUpdateDTO {

    @NotEmpty(message = "权限列表不能为空")
    private List<Long> permissionIds;
}
