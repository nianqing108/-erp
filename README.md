# 简易 ERP 系统

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
# 1. 初始化数据库
mysql -u root -p < sql/init.sql
# 可选：导入演示数据
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
draft ──生成出货单──▶ pending ──确认发货──▶ shipped ──收款达额──▶ paid
  └──────────── 取消 ──────────┘                   cancelled
```

| 状态 | 含义 | 可取消 | 计入应收欠款 |
| ---- | ---- | ---- | ---- |
| draft 录入 | 刚录入，未排产 | ✅ | ❌ |
| pending 待出货 | 已生成出货单，未发 | ✅ | ❌ |
| shipped 待付款 | 已发货，形成应收 | ❌ | ✅ |
| paid 已完成 | 款项收齐 | ❌ | ❌（欠款为 0） |
| cancelled 已取消 | 作废 | — | ❌ |

### 欠款口径

**仅「已出货（shipped）」订单计入应收欠款**，公式为 `订单总额 − 累计收款`。
`draft` 记为「在录金额」、`pending` 记为「待发货金额」，在仪表盘分档展示但不计入应收。

### 订单编号

- **内部订单号**：系统自动生成 `ORD + yyyyMMdd + 4位流水`（如 `ORD202609040001`），不可手改
- **客户订单号（PO 号）**：选填，由客户提供的原始单号，用于与客户对账与检索

---

## Git 分支规范

| 分支 | 用途 |
| ---- | ---- |
| `main` | 可发布分支，每个迭代结束打 tag |
| `develop` | 集成分支 |
| `feature/xxx` | 功能分支，合并后删除 |

提交信息遵循 [Conventional Commits](https://www.conventionalcommits.org/)：`feat:` `fix:` `docs:` `test:` `refactor:` `chore:`。
