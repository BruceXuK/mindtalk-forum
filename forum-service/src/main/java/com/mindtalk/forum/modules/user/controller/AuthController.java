package com.mindtalk.forum.modules.user.controller;

import com.mindtalk.common.model.Result;
import com.mindtalk.forum.common.annotation.RateLimit;
import com.mindtalk.forum.modules.user.dto.LoginDTO;
import com.mindtalk.forum.modules.user.dto.RefreshTokenDTO;
import com.mindtalk.forum.modules.user.dto.RegisterDTO;
import com.mindtalk.forum.modules.user.service.UserService;
import com.mindtalk.forum.modules.user.vo.LoginResultVO;
import com.mindtalk.forum.modules.user.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 认证接口
 */
@Tag(name = "认证管理")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    @RateLimit(key = "rate:register:", window = 60, maxRequests = 5, message = "注册请求过于频繁，请 60 秒后再试")
    public Result<UserVO> register(@Valid @RequestBody RegisterDTO dto) {
        return Result.ok(userService.register(dto));
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    @RateLimit(key = "rate:login:", window = 60, maxRequests = 10, message = "登录请求过于频繁，请 60 秒后再试")
    public Result<LoginResultVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.ok(userService.login(dto));
    }

    @Operation(summary = "刷新令牌")
    @PostMapping("/refresh")
    public Result<LoginResultVO> refresh(@Valid @RequestBody RefreshTokenDTO dto) {
        return Result.ok(userService.refreshToken(dto));
    }

    @Operation(summary = "登出")
    @PostMapping("/logout")
    public Result<Void> logout() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long userId) {
            String jti = null;
            java.util.Date exp = null;
            // 优先从 Claims（直接访问 forum-service）
            if (auth.getDetails() instanceof io.jsonwebtoken.Claims claims) {
                jti = claims.getId();
                exp = claims.getExpiration();
            }
            // 回退：Gateway 传递的 JTI 字符串（无过期时间，黑名单用默认 TTL）
            if (jti == null && auth.getCredentials() instanceof String credentialsStr) {
                jti = credentialsStr;
            }
            userService.logout(userId, jti, exp);
        }
        return Result.ok();
    }
}
