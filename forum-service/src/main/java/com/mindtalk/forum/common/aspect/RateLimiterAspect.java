package com.mindtalk.forum.common.aspect;

import com.mindtalk.common.exception.BusinessException;
import com.mindtalk.forum.common.annotation.RateLimit;
import com.mindtalk.forum.common.utils.RedisUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;

/**
 * Redis 滑动窗口限流切面
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimiterAspect {

    private final RedisUtils redisUtils;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String key = buildKey(rateLimit);
        long current = redisUtils.increment(key);

        // 首次访问时设置过期时间
        if (current == 1) {
            redisUtils.expire(key, rateLimit.window(), TimeUnit.SECONDS);
        }

        if (current > rateLimit.maxRequests()) {
            log.warn("[限流] key={} count={} limit={}", key, current, rateLimit.maxRequests());
            throw BusinessException.tooManyRequests(rateLimit.message());
        }

        return joinPoint.proceed();
    }

    private String buildKey(RateLimit rateLimit) {
        String identifier = "anonymous";

        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                // 优先使用用户 ID
                String userId = request.getHeader("X-User-Id");
                if (userId != null && !userId.isEmpty()) {
                    identifier = "user:" + userId;
                } else {
                    // 回退到 IP
                    String ip = request.getHeader("X-Forwarded-For");
                    if (ip == null || ip.isEmpty()) {
                        ip = request.getHeader("X-Real-IP");
                    }
                    if (ip == null || ip.isEmpty()) {
                        ip = request.getRemoteAddr();
                    }
                    identifier = "ip:" + (ip != null ? ip : "unknown");
                }
            }
        } catch (Exception e) {
            // 非 Web 上下文，使用默认标识
        }

        return rateLimit.key() + identifier;
    }
}
