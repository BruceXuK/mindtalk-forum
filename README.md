<p align="center">
  <h1 align="center">MindTalk 思享论坛</h1>
  <p align="center">现代化知识社区平台</p>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Vue-3.4-4FC08D?logo=vuedotjs" alt="Vue">
  <img src="https://img.shields.io/badge/TypeScript-5.4-3178C6?logo=typescript" alt="TypeScript">
  <img src="https://img.shields.io/badge/Spring_Boot-3.2-6DB33F?logo=springboot" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk" alt="Java">
  <img src="https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql" alt="PostgreSQL">
</p>

---

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3 + TypeScript + Vite + Pinia + Vue Router + Element Plus + SCSS |
| 后端 | Spring Boot 3.2 + Spring Cloud Gateway + MyBatis Plus + Spring Security |
| 数据库 | PostgreSQL 16 + Redis 7 + Elasticsearch 8 |
| 中间件 | RocketMQ 5.1 + Nacos 2.3 + MinIO |
| 部署 | Docker Compose + Nginx |

## 项目结构

```
mindtalk-forum/
├── gateway/              API 网关（Spring Cloud Gateway + JWT 鉴权 + 路由转发）
├── forum-service/        核心业务服务（用户/帖子/评论/通知/管理后台）
│   └── src/main/
│       ├── java/com/mindtalk/forum/
│       │   ├── config/           安全/Web/CORS 配置
│       │   ├── common/           工具类（Redis/JWT）
│       │   └── modules/
│       │       ├── admin/        管理后台（用户/帖子/评论/举报/公告/日志/统计）
│       │       ├── comment/      评论
│       │       ├── message/      私信/通知
│       │       ├── post/         帖子/分类/标签/RSS
│       │       ├── report/       举报提交
│       │       ├── search/       搜索
│       │       ├── subscription/ 关注/收藏
│       │       └── user/         用户/认证
│       └── resources/
│           └── mapper/           MyBatis XML
├── web/                  Vue 3 前端
│   └── src/
│       ├── api/          API 请求模块
│       ├── components/   通用组件
│       ├── composables/  组合式函数（useMarkdown/useMention/useMarkdownToolbar）
│       ├── layouts/      布局组件
│       ├── router/       路由配置
│       ├── stores/       Pinia 状态管理
│       ├── types/        TypeScript 类型定义
│       └── views/        页面视图
├── deploy/               Docker Compose 编排 + Nginx + SQL + 部署脚本
│   ├── deploy.sh         一键部署脚本
│   ├── docker-compose.yml
│   ├── .env.example      环境变量模板
│   ├── nginx/            Nginx 配置
│   ├── postgres/         init.sql（DDL + 初始数据） + reset.sql
│   └── seed.sql          示例数据
└── init.sql              根目录初始化 SQL（指向 deploy/postgres/init.sql）
```

---

## 快速开始

### 生产部署（Docker Compose）

```bash
# 1. 克隆代码
git clone <your-repo-url> /opt/mindtalk && cd /opt/mindtalk

# 2. 创建环境配置
cp deploy/.env.example deploy/.env
vim deploy/.env    # 修改密码和 JWT_SECRET

# 3. 一键部署（构建前端 + 后端 + Docker 镜像 + 启动）
cd deploy && ./deploy.sh
```

部署脚本支持增量更新：

```bash
./deploy.sh --update               # git pull + 智能跳过未变更模块
./deploy.sh --update --skip-frontend  # 只更新后端
./deploy.sh --update --skip-backend   # 只更新前端
```

访问 `http://localhost`

### 本地开发

```bash
# 1. 启动基础设施
cd deploy && docker compose up -d postgres redis nacos minio elasticsearch rocketmq-namesrv rocketmq-broker

# 2. 启动后端（两个终端）
cd forum-service && mvn spring-boot:run    # :8081
cd gateway && mvn spring-boot:run           # :8080

# 3. 启动前端
cd web && npm install && npm run dev        # :5173 → proxy → :8080
```

