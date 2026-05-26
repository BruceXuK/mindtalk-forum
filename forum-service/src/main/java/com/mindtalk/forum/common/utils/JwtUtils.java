package com.mindtalk.forum.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * JWT 工具类
 */
@Slf4j
@Component
public class JwtUtils {

    private final SecretKey secretKey;
    private final String secret;
    private final long accessTokenTtl;
    private final long refreshTokenTtl;

    public JwtUtils(@Value("${jwt.secret}") String secret,
                    @Value("${jwt.access-token-ttl:1800000}") long accessTokenTtl,
                    @Value("${jwt.refresh-token-ttl:604800000}") long refreshTokenTtl) {
        this.secret = secret;
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenTtl = accessTokenTtl;
        this.refreshTokenTtl = refreshTokenTtl;
    }

    // ──────────────────── 生成令牌 ────────────────────

    /**
     * 生成 Access Token
     */
    public String generateAccessToken(Long userId, String username, String role) {
        Date now = new Date();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", role)
                .claim("type", "access")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessTokenTtl))
                .signWith(secretKey)
                .compact();
    }

    /**
     * 生成 Refresh Token
     */
    public String generateRefreshToken(Long userId) {
        Date now = new Date();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(userId))
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + refreshTokenTtl))
                .signWith(secretKey)
                .compact();
    }

    // ──────────────────── 解析 ────────────────────

    /**
     * 解析令牌
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 校验令牌有效性，返回 Claims；无效时返回 null
     */
    public Claims validateToken(String token) {
        try {
            return parseToken(token);
        } catch (ExpiredJwtException e) {
            log.debug("[JWT] 令牌已过期: {}", e.getMessage());
            return null;
        } catch (JwtException e) {
            log.debug("[JWT] 令牌无效: {}", e.getMessage());
            return null;
        }
    }

    // ──────────────────── 提取信息 ────────────────────

    public Long getUserId(Claims claims) {
        return Long.valueOf(claims.getSubject());
    }

    public String getUsername(Claims claims) {
        return claims.get("username", String.class);
    }

    public String getRole(Claims claims) {
        return claims.get("role", String.class);
    }

    public String getTokenId(Claims claims) {
        return claims.getId();
    }

    public boolean isAccessToken(Claims claims) {
        return "access".equals(claims.get("type", String.class));
    }

    // ──────────────────── 启动检查 ────────────────────

    private static final String DEFAULT_SECRET = "mindtalk-forum-jwt-secret-key-2024";

    @PostConstruct
    public void checkSecret() {
        if (DEFAULT_SECRET.equals(secret)) {
            log.warn("⚠️  [JWT] 正在使用默认密钥，生产环境请通过 JWT_SECRET 环境变量设置强密钥！");
        }
    }

    // ──────────────────── TTL ────────────────────

    public long getAccessTokenTtl() {
        return accessTokenTtl;
    }

    public long getRefreshTokenTtl() {
        return refreshTokenTtl;
    }
}
