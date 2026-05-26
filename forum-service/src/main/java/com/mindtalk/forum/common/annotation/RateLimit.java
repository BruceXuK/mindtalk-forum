package com.mindtalk.forum.common.annotation;

import java.lang.annotation.*;

/**
 * 接口限流注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /** 限流 key 前缀 */
    String key() default "rate:";

    /** 时间窗口（秒） */
    int window() default 60;

    /** 窗口内最大请求数 */
    int maxRequests() default 10;

    /** 限流提示 */
    String message() default "请求过于频繁，请稍后再试";
}
