#!/bin/bash
#=======================================
# MindTalk 自动化备份脚本
# 建议通过 crontab 定时执行
#=======================================

# 配置
BACKUP_DIR="/opt/mindtalk/backups"
DATE=$(date +%Y%m%d_%H%M%S)
RETENTION_DAYS=7

# Docker 容器名称
POSTGRES_CONTAINER="mindtalk-postgres"
REDIS_CONTAINER="mindtalk-redis"
MINIO_BUCKET="mindtalk"

# Redis 密码（从环境变量获取或直接指定）
REDIS_PASSWORD="${REDIS_PASSWORD:-}"

# PostgreSQL 连接信息
PG_USER="${POSTGRES_USER:-mindtalk}"
PG_DB="${POSTGRES_DB:-mindtalk}"

# 日志函数
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

error_exit() {
    log "ERROR: $1"
    exit 1
}

# 创建备份目录
mkdir -p "$BACKUP_DIR" || error_exit "无法创建备份目录"

log "========== 开始备份 =========="

#=======================================
# 1. PostgreSQL 备份
#=======================================
log "1. 备份 PostgreSQL 数据库..."
PG_BACKUP_FILE="$BACKUP_DIR/pg_${DATE}.sql.gz"

docker exec "$POSTGRES_CONTAINER" pg_dump -U "$PG_USER" -d "$PG_DB" | gzip > "$PG_BACKUP_FILE"

if [ $? -eq 0 ] && [ -f "$PG_BACKUP_FILE" ]; then
    PG_SIZE=$(du -h "$PG_BACKUP_FILE" | cut -f1)
    log "   PostgreSQL 备份成功: $PG_BACKUP_FILE ($PG_SIZE)"
else
    error_exit "PostgreSQL 备份失败"
fi

#=======================================
# 2. Redis 备份
#=======================================
log "2. 备份 Redis 数据..."
REDIS_BACKUP_FILE="$BACKUP_DIR/redis_${DATE}.rdb"

docker exec "$REDIS_CONTAINER" redis-cli -a "$REDIS_PASSWORD" BGSAVE > /dev/null 2>&1
sleep 2
docker cp "$REDIS_CONTAINER:/data/dump.rdb" "$REDIS_BACKUP_FILE"

if [ $? -eq 0 ] && [ -f "$REDIS_BACKUP_FILE" ]; then
    REDIS_SIZE=$(du -h "$REDIS_BACKUP_FILE" | cut -f1)
    log "   Redis 备份成功: $REDIS_BACKUP_FILE ($REDIS_SIZE)"
else
    log "   WARNING: Redis 备份失败，继续其他备份"
fi

#=======================================
# 3. MinIO 文件备份（可选，文件较大）
#=======================================
# 如果需要备份 MinIO 文件，取消下面的注释
# log "3. 备份 MinIO 文件..."
# mc mirror "mindtalk/$MINIO_BUCKET" "$BACKUP_DIR/minio_${DATE}/"
# if [ $? -eq 0 ]; then
#     log "   MinIO 备份成功"
# else
#     log "   WARNING: MinIO 备份失败"
# fi

#=======================================
# 4. 清理过期备份
#=======================================
log "4. 清理 ${RETENTION_DAYS} 天前的备份..."
find "$BACKUP_DIR" -name "pg_*.sql.gz" -mtime +$RETENTION_DAYS -delete
find "$BACKUP_DIR" -name "redis_*.rdb" -mtime +$RETENTION_DAYS -delete
find "$BACKUP_DIR" -name "minio_*" -type d -mtime +$RETENTION_DAYS -exec rm -rf {} \; 2>/dev/null

#=======================================
# 5. 生成备份清单
#=======================================
BACKUP_LIST="$BACKUP_DIR/backup_list_${DATE}.txt"
echo "备份时间: $(date '+%Y-%m-%d %H:%M:%S')" > "$BACKUP_LIST"
echo "备份目录: $BACKUP_DIR" >> "$BACKUP_LIST"
echo "" >> "$BACKUP_LIST"
echo "=== 文件列表 ===" >> "$BACKUP_LIST"
ls -lh "$BACKUP_DIR" | tail -n +2 >> "$BACKUP_LIST"

log "========== 备份完成 =========="
log "备份清单: $BACKUP_LIST"

# 显示备份统计
echo ""
echo "=== 备份统计 ==="
echo "PostgreSQL: $(ls -lh "$BACKUP_DIR"/pg_*.sql.gz 2>/dev/null | wc -l) 个文件"
echo "Redis:      $(ls -lh "$BACKUP_DIR"/redis_*.rdb 2>/dev/null | wc -l) 个文件"
echo "总大小:     $(du -sh "$BACKUP_DIR" | cut -f1)"

exit 0
