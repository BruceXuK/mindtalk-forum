# MindTalk 定时任务配置

## 1. 备份脚本权限设置

```bash
chmod +x /opt/mindtalk/deploy/scripts/backup.sh
chmod +x /opt/mindtalk/deploy/scripts/restore.sh
```

## 2. Crontab 配置

编辑 crontab：
```bash
crontab -e
```

### 每日凌晨 3:00 自动备份
```cron
# 每天凌晨 3:00 执行备份
0 3 * * * /opt/mindtalk/deploy/scripts/backup.sh >> /opt/mindtalk/logs/backup.log 2>&1
```

### 每周日凌晨 2:00 执行完整备份（包括 MinIO）
```cron
# 每周日凌晨 2:00 执行完整备份
0 2 * * 0 /opt/mindtalk/deploy/scripts/backup_full.sh >> /opt/mindtalk/logs/backup.log 2>&1
```

### 每月 1 日凌晨 1:00 执行月度备份（保留 12 个月）
```cron
# 每月 1 日凌晨 1:00 执行月度备份
0 1 1 * * /opt/mindtalk/deploy/scripts/backup_monthly.sh >> /opt/mindtalk/logs/backup.log 2>&1
```

## 3. 查看 Crontab 状态

```bash
# 查看当前用户的 crontab
crontab -l

# 删除所有 crontab 任务
crontab -r
```

## 4. 备份验证

定期验证备份文件是否正常：
```bash
# 检查备份目录
ls -lh /opt/mindtalk/backups/

# 测试恢复（不要在生产环境直接测试）
/opt/mindtalk/deploy/scripts/restore.sh
```

## 5. 远程备份（可选）

### 同步到远程服务器
```bash
# 安装 rsync
apt install rsync

# 添加 rsync 备份任务
0 4 * * * rsync -avz --delete /opt/mindtalk/backups/ user@remote-server:/backup/mindtalk/ >> /opt/mindtalk/logs/rsync.log 2>&1
```

### 同步到云存储（以 S3 为例）
```bash
# 安装 s3cmd
apt install s3cmd

# 配置 s3cmd
s3cmd --configure

# 添加 S3 备份任务
0 5 * * * s3cmd sync /opt/mindtalk/backups/ s3://your-bucket/mindtalk-backups/ --delete-removed >> /opt/mindtalk/logs/s3.log 2>&1
```

## 6. 备份监控

### 设置邮件通知（需要配置邮件服务器）
```bash
# 安装 mailutils
apt install mailutils

# 编辑 crontab 添加邮件通知
MAILTO="admin@example.com"
0 3 * * * /opt/mindtalk/deploy/scripts/backup.sh >> /opt/mindtalk/logs/backup.log 2>&1
```

### 设置日志监控告警
```bash
# 检查备份日志中是否有错误
0 6 * * * grep -i "ERROR" /opt/mindtalk/logs/backup.log && mail -s "MindTalk Backup Alert" admin@example.com
```
