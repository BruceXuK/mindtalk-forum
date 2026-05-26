-- 帖子系列/合集功能
CREATE TABLE IF NOT EXISTS series (
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

CREATE TABLE IF NOT EXISTS series_posts (
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

-- 创建触发器
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_series_update_time') THEN
        CREATE TRIGGER trg_series_update_time
        BEFORE UPDATE ON series FOR EACH ROW
        EXECUTE FUNCTION update_updated_at_column();
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_series_posts_update_time') THEN
        CREATE TRIGGER trg_series_posts_update_time
        BEFORE UPDATE ON series_posts FOR EACH ROW
        EXECUTE FUNCTION update_updated_at_column();
    END IF;
END;
$$ LANGUAGE plpgsql;
