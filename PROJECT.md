# MindTalk（思享论坛）项目文档

## 项目概述

MindTalk（思享论坛）是一个现代化的知识社区平台，参考 Notion、Medium、GitHub、掘金、Linear 等产品设计风格，采用极简、现代、清晰的设计语言。

| 属性 | 说明 |
|------|------|
| 项目名称 | MindTalk Forum |
| GroupId | com.mindtalk |
| 版本 | 1.0.0-SNAPSHOT |
| 架构 | 微服务（Spring Cloud Gateway + Nacos） |
| 构建工具 | Maven（父 POM + 多模块） |

---

## 技术架构

```
┌─────────────────────────────────────────────────────┐
│                    Nginx (:80)                       │
│             静态资源 + 反向代理                        │
└─────────────┬───────────────────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────────────────┐
│              Gateway (:8080)                         │
│     Spring Cloud Gateway + JWT 全局鉴权              │
│     服务发现：Nacos                                  │
└─────────────┬───────────────────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────────────────┐
│           Forum-Service (:8081)                      │
│      Spring Boot 3.2.5 核心业务服务                   │
│      ┌──────────────────────────────┐               │
│      │ 用户 / 帖子 / 评论 / 搜索     │               │
│      │ 消息 / 通知 / 管理 / 文件     │               │
│      └──────────────────────────────┘               │
└──────┬───────┬────────┬────────┬───────────────────┘
       │       │        │        │
       ▼       ▼        ▼        ▼
┌──────┐ ┌──────┐ ┌────────┐ ┌──────┐
│Post- │ │Redis │ │Elastic-│ │MinIO │
│greSQL│ │  7   │ │search  │ │      │
│  16  │ │      │ │        │ │      │
└──────┘ └──────┘ └────────┘ └──────┘
                    ┌──────────┐
                    │ RocketMQ │
                    │  5.1.4   │
                    └──────────┘
```

---

## 技术栈

### 后端

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 21 | 运行环境 |
| Spring Boot | 3.2.5 | 应用框架 |
| Spring Cloud | 2023.0.1 | 微服务框架 |
| Spring Cloud Alibaba | 2023.0.1.0 | Nacos 服务发现 |
| Spring Cloud Gateway | - | API 网关 |
| Spring Security | - | 安全框架 |
| MyBatis Plus | 3.5.6 | ORM 框架 |
| PostgreSQL | 16 | 关系型数据库 |
| Redis | 7 | 缓存 / 限流 |
| Elasticsearch | 8.x | 全文搜索 |
| RocketMQ | 5.1.4 | 消息队列 |
| MinIO | latest | 对象存储 |
| Nacos | 2.3.0 | 服务注册与发现 |
| JWT (jjwt) | 0.12.5 | 身份认证 |
| Knife4j | 4.5.0 | API 文档 |
| Hutool | 5.8.27 | 工具库 |

### 前端

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.4.27 | UI 框架 |
| TypeScript | 5.4.5 | 类型系统 |
| Vite | 5.2.11 | 构建工具 |
| Pinia | 2.1.7 | 状态管理 |
| Vue Router | 4.3.2 | 路由管理 |
| Axios | 1.7.2 | HTTP 客户端 |
| Element Plus | 2.7.0 | UI 组件库 |
| SCSS/Sass | 1.77.2 | 样式预处理 |
| unplugin-auto-import | 0.17.5 | 自动导入 |
| unplugin-vue-components | 0.27.0 | 组件自动注册 |

---

## 项目结构

