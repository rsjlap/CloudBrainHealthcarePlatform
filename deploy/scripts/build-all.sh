#!/usr/bin/env bash
# 一键构建前后端产物（Linux / macOS / Git Bash）
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
FRONTEND="$ROOT/cloud-brain-medical-frontend"
BACKEND="$ROOT/cloud-brain-medical-backend"
OUT="$ROOT/deploy/artifacts"

echo "==> 构建前端..."
cd "$FRONTEND"
npm ci
npm run build

echo "==> 构建后端 Jar..."
cd "$BACKEND"
mvn -q clean package -DskipTests

mkdir -p "$OUT/frontend" "$OUT/backend"
rm -rf "$OUT/frontend"/*
cp -r "$FRONTEND/dist/." "$OUT/frontend/"
cp "$BACKEND/target/cloud-brain-medical-backend-"*.jar "$OUT/backend/app.jar"

echo ""
echo "构建完成："
echo "  前端静态资源: $OUT/frontend/"
echo "  后端 Jar:       $OUT/backend/app.jar"
echo ""
echo "下一步：将 frontend 部署到 Nginx root，用 prod profile 运行 app.jar"
