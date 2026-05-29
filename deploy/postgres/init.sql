-- ============================================================
-- MindTalk（思享论坛）PostgreSQL 初始化 DDL
-- ============================================================

-- 创建数据库（如需要，由 DBA 手动执行）
-- CREATE DATABASE mindtalk WITH ENCODING 'UTF8' LC_COLLATE 'en_US.UTF-8' LC_CTYPE 'en_US.UTF-8';

-- ============================================================
-- 扩展
-- ============================================================
CREATE EXTENSION IF NOT EXISTS "pg_trgm";       -- 模糊搜索
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";     -- UUID 生成

-- ============================================================
-- 1. 角色表
-- ============================================================
CREATE TABLE roles (
    id              BIGSERIAL       PRIMARY KEY,
    role_name       VARCHAR(50)     NOT NULL,
    role_code       VARCHAR(50)     NOT NULL,
    description     VARCHAR(200),
    sort_order      INT             DEFAULT 0,
    status          SMALLINT        NOT NULL DEFAULT 1,       -- 1-启用 0-禁用
    create_time     TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_time     TIMESTAMP       NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0        -- 0-正常 1-已删除
);

COMMENT ON TABLE  roles               IS '角色表';
COMMENT ON COLUMN roles.role_name      IS '角色名称';
COMMENT ON COLUMN roles.role_code      IS '角色编码';
COMMENT ON COLUMN roles.description    IS '角色描述';
COMMENT ON COLUMN roles.sort_order     IS '排序号';
COMMENT ON COLUMN roles.status         IS '状态：1-启用 0-禁用';
COMMENT ON COLUMN roles.deleted        IS '逻辑删除：0-正常 1-已删除';

CREATE UNIQUE INDEX uk_roles_code ON roles (role_code) WHERE deleted = 0;

-- ============================================================
-- 2. 权限表
-- ============================================================
CREATE TABLE permissions (
    id              BIGSERIAL       PRIMARY KEY,
    parent_id       BIGINT          DEFAULT 0,                -- 父权限 ID，0 表示顶级
    perm_name       VARCHAR(100)    NOT NULL,
    perm_code       VARCHAR(100)    NOT NULL,
    perm_type       SMALLINT        NOT NULL DEFAULT 1,       -- 1-菜单 2-按钮 3-接口
    path            VARCHAR(200),                             -- 路由路径/接口路径
    icon            VARCHAR(100),                             -- 菜单图标
    sort_order      INT             DEFAULT 0,
    status          SMALLINT        NOT NULL DEFAULT 1,
    create_time     TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_time     TIMESTAMP       NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0
);

COMMENT ON TABLE  permissions             IS '权限表';
COMMENT ON COLUMN permissions.parent_id   IS '父权限 ID';
COMMENT ON COLUMN permissions.perm_name   IS '权限名称';
COMMENT ON COLUMN permissions.perm_code   IS '权限编码';
COMMENT ON COLUMN permissions.perm_type   IS '权限类型：1-菜单 2-按钮 3-接口';
COMMENT ON COLUMN permissions.path        IS '路由路径或接口路径';
COMMENT ON COLUMN permissions.deleted     IS '逻辑删除：0-正常 1-已删除';

CREATE UNIQUE INDEX uk_permissions_code ON permissions (perm_code) WHERE deleted = 0;
CREATE INDEX idx_permissions_parent ON permissions (parent_id) WHERE deleted = 0;

-- ============================================================
-- 3. 角色权限关联表
-- ============================================================
CREATE TABLE role_permissions (
    id              BIGSERIAL       PRIMARY KEY,
    role_id         BIGINT          NOT NULL,
    permission_id   BIGINT          NOT NULL,
    create_time     TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_time     TIMESTAMP       NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0
);

COMMENT ON TABLE role_permissions IS '角色权限关联表';

CREATE UNIQUE INDEX uk_role_perm ON role_permissions (role_id, permission_id) WHERE deleted = 0;
CREATE INDEX idx_rp_role ON role_permissions (role_id) WHERE deleted = 0;
CREATE INDEX idx_rp_perm ON role_permissions (permission_id) WHERE deleted = 0;

-- ============================================================
-- 4. 用户表
-- ============================================================
CREATE TABLE users (
    id              BIGSERIAL       PRIMARY KEY,
    username        VARCHAR(50)     NOT NULL,
    email           VARCHAR(100),
    phone           VARCHAR(20),
    password_hash   VARCHAR(255)    NOT NULL,
    nickname        VARCHAR(50),
    avatar_url      VARCHAR(500),
    bio             VARCHAR(500),
    gender          SMALLINT        DEFAULT 0,                -- 0-未知 1-男 2-女
    birthday        DATE,
    location        VARCHAR(100),
    status          SMALLINT        NOT NULL DEFAULT 1,       -- 1-正常 0-封禁
    last_login_at   TIMESTAMP,
    last_login_ip   VARCHAR(45),
    create_time     TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_time     TIMESTAMP       NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0
);

COMMENT ON TABLE  users                IS '用户表';
COMMENT ON COLUMN users.username       IS '用户名';
COMMENT ON COLUMN users.email          IS '邮箱';
COMMENT ON COLUMN users.phone          IS '手机号';
COMMENT ON COLUMN users.password_hash  IS 'BCrypt 密码哈希';
COMMENT ON COLUMN users.nickname       IS '昵称';
COMMENT ON COLUMN users.avatar_url     IS '头像 URL';
COMMENT ON COLUMN users.bio            IS '个人简介';
COMMENT ON COLUMN users.gender         IS '性别：0-未知 1-男 2-女';
COMMENT ON COLUMN users.status         IS '状态：1-正常 0-封禁';
COMMENT ON COLUMN users.deleted        IS '逻辑删除：0-正常 1-已删除';

-- 唯一约束（排除已删除记录）
CREATE UNIQUE INDEX uk_users_username ON users (username) WHERE deleted = 0;
CREATE UNIQUE INDEX uk_users_email    ON users (email)    WHERE deleted = 0 AND email IS NOT NULL;
CREATE UNIQUE INDEX uk_users_phone    ON users (phone)    WHERE deleted = 0 AND phone IS NOT NULL;

-- 查询索引
CREATE INDEX idx_users_status     ON users (status)     WHERE deleted = 0;
CREATE INDEX idx_users_nickname   ON users (nickname)   WHERE deleted = 0;
CREATE INDEX idx_users_create_time ON users (create_time DESC) WHERE deleted = 0;

