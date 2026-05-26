-- 阅读历史功能
CREATE TABLE IF NOT EXISTS reading_history (
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

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_reading_history_update_time') THEN
        CREATE TRIGGER trg_reading_history_update_time
        BEFORE UPDATE ON reading_history FOR EACH ROW
        EXECUTE FUNCTION update_updated_at_column();
    END IF;
END;
$$ LANGUAGE plpgsql;
