-- 置顶/加精时效功能：添加过期时间字段
ALTER TABLE posts ADD COLUMN IF NOT EXISTS pinned_until TIMESTAMP;
ALTER TABLE posts ADD COLUMN IF NOT EXISTS featured_until TIMESTAMP;

COMMENT ON COLUMN posts.pinned_until IS '置顶过期时间，NULL 表示永久置顶';
COMMENT ON COLUMN posts.featured_until IS '加精过期时间，NULL 表示永久加精';