-- ============================================================
-- 5. 用户角色关联表
-- ============================================================
CREATE TABLE user_roles (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    role_id         BIGINT          NOT NULL,
    create_time     TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_time     TIMESTAMP       NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0
);

COMMENT ON TABLE user_roles IS '用户角色关联表';

CREATE UNIQUE INDEX uk_user_role ON user_roles (user_id, role_id) WHERE deleted = 0;
CREATE INDEX idx_ur_user ON user_roles (user_id) WHERE deleted = 0;
CREATE INDEX idx_ur_role ON user_roles (role_id) WHERE deleted = 0;

-- ============================================================
-- 6. 分类表
-- ============================================================
CREATE TABLE categories (
    id              BIGSERIAL       PRIMARY KEY,
    name            VARCHAR(50)     NOT NULL,
    description     VARCHAR(200),
    icon            VARCHAR(200),                             -- 图标 URL 或 icon class
    sort_order      INT             DEFAULT 0,
    post_count      INT             DEFAULT 0,                -- 冗余：帖子数
    status          SMALLINT        NOT NULL DEFAULT 1,       -- 1-启用 0-禁用
    create_time     TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_time     TIMESTAMP       NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0
);

COMMENT ON TABLE  categories              IS '分类表';
COMMENT ON COLUMN categories.name         IS '分类名称';
COMMENT ON COLUMN categories.description  IS '分类描述';
COMMENT ON COLUMN categories.post_count   IS '帖子数（冗余字段）';
COMMENT ON COLUMN categories.status       IS '状态：1-启用 0-禁用';
COMMENT ON COLUMN categories.deleted      IS '逻辑删除：0-正常 1-已删除';

CREATE UNIQUE INDEX uk_categories_name ON categories (name) WHERE deleted = 0;
CREATE INDEX idx_categories_sort ON categories (sort_order) WHERE deleted = 0;

-- ============================================================
-- 7. 标签表
-- ============================================================
CREATE TABLE tags (
    id              BIGSERIAL       PRIMARY KEY,
    name            VARCHAR(50)     NOT NULL,
    description     VARCHAR(200),
    post_count      INT             DEFAULT 0,                -- 冗余：关联帖子数
    status          SMALLINT        NOT NULL DEFAULT 1,       -- 1-启用 0-禁用
    create_time     TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_time     TIMESTAMP       NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0
);

COMMENT ON TABLE  tags              IS '标签表';
COMMENT ON COLUMN tags.name         IS '标签名称';
COMMENT ON COLUMN tags.post_count   IS '关联帖子数（冗余字段）';
COMMENT ON COLUMN tags.deleted      IS '逻辑删除：0-正常 1-已删除';

CREATE UNIQUE INDEX uk_tags_name ON tags (name) WHERE deleted = 0;
CREATE INDEX idx_tags_post_count ON tags (post_count DESC) WHERE deleted = 0;

-- ============================================================
-- 8. 帖子表
-- ============================================================
CREATE TABLE posts (
    id              BIGSERIAL       PRIMARY KEY,
    title           VARCHAR(200)    NOT NULL,
    content         TEXT            NOT NULL,
    content_text    TEXT,                                     -- 纯文本（搜索用）
    author_id       BIGINT          NOT NULL,
    category_id     BIGINT,
    is_pinned       BOOLEAN         DEFAULT FALSE,
    is_featured     BOOLEAN         DEFAULT FALSE,
    view_count      INT             DEFAULT 0,
    like_count      INT             DEFAULT 0,                -- 冗余
    comment_count   INT             DEFAULT 0,                -- 冗余
    collect_count   INT             DEFAULT 0,                -- 冗余
    status          SMALLINT        NOT NULL DEFAULT 1,       -- 1-正常 2-审核中
    pinned_at       TIMESTAMP,                                -- 置顶时间
    featured_at     TIMESTAMP,                                -- 加精时间
    pinned_until    TIMESTAMP,                                -- 置顶过期时间，NULL 表示永久
    featured_until  TIMESTAMP,                                -- 加精过期时间，NULL 表示永久
    create_time     TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_time     TIMESTAMP       NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0
);

COMMENT ON TABLE  posts                IS '帖子表';
COMMENT ON COLUMN posts.title          IS '帖子标题';
COMMENT ON COLUMN posts.content        IS '帖子正文（富文本/HTML）';
COMMENT ON COLUMN posts.content_text   IS '纯文本（Elasticsearch 索引用）';
COMMENT ON COLUMN posts.author_id      IS '作者 ID';
COMMENT ON COLUMN posts.category_id    IS '分类 ID';
COMMENT ON COLUMN posts.is_pinned      IS '是否置顶';
COMMENT ON COLUMN posts.is_featured    IS '是否加精';
COMMENT ON COLUMN posts.view_count     IS '浏览量（冗余）';
COMMENT ON COLUMN posts.like_count     IS '点赞数（冗余）';
COMMENT ON COLUMN posts.comment_count  IS '评论数（冗余）';
COMMENT ON COLUMN posts.collect_count  IS '收藏数（冗余）';
COMMENT ON COLUMN posts.status         IS '状态：1-正常 2-审核中';
COMMENT ON COLUMN posts.pinned_until   IS '置顶过期时间，NULL 表示永久置顶';
COMMENT ON COLUMN posts.featured_until IS '加精过期时间，NULL 表示永久加精';
COMMENT ON COLUMN posts.deleted        IS '逻辑删除：0-正常 1-已删除';

-- 查询索引
CREATE INDEX idx_posts_author       ON posts (author_id)        WHERE deleted = 0;
CREATE INDEX idx_posts_category     ON posts (category_id)      WHERE deleted = 0;
CREATE INDEX idx_posts_create_time  ON posts (create_time DESC) WHERE deleted = 0;
CREATE INDEX idx_posts_pinned       ON posts (is_pinned, pinned_at DESC) WHERE deleted = 0;
CREATE INDEX idx_posts_featured     ON posts (is_featured, featured_at DESC) WHERE deleted = 0;
CREATE INDEX idx_posts_view         ON posts (view_count DESC)  WHERE deleted = 0;
CREATE INDEX idx_posts_like         ON posts (like_count DESC)  WHERE deleted = 0;
CREATE INDEX idx_posts_comment      ON posts (comment_count DESC) WHERE deleted = 0;
CREATE INDEX idx_posts_status       ON posts (status)           WHERE deleted = 0;

-- 复合索引：首页按时间查询某分类下的帖子
CREATE INDEX idx_posts_cat_time     ON posts (category_id, create_time DESC) WHERE deleted = 0;
-- 复合索引：按作者+时间查询
CREATE INDEX idx_posts_author_time  ON posts (author_id, create_time DESC) WHERE deleted = 0;

