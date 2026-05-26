package com.mindtalk.forum.config;

import com.mindtalk.common.constant.Constants;
import com.mindtalk.forum.common.utils.JwtUtils;
import com.mindtalk.forum.common.utils.RedisUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器
 * 优先信任 Gateway 传递的 X-User-Id / X-Role / X-Jwt-Id 请求头（避免重复解析 JWT）
 * 仅在直接访问（绕过 Gateway）时回退到完整 JWT 解析
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_ROLE = "X-Role";
    private static final String HEADER_JWT_ID = "X-Jwt-Id";

    private final JwtUtils jwtUtils;
    private final RedisUtils redisUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 优先使用 Gateway 传递的请求头（已校验过 JWT，无需重复解析）
        String gatewayUserId = request.getHeader(HEADER_USER_ID);
        if (StringUtils.hasText(gatewayUserId)) {
            String gatewayRole = request.getHeader(HEADER_ROLE);
            String gatewayJti = request.getHeader(HEADER_JWT_ID);

            // 检查黑名单
            if (gatewayJti != null && redisUtils.hasKey(Constants.AUTH_BLACKLIST + gatewayJti)) {
                log.debug("[JWT] Token 已被加入黑名单 jti={}", gatewayJti);
                filterChain.doFilter(request, response);
                return;
            }

            Long userId = Long.valueOf(gatewayUserId);
            String role = gatewayRole != null ? gatewayRole : "USER";

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userId, gatewayJti,  // JTI 存入 credentials，供登出黑名单使用
                            List.of(new SimpleGrantedAuthority("ROLE_" + role)));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);
            return;
        }

        // 回退：直接访问 forum-service（绕过 Gateway）时，完整解析 JWT
        String token = extractToken(request);
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        Claims claims = jwtUtils.validateToken(token);
        if (claims == null || !jwtUtils.isAccessToken(claims)) {
            filterChain.doFilter(request, response);
            return;
        }

        String jti = claims.getId();
        if (jti != null && redisUtils.hasKey(Constants.AUTH_BLACKLIST + jti)) {
            log.debug("[JWT] Token 已被加入黑名单 jti={}", jti);
            filterChain.doFilter(request, response);
            return;
        }

        Long userId = jwtUtils.getUserId(claims);
        String role = jwtUtils.getRole(claims);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userId, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        authentication.setDetails(claims);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
