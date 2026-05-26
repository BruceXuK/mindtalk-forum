package com.mindtalk.forum.modules.admin.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleVO {

    private Long id;

    private String roleName;

    private String roleCode;

    private String description;

    private Integer sortOrder;

    private Integer status;

    private LocalDateTime createTime;
}