-- ============================================================
-- 9. 帖子标签关联表
-- ============================================================
CREATE TABLE post_tags (
    id              BIGSERIAL       PRIMARY KEY,
    post_id         BIGINT          NOT NULL,
    tag_id          BIGINT          NOT NULL,
    create_time     TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_time     TIMESTAMP       NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0
);

COMMENT ON TABLE post_tags IS '帖子标签关联表';

CREATE UNIQUE INDEX uk_post_tag ON post_tags (post_id, tag_id) WHERE deleted = 0;
CREATE INDEX idx_pt_post ON post_tags (post_id) WHERE deleted = 0;
CREATE INDEX idx_pt_tag  ON post_tags (tag_id)  WHERE deleted = 0;

-- ============================================================
-- 10. 评论表
-- ============================================================
CREATE TABLE comments (
    id              BIGSERIAL       PRIMARY KEY,
    post_id         BIGINT          NOT NULL,
    user_id         BIGINT          NOT NULL,
    parent_id       BIGINT,                                   -- 父评论 ID（NULL 表示一级评论）
    reply_to_id     BIGINT,                                   -- 被回复的用户 ID
    content         TEXT            NOT NULL,
    like_count      INT             DEFAULT 0,                -- 冗余
    status          SMALLINT        NOT NULL DEFAULT 1,       -- 1-正常
    create_time     TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_time     TIMESTAMP       NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0
);

COMMENT ON TABLE  comments               IS '评论表';
COMMENT ON COLUMN comments.post_id        IS '所属帖子 ID';
COMMENT ON COLUMN comments.user_id        IS '评论者 ID';
COMMENT ON COLUMN comments.parent_id      IS '父评论 ID（二级回复）';
COMMENT ON COLUMN comments.reply_to_id    IS '被回复的用户 ID';
COMMENT ON COLUMN comments.content        IS '评论内容';
COMMENT ON COLUMN comments.like_count     IS '点赞数（冗余）';
COMMENT ON COLUMN comments.deleted        IS '逻辑删除：0-正常 1-已删除';

-- 查询索引
CREATE INDEX idx_comments_post      ON comments (post_id)          WHERE deleted = 0;
CREATE INDEX idx_comments_user      ON comments (user_id)          WHERE deleted = 0;
CREATE INDEX idx_comments_parent    ON comments (parent_id)        WHERE deleted = 0;
CREATE INDEX idx_comments_time      ON comments (create_time DESC) WHERE deleted = 0;

-- 复合索引：按帖子+时间查询一级评论
CREATE INDEX idx_comments_post_time ON comments (post_id, create_time ASC) WHERE deleted = 0 AND parent_id IS NULL;
-- 复合索引：按父评论+时间查询子回复
CREATE INDEX idx_comments_parent_time ON comments (parent_id, create_time ASC) WHERE deleted = 0;

-- ============================================================
-- 11. 点赞表
--     target_type: POST / COMMENT
--     target_id:   对应帖子或评论的 ID
-- ============================================================
CREATE TABLE likes (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    target_type     VARCHAR(20)     NOT NULL,                 -- POST / COMMENT
    target_id       BIGINT          NOT NULL,
    create_time     TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_time     TIMESTAMP       NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0
);

COMMENT ON TABLE  likes              IS '点赞表';
COMMENT ON COLUMN likes.user_id      IS '点赞用户 ID';
COMMENT ON COLUMN likes.target_type  IS '目标类型：POST / COMMENT';
COMMENT ON COLUMN likes.target_id    IS '目标 ID';
COMMENT ON COLUMN likes.deleted      IS '逻辑删除：0-正常 1-已删除（取消点赞时软删）';

-- 唯一约束：同一用户对同一目标只能点赞一次
CREATE UNIQUE INDEX uk_likes_user_target ON likes (user_id, target_type, target_id) WHERE deleted = 0;
CREATE INDEX idx_likes_target ON likes (target_type, target_id) WHERE deleted = 0;
CREATE INDEX idx_likes_user   ON likes (user_id) WHERE deleted = 0;

-- ============================================================
-- 12. 收藏表
-- ============================================================
CREATE TABLE collections (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    post_id         BIGINT          NOT NULL,
    folder_name     VARCHAR(50)     DEFAULT '默认收藏夹',
    create_time     TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_time     TIMESTAMP       NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0
);

COMMENT ON TABLE  collections             IS '收藏表';
COMMENT ON COLUMN collections.user_id     IS '收藏用户 ID';
COMMENT ON COLUMN collections.post_id     IS '收藏帖子 ID';
COMMENT ON COLUMN collections.folder_name IS '收藏夹名称';
COMMENT ON COLUMN collections.deleted     IS '逻辑删除：0-正常 1-已删除（取消收藏时软删）';

CREATE UNIQUE INDEX uk_collections_user_post ON collections (user_id, post_id) WHERE deleted = 0;
CREATE INDEX idx_collections_user ON collections (user_id, folder_name) WHERE deleted = 0;
CREATE INDEX idx_collections_post ON collections (post_id) WHERE deleted = 0;

-- ============================================================
-- 13. 关注表
-- ============================================================
CREATE TABLE follows (
    id              BIGSERIAL       PRIMARY KEY,
    follower_id     BIGINT          NOT NULL,                  -- 关注者
    followee_id     BIGINT          NOT NULL,                  -- 被关注者
    create_time     TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_time     TIMESTAMP       NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0
);

COMMENT ON TABLE  follows             IS '关注表';
COMMENT ON COLUMN follows.follower_id IS '关注者 ID';
COMMENT ON COLUMN follows.followee_id IS '被关注者 ID';
COMMENT ON COLUMN follows.deleted     IS '逻辑删除：0-正常 1-已删除（取消关注时软删）';

CREATE UNIQUE INDEX uk_follows ON follows (follower_id, followee_id) WHERE deleted = 0;
CREATE INDEX idx_follows_follower ON follows (follower_id) WHERE deleted = 0;
CREATE INDEX idx_follows_followee ON follows (followee_id) WHERE deleted = 0;

-- ============================================================
-- 14. 会话表
--     通过 CHECK 约束保证 user1_id < user2_id
-- ============================================================
CREATE TABLE conversations (
    id              BIGSERIAL       PRIMARY KEY,
    user1_id        BIGINT          NOT NULL,
    user2_id        BIGINT          NOT NULL,
    last_message    TEXT,                                     -- 最后一条消息摘要
    last_message_at TIMESTAMP,                                -- 最后消息时间
    create_time     TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_time     TIMESTAMP       NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0,
    CONSTRAINT chk_conv_users CHECK (user1_id < user2_id)
);

