package com.mindtalk.forum.modules.user.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录结果 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResultVO {

    private String accessToken;

    private String refreshToken;

    private Long expiresIn;

    private UserVO user;
}