---

## 默认管理员账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| `admin` | `admin123` | 超级管理员 |

> 首次部署时 `init.sql` 自动建表并初始化角色、权限、勋章和超管账号。

---

## 数据库管理

`deploy/postgres/init.sql` 在 PostgreSQL 容器首次启动时自动执行，包含：

| 内容 | 说明 |
|------|------|
| 29 张业务表 | 用户、角色、权限、帖子、评论、标签、分类、点赞、收藏、关注、私信、通知、举报、公告、操作日志等 |
| 角色（3 条） | ADMIN / MODERATOR / USER |
| 权限（27 条） | 管理后台菜单 + 操作按钮权限 |
| 勋章（8 条） | 内容创作、社交互动类成就定义 |
| 超管用户 | admin / admin123 |

### 重置数据

保留配置数据（角色、权限、勋章、超管），清空所有业务数据：

```bash
cd deploy
docker exec -i mindtalk-postgres psql -U mindtalk -d mindtalk < postgres/reset.sql
```

### 导入示例数据

```bash
cd deploy
docker exec -i mindtalk-postgres psql -U mindtalk -d mindtalk < seed.sql
```

---

## 环境变量

完整配置见 `deploy/.env.example`，关键变量：

| 变量 | 说明 | 生产部署 |
|------|------|----------|
| `JWT_SECRET` | JWT 签名密钥 | **必须修改**为随机字符串 |
| `POSTGRES_PASSWORD` | 数据库密码 | **必须修改** |
| `REDIS_PASSWORD` | Redis 密码 | **必须修改** |
| `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY` | MinIO 凭证 | **必须修改** |
| `MINIO_PUBLIC_ENDPOINT` | MinIO 对外访问地址 | 有域名后改为 `https://域名/storage` |
| `RSS_SITE_URL` | RSS 站点 URL | 有域名后改为实际 URL |
| `CORS_ALLOWED_ORIGINS` | 跨域白名单 | 有域名后改为具体域名 |

生产环境生成随机密钥：

```bash
openssl rand -base64 48   # JWT Secret
openssl rand -base64 32   # 其他密码
```

---

## 功能

- **帖子** — Markdown 编辑器 + 工具栏 + 实时预览 + 图片拖拽上传 + `@` 提及 + `#标签名` 自动提取
- **评论** — 二级回复 + 点赞
- **用户** — 注册/登录 + 个人主页 + 关注 + 私信 + 通知
- **内容组织** — 分类 + 标签（`#标签名` 自动创建） + 系列合集
- **管理后台** — 用户管理 + 帖子/评论审核 + 举报处理 + 标签管理（合并/启禁） + 分类管理 + 公告 + 敏感词 + 操作日志 + 统计图表
- **RSS** — 全站 / 分类 / 用户维度订阅
- **搜索** — Elasticsearch 全文搜索
- **UI** — 暗色模式 + 响应式布局 + 骨架屏 + 空状态 + PWA 离线支持
- **安全** — JWT 鉴权 + 接口限流 + 操作日志 + RBAC 权限
- **缓存** — Redis 缓存 + 防雪崩/穿透/击穿策略

---

## 运维

### 日志查看

```bash
cd deploy
docker compose logs -f forum-service    # 业务日志
docker compose logs -f nginx            # 访问日志
```

### 数据库备份

```bash
# 手动备份
docker exec mindtalk-postgres pg_dump -U mindtalk mindtalk | gzip > mindtalk_$(date +%Y%m%d).sql.gz

# 定时备份（crontab，每天凌晨 2 点）
0 2 * * * docker exec mindtalk-postgres pg_dump -U mindtalk mindtalk | gzip > /backup/mindtalk_$(date +\%Y\%m\%d).sql.gz
```

### 磁盘清理

```bash
docker system prune -a --volumes -f
```

### 健康检查

```bash
curl http://localhost:8080/actuator/health
```