COMMENT ON TABLE  conversations                 IS '会话表';
COMMENT ON COLUMN conversations.user1_id        IS '参与者1 ID（较小值）';
COMMENT ON COLUMN conversations.user2_id        IS '参与者2 ID（较大值）';
COMMENT ON COLUMN conversations.last_message    IS '最后一条消息摘要';
COMMENT ON COLUMN conversations.last_message_at IS '最后消息时间';
COMMENT ON COLUMN conversations.deleted         IS '逻辑删除：0-正常 1-已删除';

-- 保证同一对用户只有一个会话
CREATE UNIQUE INDEX uk_conversations_users ON conversations (user1_id, user2_id) WHERE deleted = 0;

CREATE INDEX idx_conv_user1 ON conversations (user1_id) WHERE deleted = 0;
CREATE INDEX idx_conv_user2 ON conversations (user2_id) WHERE deleted = 0;
CREATE INDEX idx_conv_last_msg ON conversations (last_message_at DESC) WHERE deleted = 0;

-- ============================================================
-- 15. 私信表
-- ============================================================
CREATE TABLE messages (
    id              BIGSERIAL       PRIMARY KEY,
    conversation_id BIGINT          NOT NULL,
    sender_id       BIGINT          NOT NULL,
    content         TEXT            NOT NULL,
    is_read         BOOLEAN         DEFAULT FALSE,
    read_at         TIMESTAMP,                                -- 阅读时间
    create_time     TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_time     TIMESTAMP       NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0
);

COMMENT ON TABLE  messages                  IS '私信表';
COMMENT ON COLUMN messages.conversation_id  IS '会话 ID';
COMMENT ON COLUMN messages.sender_id        IS '发送者 ID';
COMMENT ON COLUMN messages.content          IS '消息内容';
COMMENT ON COLUMN messages.is_read          IS '是否已读';
COMMENT ON COLUMN messages.read_at          IS '阅读时间';
COMMENT ON COLUMN messages.deleted          IS '逻辑删除：0-正常 1-已删除';

CREATE INDEX idx_messages_conv      ON messages (conversation_id, create_time DESC) WHERE deleted = 0;
CREATE INDEX idx_messages_sender    ON messages (sender_id) WHERE deleted = 0;
CREATE INDEX idx_messages_unread    ON messages (conversation_id, is_read) WHERE deleted = 0 AND is_read = FALSE;

-- ============================================================
-- 16. 通知表
-- ============================================================
CREATE TABLE notifications (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL,                  -- 通知接收者
    from_user_id    BIGINT,                                    -- 触发通知的用户
    notify_type     VARCHAR(30)     NOT NULL,                  -- LIKE / COMMENT / FOLLOW / MENTION / SYSTEM
    title           VARCHAR(200)    NOT NULL,
    content         TEXT,
    target_type     VARCHAR(30),                               -- POST / COMMENT / USER
    target_id       BIGINT,                                    -- 关联目标 ID
    is_read         BOOLEAN         DEFAULT FALSE,
    read_at         TIMESTAMP,
    create_time     TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_time     TIMESTAMP       NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0
);

COMMENT ON TABLE  notifications               IS '通知表';
COMMENT ON COLUMN notifications.user_id       IS '通知接收者 ID';
COMMENT ON COLUMN notifications.from_user_id  IS '触发通知的用户 ID';
COMMENT ON COLUMN notifications.notify_type   IS '通知类型：LIKE-点赞 COMMENT-评论 FOLLOW-关注 MENTION-@提及 SYSTEM-系统';
COMMENT ON COLUMN notifications.target_type   IS '关联目标类型：POST / COMMENT / USER';
COMMENT ON COLUMN notifications.target_id     IS '关联目标 ID';
COMMENT ON COLUMN notifications.deleted       IS '逻辑删除：0-正常 1-已删除';

CREATE INDEX idx_notify_user        ON notifications (user_id, create_time DESC) WHERE deleted = 0;
CREATE INDEX idx_notify_unread      ON notifications (user_id, is_read) WHERE deleted = 0 AND is_read = FALSE;
CREATE INDEX idx_notify_type        ON notifications (notify_type) WHERE deleted = 0;
CREATE INDEX idx_notify_time        ON notifications (create_time DESC) WHERE deleted = 0;
CREATE INDEX idx_notify_target      ON notifications (target_type, target_id) WHERE deleted = 0;
CREATE INDEX idx_notify_from_user   ON notifications (from_user_id) WHERE deleted = 0;

-- ============================================================
-- 17. 附件表
-- ============================================================
CREATE TABLE attachments (
    id              BIGSERIAL       PRIMARY KEY,
    user_id         BIGINT          NOT NULL,                  -- 上传者
    file_name       VARCHAR(255)    NOT NULL,                  -- 原始文件名
    file_key        VARCHAR(500)    NOT NULL,                  -- MinIO 对象 key
    file_url        VARCHAR(500),                              -- 访问 URL
    file_size       BIGINT          NOT NULL,                  -- 字节数
    mime_type       VARCHAR(100),                              -- MIME 类型
    width           INT,                                       -- 图片宽度
    height          INT,                                       -- 图片高度
    thumb_key       VARCHAR(500),                              -- 缩略图 MinIO key
    related_type    VARCHAR(30),                               -- 关联类型：POST / COMMENT / AVATAR
    related_id      BIGINT,                                    -- 关联对象 ID
    create_time     TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_time     TIMESTAMP       NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0
);

COMMENT ON TABLE  attachments               IS '附件表';
COMMENT ON COLUMN attachments.user_id       IS '上传者 ID';
COMMENT ON COLUMN attachments.file_name     IS '原始文件名';
COMMENT ON COLUMN attachments.file_key      IS 'MinIO 对象 key';
COMMENT ON COLUMN attachments.file_url      IS '访问 URL';
COMMENT ON COLUMN attachments.file_size     IS '文件大小（字节）';
COMMENT ON COLUMN attachments.mime_type     IS 'MIME 类型';
COMMENT ON COLUMN attachments.width         IS '图片宽度';
COMMENT ON COLUMN attachments.height        IS '图片高度';
COMMENT ON COLUMN attachments.thumb_key     IS '缩略图 MinIO key';
COMMENT ON COLUMN attachments.related_type  IS '关联类型：POST / COMMENT / AVATAR';
COMMENT ON COLUMN attachments.related_id    IS '关联对象 ID';
COMMENT ON COLUMN attachments.deleted       IS '逻辑删除：0-正常 1-已删除';

