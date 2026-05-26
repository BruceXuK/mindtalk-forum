package com.mindtalk.forum.modules.admin.vo;

import com.mindtalk.forum.modules.user.vo.UserVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportVO {

    private Long id;

    private UserVO reporter;

    private String targetType;

    private Long targetId;

    private String reason;

    private String description;

    private Integer status;

    private UserVO handler;

    private String handleResult;

    private LocalDateTime handleTime;

    private LocalDateTime createTime;
}
