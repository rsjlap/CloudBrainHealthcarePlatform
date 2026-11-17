# 东软智慧云脑诊疗平台 — 医护管理端

> 本仓库为云脑实训项目中**医护侧**交付内容，包含：医生工作台、系统管理员后台、检查医师端、药师端，以及对应的后端 API 与 Nginx 部署方案。  
> **患者端前端不在本仓库**；患者挂号与缴费由患者端同学对接本仓库提供的 REST 接口完成。

---

## 一、项目简介

### 1.1 背景与定位

东软智慧云脑诊疗平台采用前后端分离架构，本部分负责医院内部人员（医生、管理员、检查医师、药师）的日常业务操作，并集成 **DeepSeek 大模型** 与 **医疗知识图谱（KG）**，实现 AI 辅助病历生成、检查/处方推荐与审核等能力。

### 1.2 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3、TypeScript、Vite、Element Plus、Pinia、Vue Router、Axios、ECharts |
| 后端 | Spring Boot 3.2、Spring Security + JWT、MyBatis-Plus、Spring AI（DeepSeek） |
| 数据库 | MySQL 8.0 |
| 部署 | Nginx 静态托管 + 反向代理 Spring Boot Jar |
| 实时通信 | WebSocket（医生端高风险处方告警）、SSE（AI 流式病历） |

### 1.3 目录结构

```
job/
├── cloud-brain-medical-frontend/   # 医护端前端（Vue）
├── cloud-brain-medical-backend/    # 后端 API（Spring Boot）
├── deploy/                         # Nginx 配置、构建脚本、生产环境变量模板
├── DEPLOY.md                       # 详细部署说明（与本文互补）
└── README.md                       # 本文档
```

### 1.4 系统架构（生产）

```
浏览器
  │
  ▼
Nginx :80
  ├─ /              → 前端 dist 静态资源
  └─ /api/          → 后端 Jar :8080（本机监听）
        └─ /api/v1/ws/  → WebSocket
```

---

## 二、功能说明

### 2.1 角色与页面

| 角色 | 入口 | 主要功能 |
|------|------|----------|
| **医生** | `/doctor` | 患者队列、叫号、门诊病历、AI 口述转写、AI 病历生成（LLM + KG）、开检查/检验、开处方、签名、历史接诊 |
| **检查医师** | `/doctor/inspector` | 待检查队列（仅已缴费）、AI 生成报告、核对发布、历史记录 |
| **药师** | `/doctor/pharmacist` | 待发药队列（仅已缴费）、发药确认、历史记录 |
| **管理员** | `/admin` | 仪表盘、患者/员工管理、排班、挂号记录、药品目录、医疗设备 |

> 本前端**不包含患者登录入口**；患者账号登录会被拦截，患者业务由患者端项目对接后端接口。

### 2.2 核心业务链路（与患者端联调）

```
患者端：挂号 → 缴挂号费
    ↓
医生端：队列可见（未缴费显示「待缴」，叫号按钮禁用）→ 缴费后可叫号 → 写病历 / 开单 / 签名
    ↓
患者端：缴检查费 / 处方费
    ↓
检查医师 / 药师：队列可见 → 执行检查或发药
```

**缴费规则（后端控制）：**

- 挂号 `paid_at` 为空：医生可见患者，但**不能叫号**，不能写病历
- 检查单 `paid_at` 为空：检查医师队列**不可见**
- 处方 `paid_at` 为空：药师队列**不可见**

### 2.3 AI 能力（医生 / 管理员）

| 能力 | 说明 |
|------|------|
| 语音转写 | 科大讯飞听写，口述内容转文字 |
| AI 病历生成 | SSE 流式输出，融合知识图谱校验术语 |
| 检查推荐 | LLM + KG 融合推荐检查/检验项目 |
| 处方推荐与审核 | AI 推荐药品、风险等级审核，高风险 WebSocket 推送医生 |
| 处理意见优化 | 结合检查报告自动优化 treatment plan |
| 管理员 AI 排班 | DeepSeek 生成排班建议 |

### 2.4 提供给患者端的后端接口（联调）

患者端同学需对接以下接口（需 `PATIENT` 角色 JWT）：

| 接口 | 说明 |
|------|------|
| `POST /api/v1/registrations` | 患者挂号 |
| `GET /api/v1/registrations/my` | 我的挂号 |
| `GET /api/v1/payments/my-bills` | 账单列表 |
| `POST /api/v1/payments/registration/{id}` | 缴挂号费 |
| `POST /api/v1/payments/exam-order/{id}` | 缴检查/检验费 |
| `POST /api/v1/payments/prescription/{id}` | 缴处方费 |

完整 API 文档：后端启动后访问 `http://localhost:8080/doc.html`（Knife4j）。

---

## 三、安装部署指南

### 3.1 环境要求

| 组件 | 版本 |
|------|------|
| JDK | 17+ |
| Maven | 3.9+ |
| Node.js | 18+ |
| MySQL | 8.0+ |
| Nginx | 1.20+（生产部署） |

可选：DeepSeek API Key、知识图谱服务、科大讯飞语音配置。

### 3.2 数据库初始化