```
mindtalk-forum/
├── pom.xml                          # Maven 父 POM
├── CLAUDE.md                        # Claude AI 项目规则
├── .gitignore
│
├── common/                          # 公共模块
│   └── src/main/java/com/mindtalk/common/
│       ├── constant/Constants.java          # 系统常量
│       ├── exception/
│       │   ├── BusinessException.java       # 业务异常
│       │   └── GlobalExceptionHandler.java  # 全局异常处理
│       ├── model/
│       │   ├── Result.java                  # 统一响应体
│       │   ├── PageRequest.java             # 分页请求
│       │   └── PageResult.java              # 分页结果
│       └── utils/                           # 通用工具类
│
├── gateway/                         # API 网关 (Spring Cloud Gateway)
│   └── src/main/java/com/mindtalk/gateway/
│       ├── GatewayApplication.java          # 启动类
│       ├── config/GatewayCorsConfig.java    # CORS 配置
│       └── filter/JwtAuthFilter.java        # JWT 全局鉴权过滤器
│
├── forum-service/                   # 核心业务服务 (Spring Boot 单体)
│   └── src/main/java/com/mindtalk/forum/
│       ├── ForumServiceApplication.java     # 启动类
│       ├── config/                          # 配置类
│       │   ├── SecurityConfig.java          # Spring Security 配置
│       │   ├── JwtAuthFilter.java           # 服务内 JWT 过滤器
│       │   ├── MyBatisPlusConfig.java       # MyBatis Plus 配置
│       │   ├── RedisConfig.java             # Redis 配置
│       │   ├── RocketMQConfig.java          # RocketMQ 配置
│       │   ├── MinioConfig.java             # MinIO 配置
│       │   ├── Knife4jConfig.java           # API 文档配置
│       │   ├── WebMvcConfig.java            # Web MVC 配置
│       │   └── MyMetaObjectHandler.java     # 自动填充处理器
│       ├── common/                          # 服务级公共组件
│       │   ├── annotation/RateLimit.java    # 限流注解
│       │   ├── aspect/RateLimiterAspect.java # 限流 AOP
│       │   ├── component/RocketMQProducer.java # MQ 生产者
│       │   └── utils/
│       │       ├── JwtUtils.java            # JWT 工具类
│       │       ├── RedisUtils.java          # Redis 工具类
│       │       ├── MinioUtils.java          # MinIO 工具类
│       │       └── UserConverter.java       # 用户实体转换
│       └── modules/                        # 业务模块
│           ├── user/                        # 用户模块
│           │   ├── controller/AuthController.java   # 认证接口
│           │   ├── controller/UserController.java   # 用户接口
│           │   ├── service/UserService.java
│           │   ├── entity/User.java, Role.java, UserRole.java, UserFollow.java
│           │   ├── dto/LoginDTO, RegisterDTO, UpdateProfileDTO, ChangePasswordDTO, RefreshTokenDTO
│           │   └── mapper/UserMapper.java, RoleMapper.java, UserRoleMapper.java, UserFollowMapper.java
│           │
│           ├── post/                        # 帖子模块
│           │   ├── controller/PostController.java     # 帖子 CRUD
│           │   ├── controller/CategoryController.java # 分类管理
│           │   ├── controller/TagController.java      # 标签管理
│           │   ├── service/PostService.java, CategoryService.java, TagService.java
│           │   ├── entity/Post.java, Category.java, Tag.java, PostTag.java
│           │   ├── document/PostDocument.java         # ES 文档模型
│           │   ├── repository/PostSearchRepository.java # ES Repository
│           │   └── consumer/                          # MQ 消费者
│           │       ├── SearchSyncConsumer.java        # ES 同步
│           │       └── ViewCountConsumer.java         # 浏览量统计
│           │
│           ├── comment/                     # 评论模块
│           │   ├── controller/CommentController.java
│           │   ├── service/CommentService.java
│           │   ├── entity/Comment.java, Like.java
│           │   └── consumer/CommentEventConsumer.java, LikeEventConsumer.java
│           │
│           ├── search/                      # 搜索模块
│           │   ├── controller/SearchController.java
│           │   └── service/SearchService.java
│           │
│           ├── message/                     # 消息/通知模块
│           │   ├── controller/NotificationController.java
│           │   ├── entity/Notification.java
│           │   └── consumer/FollowEventConsumer.java
│           │
│           ├── admin/                       # 管理后台模块
│           │   ├── controller/
│           │   │   ├── AdminUserController.java      # 用户管理
│           │   │   ├── AdminPostController.java      # 帖子审核
│           │   │   ├── AdminCommentController.java   # 评论审核
│           │   │   ├── AdminReportController.java    # 举报处理
│           │   │   ├── AdminRoleController.java      # 权限管理
│           │   │   └── AdminStatsController.java     # 统计分析
│           │   ├── service/AdminService.java
│           │   ├── annotation/AdminLog.java          # 操作日志注解
│           │   └── aspect/AdminLogAspect.java        # 操作日志 AOP
│           │
│           └── file/                        # 文件模块
│               └── controller/FileController.java    # 文件上传/下载
│
├── web/                              # 前端项目 (Vue3 + Vite)
│   ├── package.json
│   ├── vite.config.ts                        # Vite 配置
│   ├── tsconfig.json
│   ├── index.html
│   └── src/
│       ├── main.ts                           # 应用入口
│       ├── App.vue                           # 根组件
│       ├── router/index.ts                   # 路由配置
│       ├── stores/                           # Pinia 状态管理
│       │   ├── index.ts
│       │   └── modules/
│       │       ├── user.ts                   # 用户状态（登录、角色）
│       │       └── app.ts                    # 应用状态（主题、侧边栏）
│       ├── api/                              # API 接口层
│       │   ├── request.ts                    # Axios 实例 + 拦截器
│       │   └── modules/
│       │       ├── user.ts                   # 用户 API
│       │       ├── post.ts                   # 帖子 API
│       │       ├── comment.ts                # 评论 API
│       │       ├── search.ts                 # 搜索 API
│       │       ├── message.ts                # 消息 API
│       │       ├── admin.ts                  # 管理 API
│       │       └── file.ts                   # 文件 API
│       ├── composables/                      # 组合式函数
│       │   ├── useAuth.ts                    # 认证逻辑
│       │   ├── useTheme.ts                   # 主题切换
│       │   ├── useSidebar.ts                 # 侧边栏状态
│       │   └── useMarkdown.ts                # Markdown 渲染
│       ├── layouts/
│       │   ├── DefaultLayout.vue             # 默认布局（Header + 侧边栏 + 内容）
│       │   └── AdminLayout.vue               # 管理后台布局
│       ├── components/
│       │   ├── layout/
│       │   │   ├── AppHeader.vue             # 顶部导航栏
│       │   │   ├── AppSidebar.vue            # 左侧边栏
│       │   │   ├── AppRightSidebar.vue       # 右侧边栏
│       │   │   └── MobileDrawer.vue          # 移动端抽屉
│       │   ├── post/PostCard.vue             # 帖子卡片
│       │   ├── comment/CommentItem.vue       # 评论项
│       │   └── common/
│       │       ├── SkeletonCard.vue          # 骨架屏卡片
│       │       └── EmptyState.vue            # 空状态
│       ├── views/
│       │   ├── home/HomeView.vue             # 首页
│       │   ├── login/LoginView.vue           # 登录页
│       │   ├── register/RegisterView.vue     # 注册页
│       │   ├── post/
│       │   │   ├── PostListView.vue          # 帖子列表
│       │   │   └── PostDetailView.vue        # 帖子详情
│       │   ├── create-post/CreatePostView.vue # 发帖/编辑
│       │   ├── user/UserProfileView.vue      # 用户主页/个人中心
│       │   ├── search/SearchView.vue         # 搜索页
│       │   ├── message/MessageView.vue       # 消息中心
│       │   ├── admin/
│       │   │   ├── AdminView.vue             # 管理后台容器
│       │   │   ├── users/AdminUsersView.vue  # 用户管理
│       │   │   ├── posts/AdminPostsView.vue  # 帖子审核
│       │   │   ├── comments/AdminCommentsView.vue # 评论审核
│       │   │   ├── reports/AdminReportsView.vue   # 举报处理
│       │   │   ├── roles/AdminRolesView.vue  # 权限管理
│       │   │   └── stats/AdminStatsView.vue  # 统计分析
│       │   └── NotFoundView.vue              # 404
│       ├── assets/styles/
│       │   ├── tokens.scss                   # CSS 变量（颜色、间距、字体等）
│       │   ├── global.scss                   # 全局样式
│       │   ├── dark.scss                     # 暗色模式
│       │   └── transitions.scss              # 过渡动画
│       └── types/
│           ├── index.ts                      # 类型定义
│           └── env.d.ts                      # 环境变量类型
│
├── deploy/                           # 部署配置
│   ├── docker-compose.yml                    # Docker Compose 编排
│   ├── .env.example                          # 环境变量模板
│   ├── seed.sql                              # 种子数据
│   ├── nginx/nginx.conf                      # Nginx 配置
│   ├── gateway/Dockerfile                    # 网关 Dockerfile
│   ├── forum-service/Dockerfile              # 服务 Dockerfile
│   ├── elasticsearch/Dockerfile              # ES Dockerfile
│   ├── postgres/init.sql                     # PostgreSQL 初始化脚本
│   ├── minio/                                # MinIO 配置目录
│   └── rocketmq/                             # RocketMQ 配置目录
│
└── sql/
    └── init.sql                       # 数据库 DDL 完整脚本
```

