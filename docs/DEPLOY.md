# MindTalk 部署指南

## 一、服务器要求

| 项目 | 最低配置 | 推荐配置 |
|------|----------|----------|
| CPU | 4 核 | 8 核 |
| 内存 | 8 GB | 16 GB |
| 磁盘 | 20 GB | 50 GB SSD |
| 系统 | Linux (Ubuntu 20.04+ / CentOS 7+) | Ubuntu 22.04 LTS |
| Docker | 24.0+ | 最新稳定版 |
| Docker Compose | v2.20+ | 最新稳定版 |

---

## 二、安装 Docker 环境

```bash
# Ubuntu
curl -fsSL https://get.docker.com | bash
sudo usermod -aG docker $USER
# 重新登录使权限生效

# 验证安装
docker --version
docker compose version
```

---

## 三、项目文件准备

将以下目录上传到服务器（如 `/opt/mindtalk/`）：

```
mindtalk-forum/
├── deploy/                    # Docker 编排
│   ├── docker-compose.yml
│   ├── .env.example
│   ├── nginx/
│   │   └── nginx.conf
│   ├── postgres/
│   │   └── init.sql
│   ├── elasticsearch/
│   │   └── Dockerfile
│   ├── forum-service/
│   │   └── Dockerfile
│   ├── gateway/
│   │   └── Dockerfile
│   └── minio/.gitkeep
├── forum-service/             # 后端源码（需先 mvn package）
│   ├── target/*.jar
│   └── ...
├── gateway/                   # 网关源码（需先 mvn package）
│   ├── target/*.jar
│   └── ...
├── web/
│   └── dist/                  # 前端构建产物（需先 npm run build）
└── sql/
    └── *.sql                  # 迁移脚本（仅参考，init.sql 会自动执行）
```

**最小部署包**（仅需这些文件即可运行）：

```
mindtalk-forum/
├── deploy/
│   ├── docker-compose.yml
│   ├── .env                  # 从 .env.example 复制并修改
│   ├── nginx/nginx.conf
│   ├── postgres/init.sql
│   ├── elasticsearch/Dockerfile
│   ├── forum-service/Dockerfile
│   └── gateway/Dockerfile
├── forum-service/target/*.jar
├── gateway/target/*.jar
└── web/dist/
```

---

## 四、构建步骤

### 1. 构建后端 JAR

```bash
# 在项目根目录执行
cd mindtalk-forum

# 构建 forum-service
cd forum-service
mvn package -Dmaven.test.skip=true
cd ..

# 构建 gateway
cd gateway
mvn package -Dmaven.test.skip=true
cd ..
```

### 2. 构建前端

```bash
cd web
npm install
npm run build
cd ..
```

### 3. 配置环境变量

```bash
cd deploy
cp .env.example .env
vim .env
```

修改 `.env` 中的密码为强密码：

```ini
POSTGRES_USER=mindtalk
POSTGRES_PASSWORD=<强密码>
REDIS_PASSWORD=<强密码>
MINIO_ACCESS_KEY=<强密码>
MINIO_SECRET_KEY=<强密码>
JWT_SECRET=<随机64位字符串>
```

生成 JWT 密钥：
```bash
openssl rand -base64 64
```

### 4. 构建并启动

```bash
cd deploy

# 构建镜像
docker compose build

# 启动所有服务（后台运行）
docker compose up -d

# 查看启动状态
docker compose ps

# 查看日志
docker compose logs -f forum-service
```

首次启动需要拉取基础镜像，约 5-10 分钟。

---

## 五、环境变量一览

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `POSTGRES_USER` | 数据库用户名 | `mindtalk` |
| `POSTGRES_PASSWORD` | 数据库密码 | **必须修改** |
| `REDIS_PASSWORD` | Redis 密码 | **必须修改** |
| `MINIO_ACCESS_KEY` | MinIO 访问密钥 | **必须修改** |
| `MINIO_SECRET_KEY` | MinIO 秘密密钥 | **必须修改** |
| `JWT_SECRET` | JWT 签名密钥 | **必须修改** |

---

## 六、服务端口

| 服务 | 端口 | 用途 |
|------|------|------|
| Nginx | 80 | 浏览器访问入口 |
| Gateway | 8080 | API 网关（内部） |
| Forum-Service | 8081 | 论坛后端（内部） |
| PostgreSQL | 5432 | 数据库 |
| Redis | 6379 | 缓存 |
| Elasticsearch | 9200 | 搜索引擎 |
| RocketMQ NameServer | 9876 | 消息队列 |
| RocketMQ Broker | 10911 | 消息队列 |
| MinIO API | 9000 | 对象存储 |
| MinIO Console | 9001 | 对象存储管理界面 |
| Nacos | 8848 | 服务注册中心 |

**生产环境建议**：仅对外开放 80 端口（Nginx），其余端口通过防火墙限制：

```bash
# 使用 ufw
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw allow 22/tcp
sudo ufw enable
```

---

## 七、HTTPS 配置

### 使用 Let's Encrypt

