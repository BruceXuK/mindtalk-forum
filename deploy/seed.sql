-- Seed data for MindTalk
-- Run: docker exec -i mindtalk-postgres psql -U mindtalk -d mindtalk < seed.sql

-- ============================================
-- Users (password: password123)
-- ============================================
INSERT INTO users (username, email, password_hash, nickname, bio, gender, location) VALUES
('zhangsan', 'zhangsan@mindtalk.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '张三', '全栈工程师，热爱开源', 1, '北京'),
('lisi', 'lisi@mindtalk.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '李四', '后端开发，专注 Java 生态', 1, '上海'),
('wangwu', 'wangwu@mindtalk.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '王五', '前端爱好者，Vue/React 通吃', 0, '深圳'),
('zhaoliu', 'zhaoliu@mindtalk.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '赵六', 'AI 算法工程师，LLM 探索者', 1, '杭州'),
('sunqi', 'sunqi@mindtalk.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '孙七', 'DevOps/SRE，云原生布道者', 1, '广州');

-- Assign USER role
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, 3 FROM users u
WHERE u.username IN ('zhangsan','lisi','wangwu','zhaoliu','sunqi')
AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id AND ur.role_id = 3 AND ur.deleted = 0);

-- ============================================
-- Posts
-- ============================================
INSERT INTO posts (title, content, content_text, author_id, category_id, view_count, like_count) VALUES
(
  'Spring Boot 3.2 新特性实战指南',
  E'## 概述\n\nSpring Boot 3.2 带来了许多令人兴奋的新特性。\n\n## 虚拟线程\n\n基于 Project Loom，Spring Boot 3.2 默认启用虚拟线程支持。\n\n```java\n@Bean\npublic TomcatProtocolHandlerCustomizer<?> customizer() {\n    return handler -> handler.setExecutor(Executors.newVirtualThreadPerTaskExecutor());\n}\n```\n\n> 虚拟线程大幅提升了 IO 密集型任务的吞吐量。\n\n## RestClient\n\n全新的同步 HTTP 客户端：\n\n```java\nRestClient restClient = RestClient.create();\nString result = restClient.get().uri("https://api.example.com/data").retrieve().body(String.class);\n```\n\n## 总结\n\nSpring Boot 3.2 是一次重要的里程碑版本。',
  'Spring Boot 3.2 带来了虚拟线程、RestClient、Docker Compose 支持等核心改进。',
  (SELECT id FROM users WHERE username = 'zhangsan'), 1, 1520, 48
),
(
  'Vue 3 Composition API 最佳实践',
  E'## 为什么要用 Composition API\n\n相比 Options API，Composition API 提供了更好的**逻辑复用**和**类型推导**。\n\n## 核心原则\n\n### 1. 单一职责\n\n每个 composable 只做一件事。\n\n### 2. 避免在 setup 中写业务逻辑\n\n把业务逻辑抽到 composable 中，保持组件简洁。\n\n### 3. 合理使用响应式 API\n\n- `ref` 用于基本类型\n- `reactive` 用于对象\n- `computed` 用于派生状态\n\n## 常见陷阱\n\n1. 忘记 `.value`\n2. watch 的惰性执行\n3. 响应式丢失\n\n---\n\n欢迎在评论区讨论你的实践！',
  'Composition API 提供了更好的逻辑复用和类型推导。核心原则和常见陷阱。',
  (SELECT id FROM users WHERE username = 'wangwu'), 1, 2300, 76
),
(
  'Kubernetes 生产环境避坑指南',
  E'## 前言\n\n在 K8s 生产环境中踩过的坑，分享给大家。\n\n## 1. 资源限制\n\n**必须设置** resource requests 和 limits：\n\n```yaml\nresources:\n  requests:\n    memory: "256Mi"\n    cpu: "250m"\n  limits:\n    memory: "512Mi"\n    cpu: "500m"\n```\n\n## 2. 探针配置\n\n- `initialDelaySeconds` 要给足\n- `failureThreshold` 不要设太小\n\n## 3. 日志管理\n\n使用集中式日志方案（EFK/Loki），不要依赖 `kubectl logs`。\n\n## 4. 网络策略\n\n默认拒绝所有流量，按需开放。\n\n> 安全第一。',
  '在 K8s 生产环境中踩过的坑。资源限制、探针配置、日志管理、网络策略。',
  (SELECT id FROM users WHERE username = 'sunqi'), 3, 980, 32
),
(
  'LLM 应用开发：从零到部署',
  E'## 引言\n\n2024 年是 LLM 应用爆发的一年。\n\n## 技术选型\n\n| 组件 | 推荐 | 备选 |\n|------|------|------|\n| 模型服务 | vLLM | TGI |\n| 编排框架 | LangChain | LlamaIndex |\n| 向量数据库 | Milvus | Pinecone |\n\n## RAG 架构\n\n```\n用户查询 -> Embedding -> 向量检索 -> 上下文拼接 -> LLM 生成\n```\n\n## 实践经验\n\n1. **Prompt 工程**比模型本身更重要\n2. **向量分块**大小影响检索质量\n3. **缓存策略**大幅降低成本\n\n## 未来方向\n\n- Agent 自动化\n- 多模态融合\n- 本地推理',
  '从 LangChain 到 vLLM，生态发展迅猛。RAG 架构、技术选型、实践经验。',
  (SELECT id FROM users WHERE username = 'zhaoliu'), 1, 3100, 95
),
(
  '微服务还是单体？架构选择思考',
  E'很多人一上来就选微服务，但大部分项目其实不需要。\n\n## 单体的优势\n\n- 开发效率高\n- 部署简单\n- 调试方便\n- 事务管理简单\n\n## 微服务的代价\n\n- 网络开销\n- 分布式事务\n- 运维复杂度\n\n> "任何可以用单体解决的问题，都不要用微服务。" — Martin Fowler\n\n## 我的建议\n\n1. 初期用**模块化单体**\n2. 当团队超过 20 人再考虑拆分\n3. 按业务边界拆分\n\n## 重构路径\n\n模块化单体 -> 按领域拆分 -> 独立部署 -> 微服务',
  '单体的优势、微服务的代价、模块化单体的建议。',
  (SELECT id FROM users WHERE username = 'lisi'), 2, 1850, 56
),
(
  '前端性能优化实战：从 5s 到 1s',
  E'## 背景\n\n我们项目首页加载时间 5 秒，通过以下优化降到了 1 秒。\n\n## 优化清单\n\n### 1. 代码分割\n```javascript\nconst HomePage = lazy(() => import("./pages/HomePage"))\n```\n\n### 2. 图片优化\n- WebP 格式\n- 响应式图片\n- 懒加载\n\n### 3. 缓存策略\n- Service Worker\n- CDN 缓存\n\n### 4. 打包优化\n```javascript\nbuild: { rollupOptions: { output: { manualChunks: { vendor: ["vue"], ui: ["element-plus"] } } } }\n```\n\n## 效果\n\n| 指标 | 优化前 | 优化后 |\n|------|--------|--------|\n| FCP | 2.3s | 0.8s |\n| LCP | 5.1s | 1.1s |\n| TTI | 4.8s | 1.0s |',
  '通过代码分割、图片优化、缓存策略、打包优化，首页从 5s 降到 1s。',
  (SELECT id FROM users WHERE username = 'wangwu'), 1, 4200, 128
),
(
  'Git 工作流最佳实践',
  E'## 分支策略\n\n推荐 **Trunk-Based Development**。\n\n## Commit 规范\n\n使用 Conventional Commits：\n\n```\nfeat: 添加用户登录功能\nfix: 修复分页溢出问题\nrefactor: 重构认证模块\n```\n\n## Code Review 要点\n\n1. 每个 PR 不超过 400 行\n2. 关注逻辑正确性\n3. 24 小时内完成 Review\n\n## 合并策略\n\n- Squash Merge: 保持 main 分支干净\n- Rebase: 保持历史线性',
  'Trunk-Based Development 分支策略，Conventional Commits 规范，Code Review 要点。',
  (SELECT id FROM users WHERE username = 'zhangsan'), 4, 670, 18
),
(
  'TypeScript 高级类型技巧',
  E'## 类型系统\n\nTypeScript 的类型系统是**图灵完备**的。\n\n## 条件类型\n\n```typescript\ntype IsString<T> = T extends string ? true : false\n```\n\n## 模板字面量类型\n\n```typescript\ntype EventName<T extends string> = `on${Capitalize<T>}`\n```\n\n## infer 关键字\n\n```typescript\ntype ReturnType<T> = T extends (...args: any[]) => infer R ? R : never\n```\n\n## 映射类型\n\n> 类型即文档。',
  '条件类型、模板字面量类型、infer 关键字、映射类型等高级技巧。',
  (SELECT id FROM users WHERE username = 'wangwu'), 1, 890, 35
),
(
  '构建高效的 CI/CD 流水线',
  E'## 目标\n\n提交即构建、构建即测试、测试即部署。\n\n## 流水线设计\n\n### Stage 1: 代码检查\n- eslint\n- prettier\n- tsc --noEmit\n\n### Stage 2: 单元测试\n- vitest --coverage\n- 覆盖率门槛 80%\n\n### Stage 3: 构建 & 部署\n- 蓝绿部署\n- 金丝雀发布\n\n## 优化技巧\n\n1. 缓存 node_modules\n2. 并行执行独立任务\n3. 使用增量构建',
  'CI/CD 流水线设计：代码检查、单元测试、构建部署。优化技巧。',
  (SELECT id FROM users WHERE username = 'sunqi'), 3, 540, 22
),
(
  '如何做好技术分享',
  E'## 为什么要分享\n\n分享是**最好的学习方式**。\n\n## 准备流程\n\n1. **选题**：解决实际问题\n2. **大纲**：金字塔结构\n3. **Demo**：代码优于 PPT\n4. **排练**：至少一次\n\n## 演讲技巧\n\n- 眼神交流\n- 控制语速\n- 适当幽默\n\n> "如果你不能简单地解释它，说明你还没有完全理解它。" — 爱因斯坦\n\n大家最近有什么想分享的话题？',
  '分享是最好的学习方式。准备流程和演讲技巧。',
  (SELECT id FROM users WHERE username = 'zhaoliu'), 6, 430, 15
);

-- ============================================
-- Comments
-- ============================================

-- Post: Spring Boot 3.2
INSERT INTO comments (post_id, user_id, content, parent_id, reply_to_id) VALUES
((SELECT id FROM posts WHERE title LIKE 'Spring Boot 3.2%'), (SELECT id FROM users WHERE username = 'lisi'), '写得很好！虚拟线程确实是个重大改进，我们项目迁移后吞吐量提升了 3 倍。', NULL, NULL),
((SELECT id FROM posts WHERE title LIKE 'Spring Boot 3.2%'), (SELECT id FROM users WHERE username = 'zhaoliu'), 'RestClient 终于来了，RestTemplate 早该退休了。', NULL, NULL),
((SELECT id FROM posts WHERE title LIKE 'Spring Boot 3.2%'), (SELECT id FROM users WHERE username = 'sunqi'), '希望 Docker Compose 支持能简化本地开发环境搭建。', NULL, NULL);

-- Sub-replies for Spring Boot post
WITH c AS (
  SELECT c1.id as cid, c1.user_id as uid
  FROM comments c1
  WHERE c1.post_id = (SELECT id FROM posts WHERE title LIKE 'Spring Boot 3.2%')
  ORDER BY c1.id
  LIMIT 1
)
INSERT INTO comments (post_id, user_id, content, parent_id, reply_to_id)
SELECT (SELECT id FROM posts WHERE title LIKE 'Spring Boot 3.2%'), (SELECT id FROM users WHERE username = 'zhangsan'), '我们团队也感受到了明显的性能提升。', c.cid, c.uid FROM c;

-- Post: Vue 3
INSERT INTO comments (post_id, user_id, content, parent_id, reply_to_id) VALUES
((SELECT id FROM posts WHERE title LIKE 'Vue 3%'), (SELECT id FROM users WHERE username = 'zhangsan'), 'Composition API 真香！逻辑复用太方便了。', NULL, NULL),
((SELECT id FROM posts WHERE title LIKE 'Vue 3%'), (SELECT id FROM users WHERE username = 'lisi'), '作为一个后端转前端的，这个文章对我帮助很大。', NULL, NULL),
((SELECT id FROM posts WHERE title LIKE 'Vue 3%'), (SELECT id FROM users WHERE username = 'zhaoliu'), '建议补充一下关于 Pinia 状态管理的最佳实践。', NULL, NULL);

-- Sub-replies for Vue post
WITH c AS (
  SELECT c2.id as cid, c2.user_id as uid
  FROM comments c2
  WHERE c2.post_id = (SELECT id FROM posts WHERE title LIKE 'Vue 3%')
  ORDER BY c2.id
  LIMIT 1
)
INSERT INTO comments (post_id, user_id, content, parent_id, reply_to_id)
SELECT (SELECT id FROM posts WHERE title LIKE 'Vue 3%'), (SELECT id FROM users WHERE username = 'wangwu'), '哈哈，文章里提到的常见陷阱我都踩过。', c.cid, c.uid FROM c;

-- Post: Kubernetes
INSERT INTO comments (post_id, user_id, content, parent_id, reply_to_id) VALUES
((SELECT id FROM posts WHERE title LIKE 'Kubernetes%'), (SELECT id FROM users WHERE username = 'lisi'), '资源限制那段非常实用，之前因为没有设 limits 导致节点被打爆。', NULL, NULL),
((SELECT id FROM posts WHERE title LIKE 'Kubernetes%'), (SELECT id FROM users WHERE username = 'zhangsan'), '网络策略默认拒绝的思路很好，安全第一。', NULL, NULL);

-- Post: LLM
INSERT INTO comments (post_id, user_id, content, parent_id, reply_to_id) VALUES
((SELECT id FROM posts WHERE title LIKE 'LLM%'), (SELECT id FROM users WHERE username = 'wangwu'), 'RAG 现在确实很火，你们用的是什么向量数据库？', NULL, NULL),
((SELECT id FROM posts WHERE title LIKE 'LLM%'), (SELECT id FROM users WHERE username = 'sunqi'), 'Prompt 工程比模型本身更重要——赞同。', NULL, NULL),
((SELECT id FROM posts WHERE title LIKE 'LLM%'), (SELECT id FROM users WHERE username = 'zhangsan'), '期待多模态融合的进展，感觉今年会有突破。', NULL, NULL);

-- Sub-reply for LLM
WITH c AS (
  SELECT c3.id as cid, c3.user_id as uid
  FROM comments c3
  WHERE c3.post_id = (SELECT id FROM posts WHERE title LIKE 'LLM%')
  ORDER BY c3.id
  LIMIT 1
)
INSERT INTO comments (post_id, user_id, content, parent_id, reply_to_id)
SELECT (SELECT id FROM posts WHERE title LIKE 'LLM%'), (SELECT id FROM users WHERE username = 'zhaoliu'), '我们用的 Milvus，性能很不错，社区也活跃。', c.cid, c.uid FROM c;

-- Post: 微服务
INSERT INTO comments (post_id, user_id, content, parent_id, reply_to_id) VALUES
((SELECT id FROM posts WHERE title LIKE '微服务%'), (SELECT id FROM users WHERE username = 'wangwu'), '模块化单体确实是最佳起点，我们就是这样演进的。', NULL, NULL),
((SELECT id FROM posts WHERE title LIKE '微服务%'), (SELECT id FROM users WHERE username = 'sunqi'), 'Fowler 那句话是至理名言！过早微服务化是万恶之源。', NULL, NULL),
((SELECT id FROM posts WHERE title LIKE '微服务%'), (SELECT id FROM users WHERE username = 'zhangsan'), '团队超过 20 人再拆——很实用的标准。', NULL, NULL);

-- Post: 性能优化
INSERT INTO comments (post_id, user_id, content, parent_id, reply_to_id) VALUES
((SELECT id FROM posts WHERE title LIKE '前端性能%'), (SELECT id FROM users WHERE username = 'lisi'), '从 5s 到 1s，优化效果太惊人了！', NULL, NULL),
((SELECT id FROM posts WHERE title LIKE '前端性能%'), (SELECT id FROM users WHERE username = 'zhaoliu'), '手动分 chunk 的配置很实用，收藏了。', NULL, NULL),
((SELECT id FROM posts WHERE title LIKE '前端性能%'), (SELECT id FROM users WHERE username = 'sunqi'), '希望再详细讲讲 Service Worker 的实践。', NULL, NULL);

-- Post: Git
INSERT INTO comments (post_id, user_id, content, parent_id, reply_to_id) VALUES
((SELECT id FROM posts WHERE title LIKE 'Git%'), (SELECT id FROM users WHERE username = 'wangwu'), 'Squash Merge 保持 main 干净，我们团队也在用。', NULL, NULL),
((SELECT id FROM posts WHERE title LIKE 'Git%'), (SELECT id FROM users WHERE username = 'sunqi'), 'Conventional Commits 配合 semantic-release 自动发版很爽。', NULL, NULL);

-- Post: TypeScript
INSERT INTO comments (post_id, user_id, content, parent_id, reply_to_id) VALUES
((SELECT id FROM posts WHERE title LIKE 'TypeScript%'), (SELECT id FROM users WHERE username = 'zhangsan'), '类型体操虽好，可不要过度使用哦。代码可读性也很重要。', NULL, NULL),
((SELECT id FROM posts WHERE title LIKE 'TypeScript%'), (SELECT id FROM users WHERE username = 'lisi'), 'infer 关键字的用法终于搞懂了，感谢！', NULL, NULL);

-- Post: CI/CD
INSERT INTO comments (post_id, user_id, content, parent_id, reply_to_id) VALUES
((SELECT id FROM posts WHERE title LIKE '构建高效的 CI%'), (SELECT id FROM users WHERE username = 'zhangsan'), '蓝绿部署和金丝雀发布那段写得很清晰。', NULL, NULL),
((SELECT id FROM posts WHERE title LIKE '构建高效的 CI%'), (SELECT id FROM users WHERE username = 'lisi'), '缓存 node_modules 这个技巧省了好多构建时间。', NULL, NULL);

-- Post: 技术分享
INSERT INTO comments (post_id, user_id, content, parent_id, reply_to_id) VALUES
((SELECT id FROM posts WHERE title LIKE '如何做好%'), (SELECT id FROM users WHERE username = 'wangwu'), 'Demo 优于 PPT，深有同感！', NULL, NULL),
((SELECT id FROM posts WHERE title LIKE '如何做好%'), (SELECT id FROM users WHERE username = 'sunqi'), '爱因斯坦那句话说得太对了。准备在公司内部做个分享试试。', NULL, NULL),
((SELECT id FROM posts WHERE title LIKE '如何做好%'), (SELECT id FROM users WHERE username = 'zhangsan'), '选题建议补充：选择自己真正做过的事情，而不是纸上谈兵。', NULL, NULL);

-- Update comment counts
UPDATE posts p SET comment_count = (
  SELECT COUNT(*) FROM comments c WHERE c.post_id = p.id AND c.deleted = 0
) WHERE p.deleted = 0;

-- ============================================
-- Badges (勋章定义)
-- ============================================
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
