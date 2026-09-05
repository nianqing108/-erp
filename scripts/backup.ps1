# 简易ERP 每日备份脚本（Windows 计划任务调用示例见 docs/07）
# 用法：powershell -ExecutionPolicy Bypass -File scripts/backup.ps1
$ErrorActionPreference = "Stop"

$backupDir = Join-Path $PSScriptRoot "..\backup"
if (-not (Test-Path $backupDir)) { New-Item -ItemType Directory -Path $backupDir | Out-Null }

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$target = Join-Path $backupDir "erp_$timestamp.sql"

# 从 docker 容器内导出（密码取容器环境变量，不落盘）
docker exec erp-mysql sh -c 'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction erp_db' | Out-File -FilePath $target -Encoding utf8

if ((Get-Item $target).Length -lt 1KB) {
    Write-Error "备份文件异常过小，请检查容器状态：$target"
}

# 保留最近 30 天，其余清理
Get-ChildItem $backupDir -Filter "erp_*.sql" |
    Where-Object { $_.LastWriteTime -lt (Get-Date).AddDays(-30) } |
    Remove-Item -Force

Write-Host "备份完成：$target"
