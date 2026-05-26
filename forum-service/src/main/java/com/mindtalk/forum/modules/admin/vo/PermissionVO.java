package com.mindtalk.forum.modules.admin.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionVO {

    private Long id;

    private Long parentId;

    private String permName;

    private String permCode;

    private Integer permType;

    private String path;

    private String icon;

    private Integer sortOrder;
}
