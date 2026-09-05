# 简易 ERP 系统 v1.0.0 正式版

面向中小供货商的轻量**订单 + 欠款**管理工具，替代 Excel 手工台账。

技术栈：Spring Boot 3.2 + MyBatis-Plus + Vue 3 + Element-Plus + MySQL 8.0 + Docker Compose。

---

## 快速开始

### 方式一：Docker Compose 一键启动（推荐）

```bash
# 首次启动会自动执行 sql/init.sql 建表
docker compose up -d

# 前端  http://localhost
# 后端  http://localhost:8080
# 接口文档 http://localhost:8080/swagger-ui.html
```

### 方式二：本地开发

**前置依赖**：JDK 17+、Node.js 18+、MySQL 8.0+

```bash
# 1. 初始化数据库（正式部署只执行这一步，得到空库）
mysql -u root -p < sql/init.sql
# 可选（仅本地试用想看效果时）：导入演示数据，正式部署请勿导入
mysql -u root -p erp_db < sql/sample-data.sql

# 2. 启动后端（默认 8080，连接 localhost:3306/erp_db）
cd backend
mvn spring-boot:run

# 3. 启动前端（默认 5173，已配置 /api 代理到 8080）
cd frontend
npm install
npm run dev
```

默认数据库账号：`root / 123456`（本地开发请改为自己的密码，见 `backend/src/main/resources/application.yml`）。

### 方式三：免 MySQL 本地体验（H2 内存库）

本机未安装 MySQL 时可一键体验，数据存于内存，重启即清空：

```bash
cd backend
mvnw.cmd spring-boot:run -D"spring-boot.run.profiles=h2"

# 前端照常
cd ../frontend
npm install && npm run dev
```

---

## 验证情况

- 后端编译通过；H2 模式下完成「注册 → 登录 → 带 token 建客户 → 建订单 → 出货 → 确认发货 → 3 次分批收款 → 超收拦截 → 自动结清」全链路接口冒烟
- 认证专项：无 token 返回 401、重复注册/错误密码被拒、伪造 token 被拒、`/auth/me` 与业务接口带 token 正常
- 对账单勾稽（期末 = 期初 + 本期发货 − 本期回款）与月度报表数据经接口核验一致
- 前端 `vue-tsc` 类型检查 + 生产构建通过；6 个页面（含登录/注册）经 Playwright 浏览器冒烟，控制台零报错

---

## 目录导航

```
erp/
├── docs/                   # 项目全流程文档（章程/需求/选型/设计/计划/测试/部署/手册）
├── sql/
│   ├── init.sql            # 全量建表 DDL
│   └── sample-data.sql     # 演示数据
├── backend/                # Spring Boot 后端
├── frontend/               # Vue3 前端
└── docker-compose.yml      # 三服务编排
```

- 需求与验收标准 → [docs/02-需求规格与验收标准.md](docs/02-需求规格与验收标准.md)
- 架构与数据库设计 → [docs/04-详细设计.md](docs/04-详细设计.md)
- 部署与运维 → [docs/07-部署运维手册.md](docs/07-部署运维手册.md)
- 业务操作说明 → [docs/08-用户操作手册.md](docs/08-用户操作手册.md)

---

## 业务速览

### 订单状态机

```
draft ──确认发货(实际发货日+物流单号)──▶ shipped ──收款达额──▶ paid
  └────────────── 取消 ──────────────┘
```

| 状态 | 含义 | 可取消 | 计入应收欠款 |
| ---- | ---- | ---- | ---- |
| draft 待发货 | 已录入，未发货 | ✅ | ❌ |
| shipped 待付款 | 已发货，形成应收 | ❌ | ✅ |
| paid 已完成 | 款项收齐 | ❌ | ❌（欠款为 0） |
| cancelled 已取消 | 作废 | — | ❌ |

> 说明：流程为**一步直达发货**，无「计划发货 → 确认发货」两步；`pending` 仅作为历史数据兼容保留。

### 欠款口径

**仅「已出货（shipped）」订单计入应收欠款**，公式为 `订单总额 − 累计收款`。
`draft` 记为「未发货金额」在仪表盘单独展示，不计入应收。

**逾期账龄**：仪表盘的账龄分布按**约定收款日**（`due_date`，选填）起算逾期天数——约定 30 天、第 31 天还没收回就进入「逾期 0-30 天」档；未填约定收款日的订单回退按发货日起算；约定收款日未到的订单归入「未到期」档，各档合计恒等于应收欠款总额。

### 订单编号

- **内部订单号**：系统自动生成 `ORD + yyyyMMdd + 4位流水`（如 `ORD202609040001`），不可手改
- **客户订单号（PO 号）**：选填，由客户提供的原始单号，用于与客户对账与检索

---

## 上线前检查清单（v1.0）

**必做（安全）：**

- [ ] 修改数据库密码：复制 `.env.example` 为 `.env`，改 `DB_PASSWORD`
- [ ] 修改 JWT 密钥：`.env` 中把 `JWT_SECRET` 改为随机长字符串（≥32 字符），更换后所有人需重新登录
- [ ] 视情况设置注册邀请码：`.env` 中设置 `REGISTER_CODE` 后，注册必须携带该码（公网部署强烈建议；团队内网可信环境可留空开放注册）
- [ ] 生产档已默认启用：Swagger 关闭、Actuator 仅保留 health、日志收敛（`SPRING_PROFILES_ACTIVE=prod`）

**建议：**

- [ ] 配置每日备份：Windows 计划任务执行 `scripts/backup.ps1`（保留 30 天）
- [ ] 探活告警：定时请求 `/actuator/health`，非 200 通知
- [ ] 首次登录后录一笔真实走单验证：建客户 → 建订单 → 出货 → 确认发货 → 收款结清

**本地开发注意：** `application.yml` 中数据库密码默认值 `123456` 仅为本地开发兜底，生产请通过 `.env` 注入；CORS 仅放行 `localhost`，生产走 Nginx 同源无需跨域。

---

## 认证说明

- 打开系统先**注册 / 登录**（登录名 + 密码），登录态为 JWT（默认 12 小时有效，过期自动跳回登录页）
- 密码使用 **BCrypt** 哈希存储，数据库不落明文
- 除 `/api/auth/login`、`/api/auth/register` 外，所有接口都必须携带 `Authorization: Bearer <token>`
- 需要更严格的双层防护时，可在 `frontend/nginx.conf` 重新启用注释掉的 `auth_basic`

---

## Git 分支规范

| 分支 | 用途 |
| ---- | ---- |
| `main` | 可发布分支，每个迭代结束打 tag |
| `develop` | 集成分支 |
| `feature/xxx` | 功能分支，合并后删除 |

提交信息遵循 [Conventional Commits](https://www.conventionalcommits.org/)：`feat:` `fix:` `docs:` `test:` `refactor:` `chore:`。