---

## 数据库设计

共 **19 张表**，使用 PostgreSQL 16，统一采用逻辑删除（`deleted` 字段）。

### 核心业务表

| 表名 | 说明 | 关键字段 |
|------|------|----------|
| `users` | 用户表 | username, email, password_hash(BCrypt), nickname, avatar_url, status |
| `posts` | 帖子表 | title, content, author_id, category_id, is_pinned, is_featured, view/like/comment/collect_count |
| `comments` | 评论表 | post_id, user_id, parent_id(二级回复), reply_to_id, content |
| `categories` | 分类表 | name, icon, post_count(冗余) |
| `tags` | 标签表 | name, post_count(冗余) |
| `post_tags` | 帖子-标签关联 | post_id, tag_id |

### 交互表

| 表名 | 说明 |
|------|------|
| `likes` | 点赞表（target_type: POST/COMMENT） |
| `collections` | 收藏表 |
| `follows` | 关注表 |

### 私信/通知表

| 表名 | 说明 |
|------|------|
| `conversations` | 会话表（user1_id < user2_id 保证唯一） |
| `messages` | 私信表 |
| `notifications` | 通知表（LIKE/COMMENT/FOLLOW/MENTION/SYSTEM） |

### 权限/管理表

| 表名 | 说明 |
|------|------|
| `roles` | 角色表（ADMIN/MODERATOR/USER） |
| `permissions` | 权限表（树形结构，菜单/按钮/接口） |
| `role_permissions` | 角色-权限关联 |
| `user_roles` | 用户-角色关联 |
| `reports` | 举报表 |
| `admin_logs` | 管理员操作日志（JSONB 记录变更详情） |
| `attachments` | 附件表（MinIO 存储） |

