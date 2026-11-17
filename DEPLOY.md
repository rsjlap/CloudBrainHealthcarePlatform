# 生产环境双端分离部署指南

前端打包为 **Nginx 静态资源**，后端打包为 **可独立运行的 Jar**，通过 Nginx 反向代理实现同源访问。

## 架构示意

```
浏览器
  │
  ▼
Nginx :80 / :443
  ├─ /              → Vue dist（静态 HTML/JS/CSS）
  └─ /api/          → Spring Boot Jar :8080（仅本机监听）
        └─ /api/v1/ws/  → WebSocket 升级
```

浏览器请求 `https://your-domain.com/api/v1/...` 与页面同源，**无需 CORS**。

---

## 第一步：前端构建

```bash
cd cloud-brain-medical-frontend
npm ci
npm run build
```

产物在 `dist/` 目录。生产环境变量见 `.env.production`：

- `VITE_API_BASE_URL=/api/v1` — 与 Nginx 反代路径一致

本地预览构建结果：

```bash
npm run preview
```

---

## 第二步：后端打包

```bash
cd cloud-brain-medical-backend
mvn clean package -DskipTests
```

产物：`target/cloud-brain-medical-backend-1.0.0-SNAPSHOT.jar`

运行（生产 profile）：

```bash
export SPRING_PROFILES_ACTIVE=prod
export DB_URL=jdbc:mysql://127.0.0.1:3306/cloud_brain_medical?...
export DB_USER=root
export DB_PASSWORD=***
export JWT_SECRET=your-long-secret
export AI_API_KEY=sk-***

java -jar target/cloud-brain-medical-backend-1.0.0-SNAPSHOT.jar
```

完整环境变量模板：`deploy/env/backend.prod.env.example`

**端口说明**：`application-prod.yml` 默认 `server.address=127.0.0.1`、`port=8080`，不对外暴露，仅 Nginx 本机反代。

---

## 第三步：配置 Nginx

项目已提供完整配置：`deploy/nginx/cloud-brain-medical.conf`

| 路径 | 作用 |
|------|------|
| `location /` | SPA `try_files`，支持 Vue Router history |
| `location /api/` | 反代到 `127.0.0.1:8080`（含 WebSocket、SSE） |
| `location ~ ^/(doc\.html\|...)` | Swagger / Knife4j 文档反代 |

### 3.1 安装 Nginx

**Linux 服务器**

```bash
# Ubuntu / Debian
sudo apt update && sudo apt install -y nginx

# CentOS / RHEL
sudo yum install -y nginx
```

**Windows 本地验证**

1. 从 [nginx.org](https://nginx.org/en/download.html) 下载 Stable 版 zip 并解压，例如 `C:\nginx`
2. 运行项目脚本（见 3.3）

### 3.2 部署静态资源

**手动（Linux）**

```bash
sudo mkdir -p /var/www/cloud-brain-medical/dist
sudo cp -r cloud-brain-medical-frontend/dist/* /var/www/cloud-brain-medical/dist/
```

**脚本（Linux 一键）**

```bash
sudo SERVER_NAME=your-domain.com bash deploy/scripts/setup-nginx-linux.sh
```

### 3.3 站点配置

**Linux — 手动**

```bash
sudo cp deploy/nginx/cloud-brain-medical.conf /etc/nginx/conf.d/
# 编辑 server_name、root 路径
sudo nginx -t
sudo systemctl enable nginx
sudo systemctl reload nginx
```

**Windows — 推荐脚本**

```powershell
# 构建前端 + 同步到 deploy/nginx/html + 生成配置
.\deploy\scripts\setup-nginx-windows.ps1

# 安装到 Nginx 目录（替换为你的解压路径）
.\deploy\scripts\setup-nginx-windows.ps1 -NginxHome C:\nginx

# 启动 Nginx（在 C:\nginx 下）
cd C:\nginx
start nginx
```

浏览器访问 **http://localhost**（需后端 Jar 已在 8080 运行）。

生成的文件：

- `deploy/nginx/html/` — 前端静态资源
- `deploy/nginx/cloud-brain-medical.generated.conf` — 带本机绝对路径的站点配置

### 3.4 HTTPS（推荐）

使用 certbot 或云厂商证书，在 `server` 块增加 `listen 443 ssl` 与证书路径。

---

## 第四步：CORS 策略

| 环境 | 配置 |
|------|------|
| 开发 | `application-dev.yml`：`app.cors.enabled=true`，允许 `localhost:5173` |
| 生产（Nginx 反代） | `app.cors.enabled=false`（默认） |
| 生产（前后端不同域） | `CORS_ENABLED=true`，在 `application-prod.yml` 配置 `allowed-origins` |

配置类：`CorsProperties.java` + `CorsConfig.java`

---

## 一键构建脚本

```bash
# Linux / Git Bash
bash deploy/scripts/build-all.sh

# Windows PowerShell
.\deploy\scripts\build-all.ps1
```

输出目录：

- `deploy/artifacts/frontend/` — 拷贝到 Nginx root
- `deploy/artifacts/backend/app.jar` — 上传到服务器运行

---

## systemd 托管后端（Linux 可选）

```bash
sudo mkdir -p /opt/cloud-brain-medical
sudo cp deploy/artifacts/backend/app.jar /opt/cloud-brain-medical/
sudo cp deploy/env/backend.prod.env.example /opt/cloud-brain-medical/backend.env
# 编辑 backend.env 填入真实配置

sudo cp deploy/systemd/cloud-brain-medical.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now cloud-brain-medical
sudo systemctl status cloud-brain-medical
```

---

## 部署检查清单

- [ ] MySQL 已建库并执行 `src/main/resources/db/` 下脚本
- [ ] `SPRING_PROFILES_ACTIVE=prod`
- [ ] `JWT_SECRET`、数据库密码已更换
- [ ] 后端 `curl http://127.0.0.1:8080/api/v1/departments` 有响应
- [ ] Nginx `curl http://your-domain/api/v1/departments` 有响应
- [ ] 浏览器登录、AI 流式病历、WebSocket 告警功能正常

---

## 常见问题

**502 Bad Gateway**  
后端未启动或 `upstream` 地址/port 不对。

**SSE / 流式病历卡住**  
确认 Nginx `proxy_buffering off` 已配置。

**WebSocket 连接失败**  
确认 `/api/v1/ws/` 的 `Upgrade` 配置；前端已使用同源 `ws(s)://域名/api/v1/ws/doctor`。

**开发环境 Vite 代理 WebSocket 报错**  
后端未启动时的正常现象；生产环境不经过 Vite。
