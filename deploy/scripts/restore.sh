#!/bin/bash
#=======================================
# MindTalk 数据恢复脚本
#=======================================

set -e

# 配置
BACKUP_DIR="/opt/mindtalk/backups"
POSTGRES_CONTAINER="mindtalk-postgres"
PG_USER="${POSTGRES_USER:-mindtalk}"
PG_DB="${POSTGRES_DB:-mindtalk}"
REDIS_CONTAINER="mindtalk-redis"
REDIS_PASSWORD="${REDIS_PASSWORD:-}"

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

error_exit() {
    log "ERROR: $1"
    exit 1
}

# 显示可用备份
show_backups() {
    log "=== 可用的 PostgreSQL 备份 ==="
    ls -lh "$BACKUP_DIR"/pg_*.sql.gz 2>/dev/null || echo "没有找到备份文件"
    echo ""
}

# 恢复 PostgreSQL
restore_postgres() {
    local backup_file="$1"
    
    if [ ! -f "$backup_file" ]; then
        error_exit "备份文件不存在: $backup_file"
    fi
    
    log "正在恢复 PostgreSQL 数据库..."
    log "警告：这将覆盖现有数据！"
    
    read -p "确认恢复? (yes/no): " confirm
    if [ "$confirm" != "yes" ]; then
        log "取消恢复操作"
        exit 0
    fi
    
    gunzip -c "$backup_file" | docker exec -i "$POSTGRES_CONTAINER" psql -U "$PG_USER" -d "$PG_DB"
    
    if [ $? -eq 0 ]; then
        log "PostgreSQL 恢复成功"
    else
        error_exit "PostgreSQL 恢复失败"
    fi
}

# 恢复 Redis
restore_redis() {
    local backup_file="$1"
    
    if [ ! -f "$backup_file" ]; then
        error_exit "备份文件不存在: $backup_file"
    fi
    
    log "正在恢复 Redis 数据..."
    docker cp "$backup_file" "$REDIS_CONTAINER:/data/dump.rdb"
    docker restart "$REDIS_CONTAINER"
    
    if [ $? -eq 0 ]; then
        log "Redis 恢复成功（服务已重启）"
    else
        error_exit "Redis 恢复失败"
    fi
}

# 主菜单
show_menu() {
    echo ""
    echo "=================================="
    echo "   MindTalk 数据恢复工具"
    echo "=================================="
    echo ""
    echo "1. 恢复 PostgreSQL 数据库"
    echo "2. 恢复 Redis 数据"
    echo "3. 查看可用备份"
    echo "4. 退出"
    echo ""
}

# 主程序
while true; do
    show_menu
    read -p "请选择操作 (1-4): " choice
    
    case $choice in
        1)
            show_backups
            read -p "请输入备份文件路径: " backup_file
            restore_postgres "$backup_file"
            ;;
        2)
            read -p "请输入 Redis 备份文件路径: " backup_file
            restore_redis "$backup_file"
            ;;
        3)
            show_backups
            ;;
        4)
            log "退出"
            exit 0
            ;;
        *)
            echo "无效选择，请重新输入"
            ;;
    esac
done