### 数据库特性

- 使用 `pg_trgm` 扩展支持模糊搜索
- 使用 `uuid-ossp` 扩展生成 UUID
- 所有表创建 `update_time` 自动更新触发器
- 唯一约束通过部分索引 `WHERE deleted = 0` 实现软删除兼容
- 关键查询字段均建立索引

---

## API 设计

### 路由前缀

| 前缀 | 说明 |
|------|------|
| `/auth/**` | 认证接口（登录、注册、刷新 Token） |
| `/api/**` | 业务接口（需 JWT 鉴权，GET 允许匿名） |

### 认证流程

1. 用户登录 → 返回 `access_token`（30分钟） + `refresh_token`（7天）
2. 前端存储 Token 到 `sessionStorage`
3. Axios 拦截器自动注入 `Authorization: Bearer <token>` 请求头
4. Gateway `JwtAuthFilter` 解析 JWT，将 `X-User-Id`、`X-Username`、`X-Role` 写入请求头传递给下游
5. 白名单路径（`/auth/**`、`/doc.html` 等）跳过认证

### 鉴权层级

```
Gateway (全局 JWT 校验)
   ├── 白名单放行
   ├── 写操作强制认证（POST/PUT/DELETE 无 Token 返回 401）
   └── 读操作允许匿名（forum-service 内细粒度鉴权）
         │
         ▼
Forum-Service (Spring Security + 方法级鉴权)
   ├── @RateLimit 注解 → AOP 限流（基于 Redis）
   └── @AdminLog 注解 → AOP 操作日志记录
```

### 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

分页响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [],
    "total": 100,
    "page": 1,
    "pageSize": 20
  }
}
```

---

## 消息队列设计

使用 RocketMQ，生产者统一通过 `RocketMQProducer` 发送消息。

| Topic | 消费者 | 用途 |
|-------|--------|------|
| post-view-topic | `ViewCountConsumer` | 异步统计帖子浏览量 |
| post-sync-topic | `SearchSyncConsumer` | 帖子变更同步到 Elasticsearch |
| comment-event-topic | `CommentEventConsumer` | 评论事件通知 |
| like-event-topic | `LikeEventConsumer` | 点赞事件通知 |
| follow-event-topic | `FollowEventConsumer` | 关注事件通知 |

---

## 设计规范

### 设计理念

- **Minimal** — 极简，去除非必要元素
- **Modern** — 现代化，符合当前设计趋势
- **Content First** — 内容优先，减少装饰干扰
- **Card Layout** — 卡片式布局，信息分组清晰
- **Responsive** — 响应式设计，适配移动端

### 主题变量

| 变量 | 值 | 用途 |
|------|-----|------|
| `--color-primary` | `#2563EB` | 主色调 |
| `--color-bg` | `#F8FAFC` | 页面背景 |
| `--color-card` | `#FFFFFF` | 卡片背景 |
| `--color-text-primary` | `#0F172A` | 主要文字 |
| `--color-text-secondary` | `#64748B` | 次要文字 |
| `--color-border` | `#E2E8F0` | 边框色 |
| `--radius-lg` | `16px` | 圆角 |
| `--shadow-md` | `0 2px 8px rgba(0,0,0,0.05)` | 卡片阴影 |
| `--max-width` | `1280px` | 最大内容宽度 |

