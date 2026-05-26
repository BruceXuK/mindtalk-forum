package com.mindtalk.forum.modules.admin.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionTreeVO {

    private Long id;

    private Long parentId;

    private String permName;

    private String permCode;

    private Integer permType;

    private String path;

    private String icon;

    private Integer sortOrder;

    private List<PermissionTreeVO> children;
}