CREATE INDEX idx_attachments_user   ON attachments (user_id) WHERE deleted = 0;
CREATE INDEX idx_attachments_related ON attachments (related_type, related_id) WHERE deleted = 0;
CREATE INDEX idx_attachments_key    ON attachments (file_key) WHERE deleted = 0;

-- ============================================================
-- 18. 管理员操作日志表
-- ============================================================
CREATE TABLE admin_logs (
    id              BIGSERIAL       PRIMARY KEY,
    admin_id        BIGINT          NOT NULL,                  -- 操作管理员 ID
    action          VARCHAR(50)     NOT NULL,                  -- 操作类型
    target_type     VARCHAR(30),                               -- 目标类型：USER / POST / COMMENT / CATEGORY / TAG
    target_id       BIGINT,                                    -- 目标 ID
    detail          TEXT,                                      -- 操作详情（变更前后对比）
    ip              VARCHAR(45),                               -- 操作 IP
    create_time     TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_time     TIMESTAMP       NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0
);

COMMENT ON TABLE  admin_logs              IS '管理员操作日志表';
COMMENT ON COLUMN admin_logs.admin_id     IS '操作管理员 ID';
COMMENT ON COLUMN admin_logs.action       IS '操作类型：BAN_USER / DELETE_POST / PIN_POST / FEATURE_POST / MANAGE_CATEGORY 等';
COMMENT ON COLUMN admin_logs.target_type  IS '目标类型：USER / POST / COMMENT / CATEGORY / TAG';
COMMENT ON COLUMN admin_logs.target_id    IS '目标 ID';
COMMENT ON COLUMN admin_logs.detail       IS '操作详情（TEXT，记录变更前后对比）';
COMMENT ON COLUMN admin_logs.ip           IS '操作 IP';
COMMENT ON COLUMN admin_logs.deleted      IS '逻辑删除：0-正常 1-已删除';

CREATE INDEX idx_admin_logs_admin   ON admin_logs (admin_id) WHERE deleted = 0;
CREATE INDEX idx_admin_logs_action  ON admin_logs (action) WHERE deleted = 0;
CREATE INDEX idx_admin_logs_target  ON admin_logs (target_type, target_id) WHERE deleted = 0;
CREATE INDEX idx_admin_logs_time    ON admin_logs (create_time DESC) WHERE deleted = 0;

-- ============================================================
-- 19. 举报表
-- ============================================================
CREATE TABLE reports (
    id              BIGSERIAL       PRIMARY KEY,
    reporter_id     BIGINT          NOT NULL,
    target_type     VARCHAR(20)     NOT NULL,
    target_id       BIGINT          NOT NULL,
    reason          VARCHAR(50)     NOT NULL,
    description     TEXT,
    status          SMALLINT        NOT NULL DEFAULT 1,
    handler_id      BIGINT,
    handle_result   TEXT,
    handle_time     TIMESTAMP,
    create_time     TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_time     TIMESTAMP       NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0
);

COMMENT ON TABLE  reports                IS '举报表';
COMMENT ON COLUMN reports.reporter_id    IS '举报人 ID';
COMMENT ON COLUMN reports.target_type    IS '举报目标类型：POST / COMMENT / USER';
COMMENT ON COLUMN reports.target_id      IS '举报目标 ID';
COMMENT ON COLUMN reports.reason         IS '举报原因';
COMMENT ON COLUMN reports.description    IS '举报详细描述';
COMMENT ON COLUMN reports.status         IS '处理状态：1-待处理 2-已处理 3-已驳回';
COMMENT ON COLUMN reports.handler_id     IS '处理人 ID';
COMMENT ON COLUMN reports.handle_result  IS '处理结果';
COMMENT ON COLUMN reports.handle_time    IS '处理时间';
COMMENT ON COLUMN reports.deleted        IS '逻辑删除：0-正常 1-已删除';

CREATE INDEX idx_reports_reporter ON reports (reporter_id) WHERE deleted = 0;
CREATE INDEX idx_reports_target   ON reports (target_type, target_id) WHERE deleted = 0;
CREATE INDEX idx_reports_status   ON reports (status) WHERE deleted = 0;
CREATE INDEX idx_reports_time     ON reports (create_time DESC) WHERE deleted = 0;
CREATE INDEX idx_reports_handler  ON reports (handler_id) WHERE deleted = 0;

-- ============================================================
-- 20. 帖子系列/合集表
-- ============================================================
CREATE TABLE series (
    id          BIGSERIAL PRIMARY KEY,
    author_id   BIGINT NOT NULL,
    title       VARCHAR(200) NOT NULL,
    description TEXT,
    cover_url   VARCHAR(500),
    post_count  INT DEFAULT 0,
    sort_order  INT DEFAULT 0,
    status      SMALLINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT NOW(),
    update_time TIMESTAMP DEFAULT NOW(),
    deleted     SMALLINT DEFAULT 0
);

COMMENT ON TABLE series IS '帖子系列/合集表';
COMMENT ON COLUMN series.author_id IS '系列创建者 ID';
COMMENT ON COLUMN series.title IS '系列标题';
COMMENT ON COLUMN series.description IS '系列简介';
COMMENT ON COLUMN series.cover_url IS '系列封面图 URL';
COMMENT ON COLUMN series.post_count IS '系列内帖子数（冗余）';
COMMENT ON COLUMN series.status IS '状态：1-公开 0-私密';
COMMENT ON COLUMN series.deleted IS '逻辑删除：0-正常 1-已删除';

CREATE INDEX idx_series_author ON series(author_id) WHERE deleted = 0;
CREATE INDEX idx_series_time ON series(create_time DESC) WHERE deleted = 0;

-- ============================================================
-- 21. 系列-帖子关联表
-- ============================================================
CREATE TABLE series_posts (
    id          BIGSERIAL PRIMARY KEY,
    series_id   BIGINT NOT NULL,
    post_id     BIGINT NOT NULL,
    sort_order  INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT NOW(),
    update_time TIMESTAMP DEFAULT NOW(),
    deleted     SMALLINT DEFAULT 0
);

COMMENT ON TABLE series_posts IS '系列-帖子关联表';
COMMENT ON COLUMN series_posts.series_id IS '系列 ID';
COMMENT ON COLUMN series_posts.post_id IS '帖子 ID';
COMMENT ON COLUMN series_posts.sort_order IS '排序号';
COMMENT ON COLUMN series_posts.deleted IS '逻辑删除：0-正常 1-已删除';

