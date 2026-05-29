#!/usr/bin/env bash
set -euo pipefail

# ═══════════════════════════════════════════════
# MindTalk 一键部署脚本
# 用法: ./deploy.sh [--update] [--skip-build] [--skip-frontend]
# ═══════════════════════════════════════════════

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
WEB_DIR="$PROJECT_DIR/web"
SERVICE_DIR="$PROJECT_DIR/forum-service"
GATEWAY_DIR="$PROJECT_DIR/gateway"
ENV_FILE="$SCRIPT_DIR/.env"
ENV_EXAMPLE="$SCRIPT_DIR/.env.example"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log()  { echo -e "${GREEN}[INFO]${NC}  $*"; }
warn() { echo -e "${YELLOW}[WARN]${NC}  $*"; }
err()  { echo -e "${RED}[ERROR]${NC} $*"; exit 1; }

# ── 参数解析 ──
UPDATE=false
SKIP_BUILD=false
SKIP_FRONTEND=false
SKIP_BACKEND=false
for arg in "$@"; do
  case $arg in
    --update)       UPDATE=true ;;
    --skip-build)   SKIP_BUILD=true ;;
    --skip-frontend) SKIP_FRONTEND=true ;;
    --skip-backend)  SKIP_BACKEND=true ;;
    --help) echo "用法: ./deploy.sh [--update] [--skip-build] [--skip-frontend] [--skip-backend]"; exit 0 ;;
  esac
done

echo ""
echo "  ╔══════════════════════════════════╗"
echo "  ║   MindTalk 思享论坛 · 部署脚本  ║"
echo "  ╚══════════════════════════════════╝"
echo ""

# ═══════════════════════════════════════════════
# 1. 环境检查
# ═══════════════════════════════════════════════
log "检查运行环境..."

command -v docker  >/dev/null 2>&1 || err "请先安装 Docker"
command -v docker-compose >/dev/null 2>&1 || command -v docker >/dev/null 2>&1 || err "请先安装 Docker Compose"
command -v git    >/dev/null 2>&1 || err "请先安装 Git"

# docker compose 兼容（新版 Docker 用子命令）
DOCKER_COMPOSE="docker-compose"
if docker compose version >/dev/null 2>&1; then
  DOCKER_COMPOSE="docker compose"
fi

log "环境检查通过"
echo ""

# ═══════════════════════════════════════════════
# 2. 拉取代码
# ═══════════════════════════════════════════════
cd "$PROJECT_DIR"

if [ "$UPDATE" = true ]; then
  log "拉取最新代码..."
  git pull --ff-only || warn "git pull 失败，继续使用当前代码"
  echo ""
fi

# ═══════════════════════════════════════════════
# 3. 配置文件
# ═══════════════════════════════════════════════
log "检查配置文件..."
if [ ! -f "$ENV_FILE" ]; then
  if [ -f "$ENV_EXAMPLE" ]; then
    warn ".env 不存在，从 .env.example 复制"
    cp "$ENV_EXAMPLE" "$ENV_FILE"
    echo ""
    echo "  ⚠️  重要：请先编辑 $ENV_FILE 中的密码和密钥"
    echo "     至少修改: POSTGRES_PASSWORD, REDIS_PASSWORD, JWT_SECRET, MINIO_SECRET_KEY"
    echo ""
    read -rp "  是否已编辑完成？(y/n) " CONFIRM
    if [ "$CONFIRM" != "y" ]; then
      err "请先编辑 .env 后重新运行"
    fi
  else
    err ".env.example 不存在，请手动创建 $ENV_FILE"
  fi
else
  # 检查是否还在用默认密码
  if grep -q "请替换" "$ENV_FILE" 2>/dev/null; then
    warn ".env 中存在未修改的「请替换」占位符，请确认密码已修改"
  fi
fi
log "配置文件就绪"
echo ""

# ═══════════════════════════════════════════════
# 4. 构建前端
# ═══════════════════════════════════════════════
build_frontend() {
  log "构建前端..."
  cd "$WEB_DIR"

  # 安装依赖（有 package-lock.json 时跳过）
  if [ ! -d "node_modules" ] || [ ! -f "node_modules/.package-lock.json" ]; then
    log "安装前端依赖..."
    npm ci --silent 2>/dev/null || npm install --silent
  fi

  # 生产构建
  npm run build
  log "前端构建完成 → $WEB_DIR/dist"
  echo ""
}