### 布局

```
┌─────────────────────────────────────────────┐
│                  Header (60px)               │
├────────┬──────────────────┬─────────────────┤
│  Left  │                  │     Right       │
│ Sidebar│     Content      │    Sidebar      │
│ 240px  │                  │    280px        │
│        │                  │                 │
└────────┴──────────────────┴─────────────────┘
         ◄──── max-width: 1280px ────►
```

### 前端规范

- Composition API + `<script setup>` + TypeScript strict
- 无内联样式，统一使用 SCSS + CSS 变量
- 必须处理 Loading、Error、Empty 三种状态
- 支持暗色模式（`dark.scss` + `useTheme` composable）
- 支持骨架屏（`SkeletonCard.vue`）

---

## 部署方式

### Docker Compose（生产环境）

```bash
cd deploy
cp .env.example .env
# 编辑 .env 配置密钥等敏感信息
docker compose up -d
```

### 服务端口

| 服务 | 端口 | 说明 |
|------|------|------|
| Nginx | 80 | 前端静态资源 + 反向代理 |
| Gateway | 8080 | API 网关 |
| Forum-Service | 8081 | 核心服务 |
| PostgreSQL | 5432 | 数据库 |
| Redis | 6379 | 缓存 |
| Elasticsearch | 9200 | 搜索引擎 |
| RocketMQ NameSrv | 9876 | 消息队列注册中心 |
| RocketMQ Broker | 10911 | 消息队列 Broker |
| MinIO | 9000 | 对象存储 API |
| MinIO Console | 9001 | 对象存储控制台 |
| Nacos | 8848 | 服务注册中心 |

### 本地开发

**后端：**
```bash
# 启动基础设施（PostgreSQL、Redis 等）
cd deploy && docker compose up -d postgres redis nacos

# 启动 forum-service
cd forum-service && mvn spring-boot:run
```

**前端：**
```bash
cd web
npm install
npm run dev     # http://localhost:5173
```

前端开发服务器已配置代理：`/api/*` → `http://localhost:8080`

---

## 前端路由

| 路径 | 页面 | 认证要求 |
|------|------|----------|
| `/` | 首页 | 无 |
| `/login` | 登录 | 无 |
| `/register` | 注册 | 无 |
| `/posts` | 帖子列表 | 无 |
| `/posts/create` | 发帖 | 需登录 |
| `/posts/:id` | 帖子详情 | 无 |
| `/posts/:id/edit` | 编辑帖子 | 需登录 |
| `/users/:id` | 用户主页 | 无 |
| `/profile` | 个人中心 | 需登录 |
| `/messages` | 消息中心 | 需登录 |
| `/search` | 搜索 | 无 |
| `/admin` | 管理后台 | 需 ADMIN 角色 |
| `/admin/users` | 用户管理 | 需 ADMIN 角色 |
| `/admin/posts` | 帖子审核 | 需 ADMIN 角色 |
| `/admin/comments` | 评论审核 | 需 ADMIN 角色 |
| `/admin/reports` | 举报处理 | 需 ADMIN 角色 |
| `/admin/roles` | 权限管理 | 需 ADMIN 角色 |
| `/admin/stats` | 统计分析 | 需 ADMIN 角色 |

---

## 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `POSTGRES_HOST` | PostgreSQL 地址 | localhost |
| `POSTGRES_PORT` | PostgreSQL 端口 | 5432 |
| `POSTGRES_DB` | 数据库名 | mindtalk |
| `POSTGRES_USER` | 数据库用户 | mindtalk |
| `POSTGRES_PASSWORD` | 数据库密码 | mindtalk123 |
| `REDIS_HOST` | Redis 地址 | localhost |
| `REDIS_PORT` | Redis 端口 | 6379 |
| `REDIS_PASSWORD` | Redis 密码 | - |
| `ES_URIS` | Elasticsearch 地址 | http://localhost:9200 |
| `ROCKETMQ_ADDR` | RocketMQ NameServer | localhost:9876 |
| `MINIO_ENDPOINT` | MinIO 地址 | http://localhost:9000 |
| `MINIO_ACCESS_KEY` | MinIO 访问密钥 | minioadmin |
| `MINIO_SECRET_KEY` | MinIO 密钥 | minioadmin123 |
| `NACOS_ADDR` | Nacos 地址 | 127.0.0.1:8848 |
| `JWT_SECRET` | JWT 签名密钥 | - |
| `LOG_LEVEL` | 日志级别 | INFO |