CREATE UNIQUE INDEX uk_series_post ON series_posts(series_id, post_id) WHERE deleted = 0;
CREATE INDEX idx_sp_series ON series_posts(series_id, sort_order) WHERE deleted = 0;
CREATE INDEX idx_sp_post ON series_posts(post_id) WHERE deleted = 0;

-- ============================================================
-- 22. 阅读历史表
-- ============================================================
CREATE TABLE reading_history (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    post_id     BIGINT NOT NULL,
    read_at     TIMESTAMP DEFAULT NOW(),
    create_time TIMESTAMP DEFAULT NOW(),
    update_time TIMESTAMP DEFAULT NOW(),
    deleted     SMALLINT DEFAULT 0
);

COMMENT ON TABLE reading_history IS '阅读历史表';
COMMENT ON COLUMN reading_history.user_id IS '用户 ID';
COMMENT ON COLUMN reading_history.post_id IS '帖子 ID';
COMMENT ON COLUMN reading_history.read_at IS '阅读时间';

CREATE UNIQUE INDEX uk_rh_user_post ON reading_history(user_id, post_id) WHERE deleted = 0;
CREATE INDEX idx_rh_user_time ON reading_history(user_id, read_at DESC) WHERE deleted = 0;

-- ============================================================
-- 23. 稍后阅读（阅读列表）表
-- ============================================================
CREATE TABLE read_later (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    post_id     BIGINT NOT NULL,
    create_time TIMESTAMP DEFAULT NOW(),
    update_time TIMESTAMP DEFAULT NOW(),
    deleted     SMALLINT DEFAULT 0
);

COMMENT ON TABLE read_later IS '稍后阅读列表';
COMMENT ON COLUMN read_later.user_id IS '用户 ID';
COMMENT ON COLUMN read_later.post_id IS '帖子 ID';

CREATE UNIQUE INDEX uk_rl_user_post ON read_later(user_id, post_id) WHERE deleted = 0;
CREATE INDEX idx_rl_user_time ON read_later(user_id, create_time DESC) WHERE deleted = 0;

-- ============================================================
-- 24. 勋章/成就定义表
-- ============================================================
CREATE TABLE badges (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(50) NOT NULL UNIQUE,
    name        VARCHAR(50) NOT NULL,
    description VARCHAR(200),
    icon_url    VARCHAR(500),
    category    VARCHAR(30) DEFAULT 'general',
    sort_order  INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT NOW(),
    update_time TIMESTAMP DEFAULT NOW(),
    deleted     SMALLINT DEFAULT 0
);

COMMENT ON TABLE badges IS '勋章/成就定义表';
COMMENT ON COLUMN badges.code IS '勋章编码';
COMMENT ON COLUMN badges.name IS '勋章名称';
COMMENT ON COLUMN badges.description IS '勋章描述';
COMMENT ON COLUMN badges.category IS '分类: general / content / social';

CREATE UNIQUE INDEX uk_badges_code ON badges(code) WHERE deleted = 0;
CREATE INDEX idx_badges_category ON badges(category) WHERE deleted = 0;

-- ============================================================
-- 25. 用户勋章关联表
-- ============================================================
CREATE TABLE user_badges (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    badge_id    BIGINT NOT NULL,
    unlocked_at TIMESTAMP DEFAULT NOW(),
    notified    BOOLEAN DEFAULT FALSE,
    create_time TIMESTAMP DEFAULT NOW(),
    update_time TIMESTAMP DEFAULT NOW(),
    deleted     SMALLINT DEFAULT 0
);

COMMENT ON TABLE user_badges IS '用户勋章关联表';
COMMENT ON COLUMN user_badges.user_id IS '用户 ID';
COMMENT ON COLUMN user_badges.badge_id IS '勋章 ID';
COMMENT ON COLUMN user_badges.unlocked_at IS '解锁时间';
COMMENT ON COLUMN user_badges.notified IS '是否已通知用户';

CREATE UNIQUE INDEX uk_ub_user_badge ON user_badges(user_id, badge_id) WHERE deleted = 0;
CREATE INDEX idx_ub_user ON user_badges(user_id) WHERE deleted = 0;

-- ============================================================
-- 26. 标签订阅表
-- ============================================================
CREATE TABLE tag_subscriptions (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    tag_id      BIGINT NOT NULL,
    create_time TIMESTAMP DEFAULT NOW(),
    update_time TIMESTAMP DEFAULT NOW(),
    deleted     SMALLINT DEFAULT 0
);

COMMENT ON TABLE tag_subscriptions IS '标签订阅表';

CREATE UNIQUE INDEX uk_ts_user_tag ON tag_subscriptions(user_id, tag_id) WHERE deleted = 0;
CREATE INDEX idx_ts_user ON tag_subscriptions(user_id) WHERE deleted = 0;
CREATE INDEX idx_ts_tag ON tag_subscriptions(tag_id) WHERE deleted = 0;

-- ============================================================
-- 27. 分类订阅表
-- ============================================================
CREATE TABLE category_subscriptions (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    create_time TIMESTAMP DEFAULT NOW(),
    update_time TIMESTAMP DEFAULT NOW(),
    deleted     SMALLINT DEFAULT 0
);

COMMENT ON TABLE category_subscriptions IS '分类订阅表';

CREATE UNIQUE INDEX uk_cs_user_cat ON category_subscriptions(user_id, category_id) WHERE deleted = 0;
CREATE INDEX idx_cs_user ON category_subscriptions(user_id) WHERE deleted = 0;
CREATE INDEX idx_cs_cat ON category_subscriptions(category_id) WHERE deleted = 0;

-- ============================================================
-- 28. 通知设置表
-- ============================================================
CREATE TABLE notification_settings (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    notify_type VARCHAR(30) NOT NULL,
    enabled     BOOLEAN DEFAULT TRUE,
    create_time TIMESTAMP DEFAULT NOW(),
    update_time TIMESTAMP DEFAULT NOW(),
    deleted     SMALLINT DEFAULT 0
);

COMMENT ON TABLE notification_settings IS '用户通知设置表';
COMMENT ON COLUMN notification_settings.notify_type IS '通知类型: LIKE/COMMENT/FOLLOW/MENTION/SYSTEM';
COMMENT ON COLUMN notification_settings.enabled IS '是否启用该类型通知';

CREATE UNIQUE INDEX uk_ns_user_type ON notification_settings(user_id, notify_type) WHERE deleted = 0;

