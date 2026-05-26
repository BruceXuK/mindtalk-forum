-- 通知设置功能
CREATE TABLE IF NOT EXISTS notification_settings (
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
