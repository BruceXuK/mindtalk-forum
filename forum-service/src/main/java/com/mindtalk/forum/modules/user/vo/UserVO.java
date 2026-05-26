package com.mindtalk.forum.modules.user.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户信息 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVO {

    private Long id;

    private String username;

    private String email;

    private String nickname;

    private String avatarUrl;

    private String bio;

    private Integer gender;

    private String location;

    private Integer followingCount;

    private Integer followerCount;

    private Integer postCount;

    private LocalDateTime createTime;
}
