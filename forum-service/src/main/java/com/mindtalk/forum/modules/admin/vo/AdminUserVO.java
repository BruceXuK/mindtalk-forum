package com.mindtalk.forum.modules.admin.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserVO {

    private Long id;

    private String username;

    private String email;

    private String nickname;

    private String avatarUrl;

    private Integer status;

    private List<String> roles;

    private Long postCount;

    private Long commentCount;

    private LocalDateTime createTime;
}