```bash
# 安装 certbot
sudo apt install certbot -y

# 获取证书（standalone 模式，需先停止 nginx）
docker compose stop nginx
sudo certbot certonly --standalone -d your-domain.com
docker compose start nginx
```

### 修改 Nginx 配置

在 `deploy/nginx/nginx.conf` 中添加 SSL 配置：

```nginx
server {
    listen       443 ssl;
    server_name  your-domain.com;

    ssl_certificate     /etc/letsencrypt/live/your-domain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/your-domain.com/privkey.pem;

    location / {
        root   /usr/share/nginx/html;
        index  index.html;
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        set $upstream gateway:8080;
        proxy_pass http://$upstream;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /auth/ {
        set $upstream gateway:8080;
        proxy_pass http://$upstream;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}

server {
    listen       80;
    server_name  your-domain.com;
    return       301 https://$host$request_uri;
}
```

更新 docker-compose.yml 添加证书挂载：

```yaml
nginx:
  volumes:
    - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
    - ../web/dist:/usr/share/nginx/html:ro
    - /etc/letsencrypt:/etc/letsencrypt:ro
```

---

## 八、数据备份

### PostgreSQL 备份

```bash
# 备份
docker exec mindtalk-postgres pg_dump -U mindtalk mindtalk > backup_$(date +%Y%m%d).sql

# 恢复
docker exec -i mindtalk-postgres psql -U mindtalk mindtalk < backup_20250101.sql
```

### 定时备份脚本（crontab）

```bash
# 每天凌晨 3 点备份，保留最近 7 天
0 3 * * * cd /opt/mindtalk && docker exec mindtalk-postgres pg_dump -U mindtalk mindtalk > backups/backup_$(date +\%Y\%m\%d).sql && find backups/ -name "backup_*.sql" -mtime +7 -delete
```

### MinIO 文件备份

```bash
# 安装 mc 客户端
wget https://dl.min.io/client/mc/release/linux-amd64/mc
chmod +x mc
./mc alias set mindtalk http://localhost:9000 <access_key> <secret_key>
./mc mirror mindtalk/mindtalk /backup/minio/
```

---

## 九、常用运维命令

### 启动/停止

```bash
cd /opt/mindtalk/deploy
docker compose up -d          # 启动所有服务
docker compose down           # 停止并删除容器
docker compose restart forum-service  # 重启单个服务
```

### 查看日志

```bash
docker compose logs -f forum-service   # 跟踪后端日志
docker compose logs -f nginx          # 跟踪 nginx 日志
docker compose logs --tail 100        # 查看最近 100 行
```

### 进入容器

```bash
docker exec -it mindtalk-postgres psql -U mindtalk   # 数据库
docker exec -it mindtalk-redis redis-cli -a <密码>    # Redis
```

### 运行数据库迁移

```bash
# 示例：执行 migration_008
docker exec -i mindtalk-postgres psql -U mindtalk mindtalk < sql/migration_008_sensitive_words.sql
```

### 初始化演示数据

```bash
docker exec -i mindtalk-postgres psql -U mindtalk mindtalk < deploy/seed.sql
```

---

## 十、资源优化建议

### Docker 内存限制

在 `docker-compose.yml` 中为每个服务添加资源限制：

```yaml
forum-service:
  deploy:
    resources:
      limits:
        memory: 1g
      reservations:
        memory: 512m
```

### JVM 参数

在 Dockerfile 中添加 JVM 参数：

```dockerfile
ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-jar", "app.jar"]
```

### 日志轮转

在 `docker-compose.yml` 中配置日志轮转：

```yaml
services:
  forum-service:
    logging:
      driver: "json-file"
      options:
        max-size: "50m"
        max-file: "5"
```

---

## 十一、故障排查

### 服务启动失败

```bash
# 查看服务状态
docker compose ps

# 查看失败服务的日志
docker logs mindtalk-forum-service --tail 100

# 常见原因：
# 1. 端口冲突 — netstat -tlnp 检查端口占用
# 2. 内存不足 — free -h 检查可用内存
# 3. 镜像拉取失败 — docker pull 重试
```

### ES 索引为空导致搜索无结果

```bash
# 检查 ES 状态
curl http://localhost:9200/_cat/indices

# 重建索引（需要论坛数据存在）
curl -X PUT "http://localhost:9200/mindtalk_posts"
```

### 数据库连接失败

```bash
# 检查 PostgreSQL 是否就绪
docker exec mindtalk-postgres pg_isready -U mindtalk

# 检查网络连通性
docker exec mindtalk-forum-service ping postgres
```

---

## 十二、版本信息

- **后端**: Spring Boot 3.x + MyBatis-Plus + PostgreSQL 16
- **前端**: Vue 3 + TypeScript + Vite + Element Plus
- **中间件**: Nacos 2.3, RocketMQ 5.1.4, Redis 7, Elasticsearch 8.12, MinIO latest
- **Java**: OpenJDK 21 (eclipse-temurin)
- **打包日期**: 2026-05
