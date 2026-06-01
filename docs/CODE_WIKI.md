# MindTalk 思享论坛 - Code Wiki

> 本文档是 MindTalk 思享论坛项目的结构化知识文档，包含项目架构、模块职责、关键类说明、依赖关系及运行方式。

## 目录

1. [项目概述](#1-项目概述)
2. [技术架构](#2-技术架构)
3. [项目结构](#3-项目结构)
4. [核心模块详解](#4-核心模块详解)
5. [数据库设计](#5-数据库设计)
6. [API 设计](#6-api-设计)
7. [消息队列设计](#7-消息队列设计)
8. [前端架构](#8-前端架构)
9. [关键类与函数说明](#9-关键类与函数说明)
10. [依赖关系](#10-依赖关系)
11. [运行方式](#11-运行方式)

---

## 1. 项目概述

| 属性 | 说明 |
|------|------|
| 项目名称 | MindTalk（思享论坛） |
| 项目类型 | 现代化知识社区平台 |
| GroupId | com.mindtalk |
| 版本 | 1.0.0-SNAPSHOT |
| 架构 | 微服务（Spring Cloud Gateway + Nacos） |
| 构建工具 | Maven（父 POM + 多模块） |

### 1.1 项目特性

- **极简现代设计**：参考 Notion、Medium、GitHub、掘金等产品的设计风格
- **内容优先**：减少装饰干扰，聚焦内容本身
- **响应式布局**：支持桌面端和移动端
- **功能丰富**：帖子发布、评论互动、用户关注、私信通知、管理后台等

---

## 2. 技术架构

### 2.1 整体架构图

```
┌─────────────────────────────────────────────────────┐
│                    Nginx (:80)                       │
│             静态资源 + 反向代理                        │
└─────────────┬───────────────────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────────────────┐
│              Gateway (:8080)                         │
│     Spring Cloud Gateway + JWT 全局鉴权               │
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

### 2.2 技术栈

#### 后端技术栈

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

#### 前端技术栈

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
| ECharts | 6.1.0 | 图表库 |

---

## 3. 项目结构

```
mindtalk-forum/
├── pom.xml                          # Maven 父 POM
├── CLAUDE.md                        # Claude AI 项目规则
│
├── common/                          # 公共模块
│   ├── pom.xml
│   └── src/main/java/com/mindtalk/common/
│       ├── constant/
│       │   └── Constants.java              # 系统常量
│       ├── exception/
│       │   ├── BusinessException.java       # 业务异常
│       │   └── GlobalExceptionHandler.java # 全局异常处理
│       ├── model/
│       │   ├── Result.java                  # 统一响应体
│       │   ├── PageRequest.java             # 分页请求
│       │   └── PageResult.java              # 分页结果
│       └── utils/                           # 通用工具类
│
├── gateway/                         # API 网关
│   ├── pom.xml
│   └── src/main/java/com/mindtalk/gateway/
│       ├── GatewayApplication.java          # 启动类
│       ├── config/
│       │   └── GatewayCorsConfig.java      # CORS 配置
│       └── filter/
│           └── JwtAuthFilter.java          # JWT 全局鉴权过滤器
│
├── forum-service/                   # 核心业务服务
│   ├── pom.xml
│   └── src/main/java/com/mindtalk/forum/
│       ├── ForumServiceApplication.java    # 启动类
│       ├── config/                         # 配置类
│       ├── common/                         # 服务级公共组件
│       │   ├── annotation/                 # 注解
│       │   ├── aspect/                     # AOP 切面
│       │   ├── component/                  # 组件
│       │   └── utils/                      # 工具类
│       └── modules/                        # 业务模块
│
├── web/                             # 前端项目
│   ├── package.json
│   ├── vite.config.ts
│   └── src/
│       ├── main.ts
│       ├── App.vue
│       ├── router/                  # 路由配置
│       ├── stores/                  # Pinia 状态管理
│       ├── api/                    # API 接口层
│       ├── composables/            # 组合式函数
│       ├── layouts/               # 布局组件
│       ├── components/            # 通用组件
│       ├── views/                 # 页面视图
│       ├── assets/                # 静态资源
│       └── types/                 # 类型定义
│
├── deploy/                         # 部署配置
│   ├── docker-compose.yml
│   ├── .env.example
│   ├── nginx/
│   ├── postgres/
│   ├── elasticsearch/
│   ├── gateway/
│   ├── forum-service/
│   └── minio/
│
└── docs/                           # 文档
    ├── DEPLOY.md
    └── OPTIMIZATION.md
```

---

## 4. 核心模块详解

### 4.1 Common 模块

公共模块，提供跨服务共享的基础组件。

| 类 | 说明 |
|---|------|
| `Constants.java` | 系统常量定义 |
| `Result.java` | 统一响应体 |
| `PageRequest.java` | 分页请求模型 |
| `PageResult.java` | 分页结果模型 |
| `BusinessException.java` | 业务异常 |
| `GlobalExceptionHandler.java` | 全局异常处理器 |

### 4.2 Gateway 模块

API 网关，负责请求路由和全局 JWT 鉴权。

| 类 | 说明 |
|---|------|
| `GatewayApplication.java` | 网关启动类 |
| `GatewayCorsConfig.java` | CORS 跨域配置 |
| `JwtAuthFilter.java` | JWT 全局鉴权过滤器 |

### 4.3 Forum-Service 模块

核心业务服务，包含以下子模块：

#### 4.3.1 用户模块 (user)

| 类 | 说明 |
|---|------|
| `AuthController.java` | 认证接口（登录、注册、Token刷新） |
| `UserController.java` | 用户接口（个人信息、关注、取关） |
| `UserService.java` | 用户服务接口 |
| `UserServiceImpl.java` | 用户服务实现 |
| `User.java` | 用户实体 |
| `Role.java` | 角色实体 |
| `UserFollow.java` | 用户关注实体 |
| `LoginDTO.java` | 登录请求 DTO |
| `RegisterDTO.java` | 注册请求 DTO |

#### 4.3.2 帖子模块 (post)

| 类 | 说明 |
|---|------|
| `PostController.java` | 帖子 CRUD 接口 |
| `CategoryController.java` | 分类管理接口 |
| `TagController.java` | 标签管理接口 |
| `RssController.java` | RSS 订阅接口 |
| `PostService.java` | 帖子服务接口 |
| `Post.java` | 帖子实体 |
| `Category.java` | 分类实体 |
| `Tag.java` | 标签实体 |
| `PostDocument.java` | Elasticsearch 文档模型 |
| `ViewCountConsumer.java` | 浏览量异步统计消费者 |
| `SearchSyncConsumer.java` | ES 同步消费者 |

#### 4.3.3 评论模块 (comment)

| 类 | 说明 |
|---|------|
| `CommentController.java` | 评论接口 |
| `CommentService.java` | 评论服务接口 |
| `Comment.java` | 评论实体 |
| `Like.java` | 点赞实体 |
| `LikeEventConsumer.java` | 点赞事件消费者 |
| `CommentEventConsumer.java` | 评论事件消费者 |

#### 4.3.4 搜索模块 (search)

| 类 | 说明 |
|---|------|
| `SearchController.java` | 搜索接口 |
| `SearchService.java` | 搜索服务接口 |
| `SearchDTO.java` | 搜索请求 DTO |
| `SearchResultVO.java` | 搜索结果 VO |

#### 4.3.5 消息模块 (message)

| 类 | 说明 |
|---|------|
| `NotificationController.java` | 通知接口 |
| `ChatController.java` | 私信接口 |
| `NotificationSettingController.java` | 通知设置接口 |
| `NotificationService.java` | 通知服务 |
| `ChatService.java` | 私信服务 |
| `Notification.java` | 通知实体 |
| `Message.java` | 私信实体 |
| `Conversation.java` | 会话实体 |
| `FollowEventConsumer.java` | 关注事件消费者 |

#### 4.3.6 管理模块 (admin)

| 类 | 说明 |
|---|------|
| `AdminUserController.java` | 用户管理接口 |
| `AdminPostController.java` | 帖子审核接口 |
| `AdminCommentController.java` | 评论审核接口 |
| `AdminReportController.java` | 举报处理接口 |
| `AdminRoleController.java` | 权限管理接口 |
| `AdminStatsController.java` | 统计分析接口 |
| `SensitiveWordController.java` | 敏感词管理接口 |
| `AdminLogController.java` | 操作日志接口 |
| `AdminService.java` | 管理服务 |
| `AdminLog.java` | 管理员操作日志实体 |
| `Permission.java` | 权限实体 |
| `Report.java` | 举报实体 |

#### 4.3.7 订阅模块 (subscription)

| 类 | 说明 |
|---|------|
| `SubscriptionController.java` | 订阅接口 |
| `SubscriptionService.java` | 订阅服务 |
| `TagSubscription.java` | 标签订阅实体 |
| `CategorySubscription.java` | 分类订阅实体 |

#### 4.3.8 系列模块 (series)

| 类 | 说明 |
|---|------|
| `SeriesController.java` | 系列接口 |
| `SeriesService.java` | 系列服务 |
| `Series.java` | 系列实体 |
| `SeriesPost.java` | 系列帖子关联实体 |

#### 4.3.9 勋章模块 (badge)

| 类 | 说明 |
|---|------|
| `BadgeController.java` | 勋章接口 |
| `BadgeService.java` | 勋章服务 |
| `BadgeScheduler.java` | 勋章定时评估任务 |
| `Badge.java` | 勋章实体 |
| `UserBadge.java` | 用户勋章实体 |

#### 4.3.10 待读模块 (readlater)

| 类 | 说明 |
|---|------|
| `ReadLaterController.java` | 待读接口 |
| `ReadLaterService.java` | 待读服务 |
| `ReadLater.java` | 待读实体 |

#### 4.3.11 阅读历史模块 (reading)

| 类 | 说明 |
|---|------|
| `ReadingHistoryController.java` | 阅读历史接口 |
| `ReadingHistoryService.java` | 阅读历史服务 |
| `ReadingHistory.java` | 阅读历史实体 |

#### 4.3.12 文件模块 (file)

| 类 | 说明 |
|---|------|
| `FileController.java` | 文件上传下载接口 |

---

## 5. 数据库设计

### 5.1 数据库特性

- **数据库引擎**：PostgreSQL 16
- **逻辑删除**：所有表使用 `deleted` 字段
- **自动时间戳**：`create_time`、`update_time` 字段
- **扩展支持**：`pg_trgm`（模糊搜索）、`uuid-ossp`（UUID生成）

### 5.2 核心业务表

| 表名 | 说明 | 关键字段 |
|------|------|----------|
| `users` | 用户表 | username, email, password_hash, nickname, avatar_url, status |
| `posts` | 帖子表 | title, content, author_id, category_id, is_pinned, is_featured |
| `comments` | 评论表 | post_id, user_id, parent_id, reply_to_id, content |
| `categories` | 分类表 | name, icon, post_count |
| `tags` | 标签表 | name, post_count |
| `post_tags` | 帖子标签关联 | post_id, tag_id |

### 5.3 交互表

| 表名 | 说明 |
|------|------|
| `likes` | 点赞表（target_type: POST/COMMENT） |
| `collections` | 收藏表 |
| `follows` | 关注表 |

### 5.4 私信/通知表

| 表名 | 说明 |
|------|------|
| `conversations` | 会话表 |
| `messages` | 私信表 |
| `notifications` | 通知表（LIKE/COMMENT/FOLLOW/MENTION/SYSTEM） |

### 5.5 权限/管理表

| 表名 | 说明 |
|------|------|
| `roles` | 角色表（ADMIN/MODERATOR/USER） |
| `permissions` | 权限表（树形结构） |
| `role_permissions` | 角色权限关联 |
| `user_roles` | 用户角色关联 |
| `reports` | 举报表 |
| `admin_logs` | 管理员操作日志 |

---

## 6. API 设计

### 6.1 路由前缀

| 前缀 | 说明 |
|------|------|
| `/auth/**` | 认证接口（登录、注册、刷新Token） |
| `/api/**` | 业务接口（经 Gateway 路由后） |

### 6.2 认证流程

1. 用户登录 → 返回 `access_token`（30分钟） + `refresh_token`（7天）
2. 前端存储 Token 到 `sessionStorage`
3. Axios 拦截器自动注入 `Authorization: Bearer <token>` 请求头
4. Gateway `JwtAuthFilter` 解析 JWT，将用户信息写入请求头传递给下游
5. 白名单路径（`/auth/**`、`/doc.html` 等）跳过认证

### 6.3 鉴权层级

```
Gateway (全局 JWT 校验)
   ├── 白名单放行
   ├── 写操作强制认证
   └── 读操作允许匿名
         │
         ▼
Forum-Service (Spring Security + 方法级鉴权)
   ├── @RateLimit 注解 → AOP 限流（基于 Redis）
   └── @AdminLog 注解 → AOP 操作日志记录
```

### 6.4 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1234567890123
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

## 7. 消息队列设计

使用 RocketMQ 作为消息队列，实现异步事件处理。

### 7.1 Topic 定义

| Topic | 消费者 | 用途 |
|-------|--------|------|
| `post-view-topic` | `ViewCountConsumer` | 异步统计帖子浏览量 |
| `post-sync-topic` | `SearchSyncConsumer` | 帖子变更同步到 Elasticsearch |
| `comment-event-topic` | `CommentEventConsumer` | 评论事件通知 |
| `like-event-topic` | `LikeEventConsumer` | 点赞事件通知 |
| `follow-event-topic` | `FollowEventConsumer` | 关注事件通知 |

### 7.2 生产者

`RocketMQProducer.java` - 统一消息发送组件

---

## 8. 前端架构

### 8.1 项目结构

```
web/
├── src/
│   ├── main.ts                     # 应用入口
│   ├── App.vue                     # 根组件
│   ├── router/index.ts             # 路由配置
│   ├── stores/                    # Pinia 状态管理
│   │   ├── modules/user.ts        # 用户状态
│   │   └── modules/app.ts         # 应用状态
│   ├── api/                       # API 接口层
│   │   ├── request.ts            # Axios 实例
│   │   └── modules/              # 分模块 API
│   ├── composables/              # 组合式函数
│   │   ├── useAuth.ts           # 认证逻辑
│   │   ├── useTheme.ts          # 主题切换
│   │   ├── useSidebar.ts        # 侧边栏状态
│   │   └── useMarkdown.ts       # Markdown 渲染
│   ├── layouts/                  # 布局组件
│   ├── components/              # 通用组件
│   ├── views/                   # 页面视图
│   └── assets/styles/          # 样式文件
```

### 8.2 路由配置

| 路径 | 页面 | 认证要求 |
|------|------|----------|
| `/` | 首页 | 无 |
| `/login` | 登录 | 无 |
| `/register` | 注册 | 无 |
| `/posts` | 帖子列表 | 无 |
| `/posts/create` | 发帖 | 需登录 |
| `/posts/:id` | 帖子详情 | 无 |
| `/users/:id` | 用户主页 | 无 |
| `/profile` | 个人中心 | 需登录 |
| `/messages` | 消息中心 | 需登录 |
| `/admin` | 管理后台 | 需 ADMIN |

### 8.3 主题变量

| 变量 | 值 | 用途 |
|------|-----|------|
| `--color-primary` | `#2563EB` | 主色调 |
| `--color-bg` | `#F8FAFC` | 页面背景 |
| `--color-card` | `#FFFFFF` | 卡片背景 |
| `--color-text-primary` | `#0F172A` | 主要文字 |
| `--radius-lg` | `16px` | 圆角 |
| `--max-width` | `1280px` | 最大内容宽度 |

---

## 9. 关键类与函数说明

### 9.1 后端关键类

#### JwtUtils.java
JWT 工具类，提供 Token 生成和验证功能。

| 方法 | 说明 |
|------|------|
| `generateAccessToken(userId, username, role)` | 生成 Access Token |
| `generateRefreshToken(userId)` | 生成 Refresh Token |
| `validateToken(token)` | 校验令牌有效性 |
| `parseToken(token)` | 解析令牌 Claims |

#### Result.java
统一响应结果类。

| 方法 | 说明 |
|------|------|
| `ok()` | 成功响应（无数据） |
| `ok(data)` | 成功响应（带数据） |
| `fail(message)` | 失败响应 |
| `fail(code, message)` | 指定状态码失败响应 |
| `isSuccess()` | 判断是否成功 |

#### SecurityConfig.java
Spring Security 配置类。

| 配置项 | 说明 |
|--------|------|
| CSRF 禁用 | REST API 无状态 |
| 会话策略 | STATELESS 无状态会话 |
| 路径权限 | 白名单、公开读取、管理接口需 ADMIN |

#### JwtAuthFilter.java
JWT 认证过滤器，从请求头提取 Token 并设置认证信息。

### 9.2 前端关键模块

#### request.ts
Axios 实例封装，包含请求/响应拦截器。

```typescript
// 请求拦截器：自动注入 Token
// 响应拦截器：统一错误处理（401跳转登录、403权限不足、429限流）
```

#### useAuth.ts
认证组合式函数，提供登录、注册、登出功能。

#### useTheme.ts
主题切换组合式函数，支持亮色/暗色模式切换。

---

## 10. 依赖关系

### 10.1 Maven 依赖结构

```
mindtalk-forum (父 POM)
├── common (公共模块)
│   └── 无外部依赖（仅 JDK）
├── gateway (网关)
│   ├── common
│   ├── spring-cloud-starter-gateway
│   └── spring-cloud-starter-alibaba-nacos-discovery
└── forum-service (业务服务)
    ├── common
    ├── spring-boot-starter-web
    ├── spring-boot-starter-security
    ├── spring-boot-starter-data-redis
    ├── spring-boot-starter-data-elasticsearch
    ├── spring-boot-starter-aop
    ├── mybatis-plus-spring-boot3-starter
    ├── postgresql
    ├── rocketmq-spring-boot-starter
    ├── knife4j-openapi3-jakarta-spring-boot-starter
    ├── hutool-all
    ├── jjwt-api
    └── minio
```

### 10.2 前端依赖结构

```
mindtalk-web
├── vue + vue-router + pinia
├── element-plus + @element-plus/icons-vue
├── axios
├── echarts
├── vite + typescript
└── sass
```

---

## 11. 运行方式

### 11.1 Docker 部署（推荐）

```bash
# 进入部署目录
cd deploy

# 复制环境变量模板
cp .env.example .env

# 编辑 .env 配置密钥（可选）

# 启动所有服务
docker compose up -d
```

### 11.2 本地开发

#### 后端开发

```bash
# 启动基础设施
cd deploy && docker compose up -d postgres redis nacos

# 启动网关
cd gateway && mvn spring-boot:run

# 启动业务服务
cd forum-service && mvn spring-boot:run
```

#### 前端开发

```bash
cd web
npm install
npm run dev     # http://localhost:5173
```

### 11.3 服务端口

| 服务 | 端口 | 说明 |
|------|------|------|
| Nginx | 80 | 前端静态资源 + 反向代理 |
| Gateway | 8080 | API 网关 |
| Forum-Service | 8081 | 核心服务 |
| PostgreSQL | 5432 | 数据库 |
| Redis | 6379 | 缓存 |
| Elasticsearch | 9200 | 搜索引擎 |
| RocketMQ NameSrv | 9876 | 消息队列注册中心 |
| MinIO | 9000 | 对象存储 API |
| MinIO Console | 9001 | 对象存储控制台 |
| Nacos | 8848 | 服务注册中心 |

### 11.4 默认账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | admin123 | 超级管理员 |

---

## 附录

### A. 配置项说明

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `jwt.secret` | JWT 签名密钥 | mindtalk-forum-jwt-secret-key-2024 |
| `jwt.access-token-ttl` | Access Token 有效期 | 1800000（30分钟） |
| `jwt.refresh-token-ttl` | Refresh Token 有效期 | 604800000（7天） |
| `mybatis-plus.logic-delete-field` | 逻辑删除字段 | deleted |
| `minio.bucket` | MinIO 存储桶 | mindtalk |

### B. 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `POSTGRES_HOST` | 数据库地址 | localhost |
| `REDIS_HOST` | Redis 地址 | localhost |
| `ES_URIS` | Elasticsearch 地址 | http://localhost:9200 |
| `ROCKETMQ_ADDR` | RocketMQ 地址 | localhost:9876 |
| `MINIO_ENDPOINT` | MinIO 地址 | http://localhost:9000 |
| `NACOS_ADDR` | Nacos 地址 | 127.0.0.1:8848 |
| `JWT_SECRET` | JWT 签名密钥 | - |

---

*本文档由 Code Wiki 自动生成*
