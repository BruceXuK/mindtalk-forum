package com.mindtalk.forum.modules.user.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户主页 VO（对外展示）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileVO {

    private Long id;

    private String username;

    private String nickname;

    private String avatarUrl;

    private String bio;

    private Integer gender;

    private String location;

    private Integer followingCount;

    private Integer followerCount;

    private Integer postCount;

    /** 当前用户是否已关注此人 */
    private Boolean isFollowing;

    private LocalDateTime createTime;
}
