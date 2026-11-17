#!/usr/bin/env bash
# Linux 服务器 Nginx 部署脚本
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
FRONTEND="$ROOT/cloud-brain-medical-frontend"
NGINX_CONF="$ROOT/deploy/nginx/cloud-brain-medical.conf"
WEB_ROOT="${WEB_ROOT:-/var/www/cloud-brain-medical/dist}"
SERVER_NAME="${SERVER_NAME:-localhost}"
CONF_DST="${CONF_DST:-/etc/nginx/conf.d/cloud-brain-medical.conf}"

echo "==> 构建前端..."
cd "$FRONTEND"
npm ci
npm run build

echo "==> 部署静态资源到 $WEB_ROOT ..."
sudo mkdir -p "$WEB_ROOT"
sudo rm -rf "${WEB_ROOT:?}/"*
sudo cp -r dist/. "$WEB_ROOT/"

echo "==> 生成 Nginx 配置..."
TMP="$(mktemp)"
sed "s|root /var/www/cloud-brain-medical/dist;|root $WEB_ROOT;|" "$NGINX_CONF" \
  | sed "s|server_name localhost;|server_name $SERVER_NAME;|" > "$TMP"

sudo cp "$TMP" "$CONF_DST"
rm -f "$TMP"

echo "==> 检测并重载 Nginx..."
sudo nginx -t
sudo systemctl enable nginx 2>/dev/null || true
sudo systemctl reload nginx

echo ""
echo "部署完成："
echo "  静态资源: $WEB_ROOT"
echo "  Nginx 配置: $CONF_DST"
echo "  访问: http://$SERVER_NAME"
