-- ============================================================
-- MindTalk 数据重置脚本
-- 保留：roles / permissions / role_permissions / badges（配置数据）
--       users(id=1 admin) / user_roles(admin 角色绑定)
-- 清空：其余所有业务数据
-- 用法：docker exec -i mindtalk-postgres psql -U mindtalk -d mindtalk < reset.sql
-- ============================================================

BEGIN;

-- ============================================================
-- 1. 清空业务数据（TRUNCATE 自动重置序列）
-- ============================================================
TRUNCATE TABLE
  comments,
  post_tags,
  likes,
  collections,
  follows,
  conversations,
  messages,
  notifications,
  notification_settings,
  attachments,
  admin_logs,
  reports,
  series_posts,
  series,
  reading_history,
  read_later,
  tag_subscriptions,
  category_subscriptions,
  sensitive_words,
  categories,
  tags,
  posts,
  user_badges
RESTART IDENTITY CASCADE;

-- ============================================================
-- 2. 删除非 admin 的用户及其角色绑定
-- ============================================================
DELETE FROM user_roles WHERE user_id != 1;
DELETE FROM users WHERE id != 1;

-- ============================================================
-- 3. 重置序列（TRUNCATE 已处理上面那些表，这里处理 DELETE 的表）
-- ============================================================
SELECT setval('users_id_seq', 1, true);
SELECT setval('user_roles_id_seq', (SELECT COALESCE(MAX(id), 0) FROM user_roles), true);

COMMIT;

-- 验证
SELECT '--- 配置数据（保留）---' AS check_point;
SELECT 'roles' AS table_name, COUNT(*) AS row_count FROM roles
UNION ALL
SELECT 'permissions', COUNT(*) FROM permissions
UNION ALL
SELECT 'role_permissions', COUNT(*) FROM role_permissions
UNION ALL
SELECT 'badges', COUNT(*) FROM badges;

SELECT '--- 业务数据（应全部为 0 或仅含 admin）---' AS check_point;
SELECT 'users (仅 admin)' AS table_name, COUNT(*) FROM users
UNION ALL
SELECT 'user_roles (仅 admin)', COUNT(*) FROM user_roles
UNION ALL
SELECT 'posts', COUNT(*) FROM posts
UNION ALL
SELECT 'comments', COUNT(*) FROM comments
UNION ALL
SELECT 'categories', COUNT(*) FROM categories
UNION ALL
SELECT 'tags', COUNT(*) FROM tags;
