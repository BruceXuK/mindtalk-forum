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
| 前端 | Vue 3 + TypeScript + Vite + Pinia + Element Plus + SCSS |
| 后端 | Spring Boot 3.2 + Spring Cloud Gateway + MyBatis Plus |
| 数据库 | PostgreSQL 16 + Redis 7 + Elasticsearch 8 |
| 中间件 | RocketMQ 5.1 + Nacos 2.3 + MinIO |
| 部署 | Docker Compose + Nginx |

## 项目结构

```
mindtalk-forum/
├── common/          公共模块（常量、异常、模型）
├── gateway/         API 网关（Spring Cloud Gateway + JWT 鉴权）
├── forum-service/   核心业务服务（用户/帖子/评论/搜索/管理）
├── web/             Vue 3 前端
│   └── public/config.js   运行时配置（部署时可单独替换）
├── deploy/          Docker Compose 编排
├── sql/             数据库 DDL 脚本
└── docs/            文档
```

## 快速开始

### Docker 部署（推荐）

```bash
cd deploy
cp .env.example .env
# 编辑 .env 填入密钥
docker compose up -d
```

访问 http://localhost

### 本地开发

```bash
# 启动基础设施
cd deploy && docker compose up -d postgres redis nacos

# 启动后端
cd forum-service && mvn spring-boot:run
cd gateway && mvn spring-boot:run

# 启动前端
cd web && npm install && npm run dev
# → http://localhost:5173
```

## 前端运行时配置

`web/public/config.js` 支持部署时替换，无需重新构建：

```js
window.__APP_CONFIG__ = {
  apiBaseURL: '/api',
  siteName: 'MindTalk',
  siteURL: 'https://your-domain.com',
  // ...
}
```

## 功能

- 帖子发布/编辑/收藏/搜索（Markdown 编辑器）
- 评论/二级回复/点赞
- 用户关注/私信/通知
- 分类/标签/系列合集
- RSS 订阅
- 管理后台（用户管理/审核/权限/统计）
- 暗色模式 / 响应式布局 / 骨架屏
- PWA 离线支持
- 限流 / 操作日志 / JWT 鉴权

## 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `POSTGRES_HOST` | 数据库地址 | localhost |
| `REDIS_HOST` | Redis 地址 | localhost |
| `ES_URIS` | Elasticsearch 地址 | http://localhost:9200 |
| `ROCKETMQ_ADDR` | RocketMQ 地址 | localhost:9876 |
| `MINIO_ENDPOINT` | MinIO 地址 | http://localhost:9000 |
| `NACOS_ADDR` | Nacos 地址 | 127.0.0.1:8848 |
| `JWT_SECRET` | JWT 签名密钥 | - |

完整配置见 `deploy/.env.example`