-- ============================================================
-- 29. 敏感词库表
-- ============================================================
CREATE TABLE sensitive_words (
    id          BIGSERIAL PRIMARY KEY,
    word        VARCHAR(100) NOT NULL UNIQUE,
    replacement VARCHAR(50) DEFAULT '***',
    category    VARCHAR(30) DEFAULT 'general',
    enabled     BOOLEAN DEFAULT TRUE,
    create_time TIMESTAMP DEFAULT NOW(),
    update_time TIMESTAMP DEFAULT NOW(),
    deleted     SMALLINT DEFAULT 0
);

COMMENT ON TABLE sensitive_words IS '敏感词库';
COMMENT ON COLUMN sensitive_words.word IS '敏感词';
COMMENT ON COLUMN sensitive_words.replacement IS '替换文本';

CREATE UNIQUE INDEX uk_sw_word ON sensitive_words(word) WHERE deleted = 0;

-- ============================================================
-- 数据字典类型表
-- ============================================================
CREATE TABLE dict_types (
    id              BIGSERIAL       PRIMARY KEY,
    code            VARCHAR(50)     NOT NULL,
    name            VARCHAR(100)    NOT NULL,
    description     VARCHAR(200),
    status          SMALLINT        NOT NULL DEFAULT 1,
    create_time     TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_time     TIMESTAMP       NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0
);

COMMENT ON TABLE  dict_types              IS '数据字典类型表';
COMMENT ON COLUMN dict_types.code         IS '字典编码：NOTIFY_TYPE/REPORT_REASON/ANNOUNCE_LEVEL/REPORT_TARGET_TYPE';
COMMENT ON COLUMN dict_types.name         IS '字典名称';
COMMENT ON COLUMN dict_types.status       IS '状态：1-启用 0-禁用';

CREATE UNIQUE INDEX uk_dict_types_code ON dict_types (code) WHERE deleted = 0;

-- ============================================================
-- 数据字典项表
-- ============================================================
CREATE TABLE dict_items (
    id              BIGSERIAL       PRIMARY KEY,
    type_code       VARCHAR(50)     NOT NULL,
    item_key        VARCHAR(50)     NOT NULL,
    item_value      VARCHAR(100)    NOT NULL,
    sort_order      INT             DEFAULT 0,
    extra           VARCHAR(200),
    status          SMALLINT        NOT NULL DEFAULT 1,
    create_time     TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_time     TIMESTAMP       NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0
);

COMMENT ON TABLE  dict_items               IS '数据字典项表';
COMMENT ON COLUMN dict_items.type_code     IS '关联 dict_types.code';
COMMENT ON COLUMN dict_items.item_key      IS '存储值';
COMMENT ON COLUMN dict_items.item_value    IS '显示值';
COMMENT ON COLUMN dict_items.extra         IS '扩展字段（如 ElementPlus tag type）';
COMMENT ON COLUMN dict_items.status        IS '状态：1-启用 0-禁用';

CREATE UNIQUE INDEX uk_dict_items_key ON dict_items (type_code, item_key) WHERE deleted = 0;
CREATE INDEX idx_dict_items_type ON dict_items (type_code, status, sort_order) WHERE deleted = 0;

-- ============================================================
-- 公告表
-- ============================================================
CREATE TABLE announcements (
    id              BIGSERIAL       PRIMARY KEY,
    title           VARCHAR(200)    NOT NULL,
    content         TEXT,
    summary         VARCHAR(500),
    level           VARCHAR(20)     NOT NULL DEFAULT 'INFO',
    status          SMALLINT        NOT NULL DEFAULT 0,
    is_pinned       BOOLEAN         NOT NULL DEFAULT FALSE,
    publish_time    TIMESTAMP,
    expire_time     TIMESTAMP,
    sort_order      INT             DEFAULT 0,
    view_count      INT             DEFAULT 0,
    created_by      BIGINT,
    create_time     TIMESTAMP       NOT NULL DEFAULT NOW(),
    update_time     TIMESTAMP       NOT NULL DEFAULT NOW(),
    deleted         SMALLINT        NOT NULL DEFAULT 0
);

COMMENT ON TABLE  announcements              IS '公告表';
COMMENT ON COLUMN announcements.title         IS '公告标题';
COMMENT ON COLUMN announcements.content       IS '公告正文';
COMMENT ON COLUMN announcements.summary       IS '简短摘要（横幅展示）';
COMMENT ON COLUMN announcements.level         IS '公告级别：INFO/WARNING/IMPORTANT';
COMMENT ON COLUMN announcements.status        IS '状态：0-草稿 1-已发布 2-已撤回';
COMMENT ON COLUMN announcements.is_pinned     IS '是否置顶';
COMMENT ON COLUMN announcements.publish_time  IS '发布时间';
COMMENT ON COLUMN announcements.expire_time   IS '过期时间';
COMMENT ON COLUMN announcements.created_by    IS '发布人用户 ID';
COMMENT ON COLUMN announcements.deleted       IS '逻辑删除：0-正常 1-已删除';

CREATE INDEX idx_announcements_status ON announcements (status, is_pinned, sort_order) WHERE deleted = 0;

-- ============================================================
-- 初始化基础数据（角色 & 权限）
-- ============================================================

-- 角色
INSERT INTO roles (role_name, role_code, description, sort_order) VALUES
    ('超级管理员', 'ADMIN',      '系统超级管理员，拥有所有权限', 1),
    ('版主',       'MODERATOR', '版主，可管理指定分类内容',     2),
    ('普通用户',   'USER',       '普通注册用户',                 3);

-- 权限（顶级菜单）
INSERT INTO permissions (id, parent_id, perm_name, perm_code, perm_type, path, sort_order) VALUES
    (1,  0, '仪表盘',     'dashboard',      1, '/dashboard',       1),
    (2,  0, '用户管理',   'user:manage',     1, '/users',           2),
    (3,  0, '帖子管理',   'post:manage',     1, '/posts',           3),
    (4,  0, '评论管理',   'comment:manage',  1, '/comments',        4),
    (5,  0, '分类管理',   'category:manage', 1, '/categories',      5),
    (6,  0, '标签管理',   'tag:manage',      1, '/tags',            6),
    (7,  0, '系统配置',   'system:config',   1, '/settings',        7),
    (8,  0, '操作日志',   'log:view',        1, '/logs',            8);

