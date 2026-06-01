package com.mindtalk.forum.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 自定义业务指标收集器
 */
@Slf4j
@Component
public class BusinessMetrics {

    private final MeterRegistry meterRegistry;

    private final Counter loginCounter;
    private final Counter loginFailedCounter;
    private final Counter registerCounter;
    private final Counter postCreatedCounter;
    private final Counter commentCreatedCounter;
    private final Counter searchCounter;

    private final AtomicLong onlineUsers = new AtomicLong(0);
    private final AtomicLong activePosts = new AtomicLong(0);

    public BusinessMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.loginCounter = Counter.builder("mindtalk.auth.login")
                .description("登录成功次数")
                .register(meterRegistry);

        this.loginFailedCounter = Counter.builder("mindtalk.auth.login_failed")
                .description("登录失败次数")
                .register(meterRegistry);

        this.registerCounter = Counter.builder("mindtalk.auth.register")
                .description("注册成功次数")
                .register(meterRegistry);

        this.postCreatedCounter = Counter.builder("mindtalk.post.created")
                .description("帖子发布次数")
                .register(meterRegistry);

        this.commentCreatedCounter = Counter.builder("mindtalk.comment.created")
                .description("评论发布次数")
                .register(meterRegistry);

        this.searchCounter = Counter.builder("mindtalk.search.total")
                .description("搜索请求次数")
                .register(meterRegistry);

        Gauge.builder("mindtalk.users.online", onlineUsers, AtomicLong::get)
                .description("当前在线用户数")
                .register(meterRegistry);

        Gauge.builder("mindtalk.posts.active", activePosts, AtomicLong::get)
                .description("活跃帖子数（24h内有互动的帖子）")
                .register(meterRegistry);

        log.info("[Metrics] 自定义业务指标初始化完成");
    }

    public void recordLoginSuccess() {
        loginCounter.increment();
        onlineUsers.incrementAndGet();
    }

    public void recordLoginFailed() {
        loginFailedCounter.increment();
    }

    public void recordRegister() {
        registerCounter.increment();
    }

    public void recordPostCreated() {
        postCreatedCounter.increment();
    }

    public void recordCommentCreated() {
        commentCreatedCounter.increment();
    }

    public void recordSearch() {
        searchCounter.increment();
    }

    public void userLogout() {
        onlineUsers.decrementAndGet();
    }

    public void setOnlineUsers(long count) {
        onlineUsers.set(count);
    }

    public void setActivePosts(long count) {
        activePosts.set(count);
    }
}
