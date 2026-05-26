package com.mindtalk.common.constant;

/**
 * 全局常量
 */
public final class Constants {

    private Constants() {}

    // ──────────────────── 用户 ────────────────────

    /** 默认角色 */
    public static final String DEFAULT_ROLE = "USER";

    /** 管理员角色 */
    public static final String ROLE_ADMIN = "ADMIN";

    /** 版主角色 */
    public static final String ROLE_MODERATOR = "MODERATOR";

    /** 用户状态：正常 */
    public static final int USER_STATUS_NORMAL = 1;

    /** 用户状态：封禁 */
    public static final int USER_STATUS_BANNED = 0;

    // ──────────────────── 帖子 ────────────────────

    /** 帖子状态：草稿 */
    public static final int POST_STATUS_DRAFT = 0;

    /** 帖子状态：正常 */
    public static final int POST_STATUS_NORMAL = 1;

    /** 帖子状态：审核中 */
    public static final int POST_STATUS_AUDIT = 2;

    // ──────────────────── Redis Key 前缀 ────────────────────

    public static final String REDIS_PREFIX = "mindtalk:";
    public static final String USER_CACHE_KEY = REDIS_PREFIX + "user:info:";
    public static final String AUTH_ACCESS_TOKEN = REDIS_PREFIX + "auth:access_token:";
    public static final String AUTH_REFRESH_TOKEN = REDIS_PREFIX + "auth:refresh_token:";
    public static final String AUTH_BLACKLIST = REDIS_PREFIX + "auth:blacklist:";
    public static final String VERIFY_EMAIL = REDIS_PREFIX + "verify:email:";
    public static final String VERIFY_PHONE = REDIS_PREFIX + "verify:phone:";
    public static final String VERIFY_RATE = REDIS_PREFIX + "verify:rate:";

    // ──────────────────── RocketMQ Topic ────────────────────

    public static final String TOPIC_COMMENT_EVENT = "comment-event";
    public static final String TOPIC_LIKE_EVENT = "like-event";
    public static final String TOPIC_FOLLOW_EVENT = "follow-event";
    public static final String TOPIC_SEARCH_SYNC = "search-sync-event";
    public static final String TOPIC_VIEW_COUNT = "view-count-event";

    // ──────────────────── 通知类型 ────────────────────

    public static final String NOTIFY_LIKE = "LIKE";
    public static final String NOTIFY_COMMENT = "COMMENT";
    public static final String NOTIFY_FOLLOW = "FOLLOW";
    public static final String NOTIFY_MENTION = "MENTION";
    public static final String NOTIFY_SYSTEM = "SYSTEM";

    // ──────────────────── 逻辑删除 ────────────────────

    public static final int NOT_DELETED = 0;
    public static final int DELETED = 1;
}
