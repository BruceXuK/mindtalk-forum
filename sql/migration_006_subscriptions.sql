-- 标签/分类订阅功能
CREATE TABLE IF NOT EXISTS tag_subscriptions (
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

CREATE TABLE IF NOT EXISTS category_subscriptions (
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
