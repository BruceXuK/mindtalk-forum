-- 用户勋章/成就系统
CREATE TABLE IF NOT EXISTS badges (
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

CREATE TABLE IF NOT EXISTS user_badges (
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

-- Seed badge definitions
INSERT INTO badges (code, name, description, category, sort_order) VALUES
    ('FIRST_POST', '初出茅庐', '发布第1篇帖子', 'content', 1),
    ('TEN_POSTS', '笔耕不辍', '发布10篇帖子', 'content', 2),
    ('FIFTY_POSTS', '著作等身', '发布50篇帖子', 'content', 3),
    ('TEN_LIKES', '人气之星', '收到10个点赞', 'content', 4),
    ('HUNDRED_LIKES', '万人迷', '收到100个点赞', 'content', 5),
    ('FIVE_FOLLOWS', '社交达人', '关注5个用户', 'social', 6),
    ('SEVEN_DAY_LOGIN', '勤勉不倦', '累计7天登录', 'general', 7),
    ('TWENTY_COMMENTS', '评论家', '发表20条评论', 'social', 8)
ON CONFLICT (code) DO NOTHING;

-- Triggers
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_badges_update_time') THEN
        CREATE TRIGGER trg_badges_update_time BEFORE UPDATE ON badges FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_user_badges_update_time') THEN
        CREATE TRIGGER trg_user_badges_update_time BEFORE UPDATE ON user_badges FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
    END IF;
END;
$$ LANGUAGE plpgsql;
