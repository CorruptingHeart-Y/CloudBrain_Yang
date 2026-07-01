#!/usr/bin/env bash
# ============================================================
# AI 服务开发期一键重启脚本（Windows + Git Bash）
# ------------------------------------------------------------
# 做三件事：
#   1. 清理占用 8000 端口的所有进程（含 --reload 遗留的孤儿 worker）
#   2. 复查端口确已空闲，否则中止并提示手动处理
#   3. 以单进程启动 uvicorn（不带 --reload，Ctrl+C 可干净停止）
#
# 用法（在 ai-service 目录或任意目录均可）：
#   bash ai-service/dev.sh
#
# 为什么不用 --reload：
#   Windows 上 --reload 的 reloader 父子结构在 Ctrl+C 时常只杀父进程，
#   worker 子进程成孤儿继续占端口答旧状态（改 .env/加路由后不生效）。
#   开发期手动重启 + 单进程最稳。
# ============================================================
set -e

PORT=8000

echo "==> [1/3] 检查并清理占用 ${PORT} 端口的进程..."
# netstat -ano 输出：Proto  Local  Foreign  State  PID；取最后一列 PID
PIDS=$(netstat -ano | grep ":${PORT}" | awk '{print $5}' | sort -u | grep -v '^0$' || true)
if [ -n "$PIDS" ]; then
  for pid in $PIDS; do
    echo "    终止 PID ${pid}"
    taskkill //F //PID "${pid}" || true
  done
else
  echo "    端口空闲，无需清理"
fi

echo "==> [2/3] 复查端口..."
sleep 1
if netstat -ano | grep -q ":${PORT}"; then
  echo "!! 端口 ${PORT} 仍被占用："
  netstat -ano | grep ":${PORT}"
  echo "!! 请手动 taskkill //F //PID <PID> 后重试"
  exit 1
fi
echo "    端口已空闲 ✓"

echo "==> [3/3] 启动 AI 服务（单进程，Ctrl+C 干净停止）..."
cd "$(dirname "$0")"
# exec 用 uvicorn 进程替换当前 shell，Ctrl+C 信号直达 uvicorn，无中间层孤儿
exec uv run uvicorn app.main:app --port ${PORT}
