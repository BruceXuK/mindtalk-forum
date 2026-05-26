-- 稍后阅读（阅读列表）功能
CREATE TABLE IF NOT EXISTS read_later (
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

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_read_later_update_time') THEN
        CREATE TRIGGER trg_read_later_update_time
        BEFORE UPDATE ON read_later FOR EACH ROW
        EXECUTE FUNCTION update_updated_at_column();
    END IF;
END;
$$ LANGUAGE plpgsql;