-- 权限（操作按钮/接口）
INSERT INTO permissions (id, parent_id, perm_name, perm_code, perm_type, path, sort_order) VALUES
    -- 用户管理子权限
    (21, 2, '查看用户',   'user:view',   2, '', 1),
    (22, 2, '编辑用户',   'user:edit',   2, '', 2),
    (23, 2, '封禁用户',   'user:ban',    2, '', 3),
    -- 帖子管理子权限
    (31, 3, '查看帖子',   'post:view',   2, '', 1),
    (32, 3, '删除帖子',   'post:delete', 2, '', 2),
    (33, 3, '置顶帖子',   'post:pin',    2, '', 3),
    (34, 3, '加精帖子',   'post:feature',2, '', 4),
    -- 评论管理子权限
    (41, 4, '查看评论',   'comment:view',   2, '', 1),
    (42, 4, '删除评论',   'comment:delete', 2, '', 2),
    -- 分类管理子权限
    (51, 5, '查看分类',   'category:view',   2, '', 1),
    (52, 5, '新增分类',   'category:create', 2, '', 2),
    (53, 5, '编辑分类',   'category:edit',   2, '', 3),
    (54, 5, '删除分类',   'category:delete', 2, '', 4),
    -- 标签管理子权限
    (61, 6, '查看标签',   'tag:view',   2, '', 1),
    (62, 6, '新增标签',   'tag:create', 2, '', 2),
    (63, 6, '编辑标签',   'tag:edit',   2, '', 3),
    (64, 6, '删除标签',   'tag:delete', 2, '', 4),
    -- 系统配置子权限
    (71, 7, '查看配置',   'config:view', 2, '', 1),
    (72, 7, '修改配置',   'config:edit', 2, '', 2);

-- 管理员角色拥有所有权限
INSERT INTO role_permissions (role_id, permission_id)
SELECT 1, id FROM permissions WHERE deleted = 0;

-- ============================================================
-- 初始化勋章定义
-- ============================================================
INSERT INTO badges (code, name, description, category, sort_order) VALUES
    ('FIRST_POST',      '初出茅庐', '发布第1篇帖子',        'content', 1),
    ('TEN_POSTS',       '笔耕不辍', '发布10篇帖子',         'content', 2),
    ('FIFTY_POSTS',     '著作等身', '发布50篇帖子',         'content', 3),
    ('TEN_LIKES',       '人气之星', '收到10个点赞',         'content', 4),
    ('HUNDRED_LIKES',   '万人迷',   '收到100个点赞',        'content', 5),
    ('FIVE_FOLLOWS',    '社交达人', '关注5个用户',          'social',  6),
    ('SEVEN_DAY_LOGIN', '勤勉不倦', '累计7天登录',          'general', 7),
    ('TWENTY_COMMENTS', '评论家',   '发表20条评论',         'social',  8);

-- ============================================================
-- 初始化数据字典
-- ============================================================
INSERT INTO dict_types (code, name, description) VALUES
    ('NOTIFY_TYPE',        '通知类型',   '系统通知类型枚举'),
    ('REPORT_REASON',      '举报原因',   '用户举报可选原因'),
    ('ANNOUNCE_LEVEL',     '公告级别',   '公告重要程度分级'),
    ('REPORT_TARGET_TYPE', '举报目标类型', '举报可针对的内容类型');

INSERT INTO dict_items (type_code, item_key, item_value, sort_order, extra) VALUES
    ('NOTIFY_TYPE', 'LIKE',    '点赞通知',   1, ''),
    ('NOTIFY_TYPE', 'COMMENT', '评论通知',   2, ''),
    ('NOTIFY_TYPE', 'FOLLOW',  '关注通知',   3, ''),
    ('NOTIFY_TYPE', 'MENTION', '@提及通知',  4, ''),
    ('NOTIFY_TYPE', 'SYSTEM',  '系统通知',   5, ''),
    ('REPORT_REASON', 'spam',      '垃圾广告', 1, ''),
    ('REPORT_REASON', 'porn',      '色情低俗', 2, ''),
    ('REPORT_REASON', 'political', '政治敏感', 3, ''),
    ('REPORT_REASON', 'abuse',     '辱骂攻击', 4, ''),
    ('REPORT_REASON', 'fake',      '虚假信息', 5, ''),
    ('REPORT_REASON', 'copyright', '侵权行为', 6, ''),
    ('REPORT_REASON', 'other',     '其他',     7, ''),
    ('ANNOUNCE_LEVEL', 'INFO',      '信息', 1, ''),
    ('ANNOUNCE_LEVEL', 'WARNING',   '警告', 2, 'warning'),
    ('ANNOUNCE_LEVEL', 'IMPORTANT', '重要', 3, 'danger'),
    ('REPORT_TARGET_TYPE', 'POST',    '帖子', 1, ''),
    ('REPORT_TARGET_TYPE', 'COMMENT', '评论', 2, ''),
    ('REPORT_TARGET_TYPE', 'USER',    '用户', 3, '');

-- ============================================================
-- 初始化默认超级管理员
-- 用户名: admin  密码: admin123
-- ============================================================
INSERT INTO users (id, username, email, password_hash, nickname, status)
VALUES (1, 'admin', 'admin@mindtalk.com',
        '$2b$10$SjowY9tQMEAdsBtJ3pTH4u0TUwebJYpoddFL.7kIrQl93IVxS9dQe',
        '超级管理员', 1);

INSERT INTO user_roles (user_id, role_id) VALUES (1, 1);

-- 修正序列，确保应用通过 IdType.AUTO 插入时不会冲突
SELECT setval('users_id_seq', 1, true);
SELECT setval('user_roles_id_seq', 1, true);
SELECT setval('permissions_id_seq', (SELECT MAX(id) FROM permissions), true);

-- ============================================================
-- 自动更新 update_time 触发器函数
-- ============================================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.update_time = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 为所有表创建触发器
DO $$
DECLARE
    tbl TEXT;
BEGIN
    FOR tbl IN
        SELECT unnest(ARRAY[
            'roles', 'permissions', 'role_permissions',
            'users', 'user_roles',
            'categories', 'tags',
            'posts', 'post_tags',
            'comments',
            'likes',
            'collections',
            'follows',
            'conversations', 'messages',
            'notifications',
            'attachments',
            'admin_logs',
            'reports',
            'series', 'series_posts',
            'reading_history',
            'read_later',
            'badges', 'user_badges',
            'tag_subscriptions', 'category_subscriptions',
            'notification_settings',
            'sensitive_words',
            'announcements',
            'dict_types', 'dict_items'
        ])
    LOOP
        EXECUTE format('
            CREATE TRIGGER trg_%s_update_time
            BEFORE UPDATE ON %I
            FOR EACH ROW
            EXECUTE FUNCTION update_updated_at_column();
        ', tbl, tbl);
    END LOOP;
END;
$$ LANGUAGE plpgsql;
