package com.mindtalk.forum.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 刷新令牌请求
 */
@Data
public class RefreshTokenDTO {

    @NotBlank(message = "RefreshToken 不能为空")
    private String refreshToken;
}