```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS cloud_brain_medical DEFAULT CHARSET utf8mb4;"
mysql -u root -p cloud_brain_medical < cloud-brain-medical-backend/src/main/resources/db/database-full.sql
```

远程组员访问 MySQL 可参考：`cloud-brain-medical-backend/src/main/resources/db/remote-access.sql`。

### 3.3 本地开发

**后端**

```bash
cd cloud-brain-medical-backend
# 可选：复制 .env.example 为 .env 并修改数据库、AI、KG 地址
mvn spring-boot:run
```

- 默认 profile：`dev`
- 地址：`http://localhost:8080`
- API 文档：`http://localhost:8080/doc.html`

**前端**

```bash
cd cloud-brain-medical-frontend
npm install
npm run dev
```

- 地址：`http://localhost:5173`
- 开发环境通过 Vite 代理 `/api` → `http://localhost:8080`

**环境变量参考**

- 本机 MySQL：`cloud-brain-medical-backend/.env.example`
- 组员连远程库：`cloud-brain-medical-backend/.env.team.example`

### 3.4 生产部署（Nginx + Jar）

**一键构建**

```powershell
# Windows
.\deploy\scripts\build-all.ps1

# Linux / Git Bash
bash deploy/scripts/build-all.sh
```

**Windows 部署 Nginx（本机验证）**

```powershell
cd deploy\scripts
.\setup-nginx-windows.ps1 -NginxHome "D:\Nginx\nginx-1.30.3"
cd D:\Nginx\nginx-1.30.3
.\nginx.exe -s reload
```

**启动后端 Jar**

```bash
cd cloud-brain-medical-backend
mvn clean package -DskipTests
# 配置环境变量（参考 deploy/env/backend.prod.env.example）
java -jar target/cloud-brain-medical-backend-1.0.0-SNAPSHOT.jar
```

浏览器访问：**http://localhost**（需 Nginx 与 Jar 均已启动）。

更详细的 HTTPS、systemd、CORS、故障排查见 **[DEPLOY.md](./DEPLOY.md)**。

---

## 四、使用说明

### 4.1 演示账号

执行 `database-full.sql` 后可用：

| 角色 | 用户名 | 密码 | 说明 |
|------|--------|------|------|
| 管理员 | admin | admin123 | 管理后台 |
| 医生 | doctor1 | doctor123 | 张医生 |
| 医生 | doctor2 | doctor123 | 李医生 |
| 检查医师 | inspector1 | doctor123 | 检查队列 |
| 药师 | pharmacist1 | doctor123 | 发药队列 |
| 患者 | patient1 | patient123 | **仅供患者端联调 API**，本前端不可登录 |

### 4.2 医生端操作流程

1. 使用 `doctor1` 登录，进入**医生工作台**
2. 左侧选择日期，查看当日患者队列
3. 患者**未缴挂号费**时：列表可见，「叫号」按钮灰色；患者缴费后刷新即可叫号
4. 叫号后填写门诊病历：可录音转写 → **AI 解析病历** → 一键填入 → 修改后签名
5. 签名时一并开立检查/检验与处方（患者需在患者端缴费后，检查医师/药师才能处理）
6. 「接诊历史」可查看已完成记录

### 4.3 检查医师 / 药师

- **检查医师**：登录后进入「待检查列表」，仅显示**患者已缴费**的检查单 → 生成/编辑报告 → 确认发布
- **药师**：登录后进入「待发药列表」，仅显示**已缴费处方** → 核对后发药

### 4.4 管理员

- **仪表盘**：注册患者、挂号、设备等统计
- **用户管理**：患者档案、医生/检查/药师账号
- **排班管理**：号源、AI 辅助排班
- **挂号记录**：查询筛选（只读，挂号由患者端发起）
- **药品 / 设备**：目录与设备使用登记

### 4.5 与患者端联调注意

1. 患者必须先**挂号并缴挂号费**，医生端才能叫号
2. 医生签名开单后，患者需缴**检查费/处方费**，检查医师和药师端才会出现任务
3. 若测试数据在开单时被误标为已缴费，需在库中将对应 `paid_at` 置为 `NULL` 后重测
4. 修改后端代码后需**重启 Jar**；仅改前端时需重新 `npm run build` 并同步 Nginx 静态文件

---

## 五、常见问题

| 现象 | 处理 |
|------|------|
| 502 Bad Gateway | 检查后端 Jar 是否在 8080 运行 |
| AI 病历生成失败但有 toast | 刷新页面；确认 DeepSeek Key 与 KG 地址可达 |
| SSE 流式卡住 | Nginx 需 `proxy_buffering off`（项目配置已包含） |
| 检查医师看不到单子 | 确认患者已在患者端缴纳该检查单费用 |
| 组员连不上数据库 | 使用 `.env.team.example`，MySQL 开放远程并执行 `remote-access.sql` |

---

## 六、交付说明

- **本仓库范围**：医生端、管理员端、检查医师端、药师端前端 + 全量后端 API + 部署脚本
- **不包含**：患者端 Vue 页面（患者端单独仓库/分工）
- **详细部署文档**：[DEPLOY.md](./DEPLOY.md)

如有联调或部署问题，可先查看 `doc.html` 接口文档与后端日志（`com.neusoft.cloudbrain` DEBUG 级别）。