if [ "$SKIP_BUILD" = false ] && [ "$SKIP_FRONTEND" = false ]; then
  # 检查前端是否有变更（仅 update 模式）
  if [ "$UPDATE" = true ] && [ -f "$WEB_DIR/dist/index.html" ]; then
    LAST_BUILD=$(stat -c %Y "$WEB_DIR/dist/index.html" 2>/dev/null || echo 0)
    RECENT_CHANGE=$(find "$WEB_DIR/src" -type f -newer "$WEB_DIR/dist/index.html" 2>/dev/null | wc -l)
    if [ "$RECENT_CHANGE" -eq 0 ]; then
      log "前端无变更，跳过构建 (--skip-frontend 可强制跳过)"
    else
      build_frontend
    fi
  else
    command -v node >/dev/null 2>&1 || err "请先安装 Node.js"
    command -v npm  >/dev/null 2>&1 || err "请先安装 npm"
    build_frontend
  fi
else
  log "跳过前端构建"
  echo ""
fi

# ═══════════════════════════════════════════════
# 5. 构建后端
# ═══════════════════════════════════════════════
build_backend() {
  log "构建 forum-service..."
  cd "$SERVICE_DIR"

  if command -v mvn >/dev/null 2>&1; then
    MVN="mvn"
  elif [ -f "./mvnw" ]; then
    MVN="./mvnw"
  else
    err "请先安装 Maven 或确保 mvnw 存在"
  fi

  $MVN package -DskipTests -Dmaven.test.skip=true -q
  log "forum-service 构建完成"

  log "构建 gateway..."
  cd "$GATEWAY_DIR"
  $MVN package -DskipTests -Dmaven.test.skip=true -q
  log "gateway 构建完成 → $GATEWAY_DIR/target"
  echo ""
}

if [ "$SKIP_BUILD" = false ] && [ "$SKIP_BACKEND" = false ]; then
  if [ "$UPDATE" = true ] && [ -d "$SERVICE_DIR/target" ] && [ -d "$GATEWAY_DIR/target" ]; then
    FORUM_CHANGED=$(find "$SERVICE_DIR/src" -name "*.java" -newer "$SERVICE_DIR/target" 2>/dev/null | wc -l)
    GATEWAY_CHANGED=$(find "$GATEWAY_DIR/src" -name "*.java" -newer "$GATEWAY_DIR/target" 2>/dev/null | wc -l)
    if [ "$FORUM_CHANGED" -eq 0 ] && [ "$GATEWAY_CHANGED" -eq 0 ]; then
      log "后端无变更，跳过构建 (--skip-backend 可强制跳过)"
    else
      build_backend
    fi
  else
    build_backend
  fi
else
  log "跳过后端构建"
  echo ""
fi

# ═══════════════════════════════════════════════
# 6. Docker 构建 & 启动
# ═══════════════════════════════════════════════
log "启动服务..."
cd "$SCRIPT_DIR"

# 构建镜像（仅 forum-service 和 gateway 需要，基础设施镜像不构建）
log "构建 Docker 镜像..."
$DOCKER_COMPOSE build forum-service gateway 2>&1 | tail -3

log "启动/更新容器..."
$DOCKER_COMPOSE up -d

echo ""
log "等待服务就绪..."
sleep 5

# ═══════════════════════════════════════════════
# 7. 健康检查
# ═══════════════════════════════════════════════
MAX_WAIT=60
WAITED=0
HEALTHY=false

while [ $WAITED -lt $MAX_WAIT ]; do
  if curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
    HEALTHY=true
    break
  fi
  sleep 3
  WAITED=$((WAITED + 3))
done

echo ""
if [ "$HEALTHY" = true ]; then
  log "✅ 后端服务健康"
else
  warn "⚠️  后端健康检查超时，请查看日志: $DOCKER_COMPOSE logs forum-service"
fi

# 前端检查
if curl -sf http://localhost > /dev/null 2>&1; then
  log "✅ 前端服务正常"
else
  warn "⚠️  前端无法访问，请检查 nginx 容器"
fi

echo ""
echo "  ╔══════════════════════════════════╗"
echo "  ║  🚀 部署完成！                  ║"
echo "  ║                                ║"
echo "  ║  前端:  http://localhost       ║"
echo "  ║  后端:  http://localhost:8080  ║"
echo "  ║                                ║"
echo "  ║  查看日志:                      ║"
echo "  ║  $DOCKER_COMPOSE logs -f       ║"
echo "  ╚══════════════════════════════════╝"
echo ""
