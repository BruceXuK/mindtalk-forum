package com.mindtalk.forum.modules.admin.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindtalk.forum.modules.admin.annotation.AdminLog;
import com.mindtalk.forum.modules.admin.mapper.AdminLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AdminLogAspect {

    private final AdminLogMapper adminLogMapper;
    private final ObjectMapper objectMapper;

    @Around("@annotation(com.mindtalk.forum.modules.admin.annotation.AdminLog)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        Object result = joinPoint.proceed();

        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            AdminLog adminLogAnno = method.getAnnotation(AdminLog.class);

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            Long adminId = auth != null ? Long.valueOf(auth.getPrincipal().toString()) : null;

            String ip = "";
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                ip = request.getHeader("X-Forwarded-For");
                if (ip == null || ip.isEmpty()) {
                    ip = request.getRemoteAddr();
                }
            }

            String detail = objectMapper.writeValueAsString(joinPoint.getArgs());

            com.mindtalk.forum.modules.admin.entity.AdminLog logEntry = com.mindtalk.forum.modules.admin.entity.AdminLog.builder()
                    .adminId(adminId)
                    .action(adminLogAnno.action())
                    .targetType(adminLogAnno.targetType())
                    .detail(detail)
                    .ip(ip)
                    .build();
            adminLogMapper.insert(logEntry);
        } catch (Exception e) {
            log.warn("[操作日志] 记录失败", e);
        }

        return result;
    }
}
