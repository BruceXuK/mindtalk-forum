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
public class AdminLogVO {

    private Long id;

    private Long adminId;

    private String adminName;

    private String action;

    private String targetType;

    private Long targetId;

    private String detail;

    private String ip;

    private LocalDateTime createTime;
}
