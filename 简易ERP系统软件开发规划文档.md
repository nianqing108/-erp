# 简易ERP系统软件开发规划文档

> 技术栈：SpringBoot 3.x + Vue3 + MySQL 8.0 + Element-Plus
> 面向中小企业轻量订单、欠款管理ERP，不含库存、采购、权限（初期）

---

## 1 引言

### 1.1 项目背景

中小企业大多依靠Excel开展订单业务管理，随着业务量增长，出现业务数据分散存储、客户欠款统计繁琐易错、订单业务状态难以追踪、出货收款信息割裂等痛点，缺少一套轻量化、低成本、上手快的订单ERP工具，因此开发本简易ERP系统解决上述业务问题。

### 1.2 项目目标

1. 实现订单全生命周期管理：录入→待出货→待付款→已完成完整业务流转；
2. 提供实时欠款统计能力：系统总欠款、单客户维度欠款查询；
3. 自动生成月度经营统计报表；
4. 独立管理已取消订单，取消订单不参与欠款核算；
5. 实现客户、出货、收款业务闭环，替代Excel手工台账。

### 1.3 项目范围

**包含功能**：客户管理、订单管理、出货管理、收款管理、欠款统计、月度经营报表、订单状态流转、对账单。

**不包含（本版本）**：库存管理、采购管理、用户权限管理、多仓库、审批流、消息通知、工资财务记账。

---

## 2 可行性分析

| 评估维度 | 评估结论 | 说明 |
| ---- | ---- | ---- |
| 技术可行性 | ✅ 可行 | Spring Boot + Vue3 技术栈成熟稳定，社区资料丰富，业务逻辑不复杂 |
| 经济可行性 | ✅ 可行 | 仅需要云服务器，预估月成本50元左右，无商业软件授权费用 |
| 操作可行性 | ✅ 可行 | 界面简洁，业务贴合中小企业现有台账习惯，操作人员3天即可上手 |
| 时间可行性 | ✅ 可行 | 3周迭代开发，第4周开展测试与上线部署 |

---

## 3 需求规格说明

### 3.1 功能需求

| 编号 | 功能模块 | 需求描述 | 优先级 |
| ---- | ---- | ---- | ---- |
| FR-01 | 客户管理 | 客户信息增删改查 | P0 |
| FR-02 | 订单录入 | 新建订单，关联客户，填写订单金额、期望发货日期 | P0 |
| FR-03 | 订单列表 | 按订单状态、客户、日期条件筛选，分页查询 | P0 |
| FR-04 | 订单状态流转 | 支持业务操作驱动订单状态流转 | P0 |
| FR-05 | 订单取消 | 仅未出货订单允许取消；已取消订单不计入欠款计算 | P1 |
| FR-06 | 出货管理 | 生成出货记录，登记发货日期、物流单号 | P0 |
| FR-07 | 收款管理 | 录入客户收款记录，系统自动判断订单是否结清 | P0 |
| FR-08 | 欠款总览 | 仪表盘展示系统总欠款、账龄分布 | P1 |
| FR-09 | 客户欠款 | 客户列表展示每个客户实时欠款金额 | P1 |
| FR-10 | 客户对账单 | 按客户查询全部订单、出货、收款明细，支持Excel导出 | P1 |
| FR-11 | 月度报表 | 统计月度订单量、出货量、回款金额、订单完成率，支持Excel导出 | P1 |
| FR-12 | 订单编号 | 订单编号系统自动生成，不可手动修改 | P0 |

### 3.2 非功能需求

| 编号 | 需求指标 |
| ---- | ---- |
| NFR-01 | 列表页面响应加载时间小于2秒 |
| NFR-02 | 支持最多10人同时在线并发操作 |
| NFR-03 | 订单状态流转、收款计算保证数据库事务、数据一致性 |
| NFR-04 | 兼容Chrome、Firefox、Edge主流最新浏览器 |
| NFR-05 | 仅录入(draft)状态订单允许编辑订单核心字段，其他状态禁止修改金额等关键信息 |

---

## 4 系统设计

### 4.1 系统架构

```text
┌─────────────────────────────────────────────────┐
│                   浏览器 (Vue3 SPA)              │
├─────────────────────────────────────────────────┤
│              Nginx (静态资源 + 反向代理)          │
├─────────────────────────────────────────────────┤
│            Spring Boot 3.x (REST API)           │
│  ┌──────────┐ ┌──────────┐ ┌──────────────┐   │
│  │Controller│ │ Service  │ │   Mapper     │   │
│  └──────────┘ └──────────┘ └──────────────┘   │
├─────────────────────────────────────────────────┤
│               MySQL 8.x 数据库                   │
└─────────────────────────────────────────────────┘
```

- 前端：Vue3 + Element-Plus，单页应用；
- Nginx：托管前端静态资源，API请求反向代理到后端服务；
- 后端：SpringBoot 3，RESTful接口，MyBatis-Plus操作数据库；
- 数据库：MySQL 8.0存储全部业务数据。

### 4.2 数据库ER核心关系

```text
customers (1) ──┬── (N) orders ──┬── (1) shipments
                │                 │
                │                 └── (N) payments
                └── (N) order_items
```

实体关系说明：

1. 一个客户对应多个订单；
2. 一个订单对应一条出货记录；
3. 一个订单可以有多条收款记录（分批收款）；
4. 一个订单包含多个订单明细条目。

### 4.3 订单状态机

```text
                    ┌──────────────┐
                    │   录入(draft) │
                    └──────┬───────┘
                           │ 生成出货单
                           ▼
                    ┌──────────────┐
            ┌───────│ 待出货(pending)│───────┐
            │       └──────┬───────┘       │
         取消              │ 确认发货       │ 可取消
            │              ▼                │
            │       ┌──────────────┐        │
            └──────▶│ 待付款(shipped)│───────┘
                    └──────┬───────┘
                           │ 录入收款(累计收款≥总额)
                           ▼
                    ┌──────────────┐
                    │  已完成(paid) │
                    └──────────────┘

                    ┌──────────────┐
                    │ 已取消(cancelled)│ ← 从draft/pending可转
                    └──────────────┘
```

业务约束：shipped、paid状态订单禁止取消；cancelled订单不再参与欠款统计。

### 4.4 RESTful API接口概览

| 方法 | 路径 | 功能 |
| ---- | ---- | ---- |
| GET | /api/orders | 订单列表（筛选+分页） |
| POST | /api/orders | 创建订单 |
| GET | /api/orders/{id} | 订单详情 |
| PUT | /api/orders/{id} | 编辑订单（仅draft允许编辑） |
| POST | /api/orders/{id}/cancel | 取消订单 |
| POST | /api/orders/{id}/ship | 生成出货单 draft→pending |
| POST | /api/orders/{id}/confirm-ship | 确认发货 pending→shipped |
| POST | /api/orders/{id}/pay | 录入收款记录 |
| GET | /api/customers | 客户列表 |
| GET | /api/customers/{id}/statement | 客户对账单 |
| GET | /api/dashboard | 仪表盘欠款统计 |
| GET | /api/reports/monthly | 月度报表 |
| GET | /api/reports/export-statement/{customerId} | 导出对账单Excel |
| GET | /api/reports/export-monthly | 导出月报Excel |

---

## 5 项目开发计划（3周迭代 + 第4周测试上线）

| 阶段 | 时间 | 任务 | 交付物 |
| ---- | ---- | ---- | ---- |
| 迭代1 | 第1周 | 环境搭建；客户CRUD；订单CRUD；订单编号自动生成；基础页面；数据库初始化脚本 | 可运行基础版本，客户、订单基础增删改查 |
| 迭代2 | 第2周 | 完整订单状态流转；生成出货、确认发货；收款录入；订单取消；事务处理；核心业务闭环 | 完整业务流程版本，走完订单正向、取消流程 |
| 迭代3 | 第3周 | 欠款统计、仪表盘；客户欠款；对账单；月度报表；Excel导出；前端页面完善 | 全部功能完整版本 |
| 验收上线 | 第4周 | 单元测试、接口测试、集成测试；bug修复；Docker部署；数据备份方案；简单操作手册 | 生产环境可用系统 |

---

## 6 测试计划

### 6.1 测试类型

| 测试类型 | 测试范围 | 通过标准 |
| ---- | ---- | ---- |
| 单元测试 | Service层核心业务逻辑（状态流转、欠款计算） | 代码覆盖率 > 70% |
| 接口测试 | 全部API接口端点 | Postman测试集合全部用例通过 |
| 集成测试 | 完整订单业务链路 | 5个核心场景全部跑通 |
| UI测试 | 核心页面操作流程 | 人工测试无阻塞bug |

### 6.2 关键测试场景

1. 正向完整流程：订单创建 → 生成出货单 → 确认发货 → 录入收款 → 订单已完成；
2. 订单创建，直接取消，校验取消订单不计入欠款；
3. 订单分批多次收款，校验欠款、结清状态计算正确；
4. 客户欠款、总欠款统计结果和手工台账核对一致；
5. 月度报表、对账单数据和底层明细数据保持一致；
6. 非法状态流转：已发货订单尝试取消，系统拦截抛出异常。

---

## 7 技术人员实现指引

### 7.1 环境要求

| 工具 | 版本 | 下载地址 |
| ---- | ---- | ---- |
| JDK | 17+ | https://adoptium.net/ |
| MySQL | 8.0+ | https://dev.mysql.com/downloads/ |
| Node.js | 18+ | https://nodejs.org/ |
| IDE | IDEA / VSCode | 根据开发人员偏好选择 |
| Git | 最新版 | https://git-scm.com/ |

### 7.2 建表SQL

```sql
CREATE DATABASE erp_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE erp_db;

-- 客户表
CREATE TABLE customers (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) UNIQUE NOT NULL COMMENT '公司名称',
    contact VARCHAR(50) COMMENT '联系人',
    phone VARCHAR(20) COMMENT '联系电话',
    credit_limit DECIMAL(10,2) DEFAULT 0 COMMENT '信用额度',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 订单表
CREATE TABLE orders (
    id INT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(20) UNIQUE NOT NULL COMMENT '订单编号',
    customer_id INT NOT NULL COMMENT '客户ID',
    order_date DATE NOT NULL COMMENT '下单日期',
    total_amount DECIMAL(10,2) NOT NULL COMMENT '订单总额',
    status ENUM('draft','pending','shipped','paid','cancelled') DEFAULT 'draft',
    expected_delivery DATE COMMENT '期望发货日',
    remark VARCHAR(255) COMMENT '备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(id),
    INDEX idx_customer (customer_id),
    INDEX idx_status (status),
    INDEX idx_order_date (order_date)
);

-- 出货单表
CREATE TABLE shipments (
    id INT PRIMARY KEY AUTO_INCREMENT,
    order_id INT NOT NULL COMMENT '订单ID',
    shipment_date DATE NOT NULL COMMENT '实际发货日',
    tracking_no VARCHAR(50) COMMENT '物流单号',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    INDEX idx_order (order_id)
);

-- 收款单表
CREATE TABLE payments (
    id INT PRIMARY KEY AUTO_INCREMENT,
    order_id INT NOT NULL COMMENT '订单ID',
    amount DECIMAL(10,2) NOT NULL COMMENT '收款金额',
    received_date DATE NOT NULL COMMENT '到账日',
    note VARCHAR(255) COMMENT '备注（银行回单号等）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    INDEX idx_order (order_id)
);

-- 订单明细表（扩展用）
CREATE TABLE order_items (
    id INT PRIMARY KEY AUTO_INCREMENT,
    order_id INT NOT NULL,
    product_name VARCHAR(100) NOT NULL,
    qty INT NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    amount DECIMAL(10,2) GENERATED ALWAYS AS (qty * unit_price) STORED,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);
```

### 7.3 SpringBoot核心代码模板

#### 统一返回 Result.java

```java
public class Result<T> {
    private int code;      // 200成功, 400参数错误, 500系统错误
    private String msg;
    private T data;

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.msg = "success";
        r.data = data;
        return r;
    }

    public static <T> Result<T> fail(int code, String msg) {
        Result<T> r = new Result<>();
        r.code = code;
        r.msg = msg;
        return r;
    }
    // getter/setter 省略
}
```

#### Order.java 实体

```java
@Data
@TableName("orders")
public class Order {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String orderNo;
    private Integer customerId;
    private LocalDate orderDate;
    private BigDecimal totalAmount;
    private String status;  // draft/pending/shipped/paid/cancelled
    private LocalDate expectedDelivery;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private String customerName;   // 联查用，非数据库字段
    @TableField(exist = false)
    private BigDecimal paidAmount; // 已收金额
}
```

#### OrderService.java

```java
@Service
@Transactional
public class OrderService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private ShipmentMapper shipmentMapper;
    @Autowired
    private PaymentMapper paymentMapper;

    /**
     * 生成出货单 draft → pending
     */
    public void shipOrder(Integer orderId, ShipmentDTO dto) {
        Order order = orderMapper.selectById(orderId);
        if (!"draft".equals(order.getStatus())) {
            throw new BusinessException("当前状态不可生成出货单");
        }
        Shipment shipment = new Shipment();
        shipment.setOrderId(orderId);
        shipment.setShipmentDate(dto.getShipmentDate());
        shipment.setTrackingNo(dto.getTrackingNo());
        shipmentMapper.insert(shipment);
        order.setStatus("pending");
        orderMapper.updateById(order);
    }

    /**
     * 确认发货 pending → shipped
     */
    public void confirmShip(Integer orderId) {
        Order order = orderMapper.selectById(orderId);
        if (!"pending".equals(order.getStatus())) {
            throw new BusinessException("仅待出货状态可确认发货");
        }
        order.setStatus("shipped");
        orderMapper.updateById(order);
    }

    /**
     * 录入收款：仅shipped待付款允许收款；累计收款≥总额自动变为paid
     */
    public void addPayment(Integer orderId, PaymentDTO dto) {
        Order order = orderMapper.selectById(orderId);
        if (!"shipped".equals(order.getStatus())) {
            throw new BusinessException("仅待付款状态订单可录入收款");
        }
        Payment payment = new Payment();
        payment.setOrderId(orderId);
        payment.setAmount(dto.getAmount());
        payment.setReceivedDate(dto.getReceivedDate());
        paymentMapper.insert(payment);

        BigDecimal paid = paymentMapper.sumByOrderId(orderId);
        if (paid.compareTo(order.getTotalAmount()) >= 0) {
            order.setStatus("paid");
        }
        orderMapper.updateById(order);
    }

    /**
     * 取消订单：仅 draft / pending 允许取消
     */
    public void cancelOrder(Integer orderId) {
        Order order = orderMapper.selectById(orderId);
        if (!"draft".equals(order.getStatus()) && !"pending".equals(order.getStatus())) {
            throw new BusinessException("已发货或已完成订单不可取消");
        }
        order.setStatus("cancelled");
        orderMapper.updateById(order);
    }
}
```

#### Mapper欠款SQL

```java
// 客户欠款：只统计有效订单，排除cancelled
@Select("""
    SELECT
        SUM(o.total_amount) - COALESCE(SUM(p.amount), 0) AS debt_amount
    FROM orders o
    LEFT JOIN payments p ON o.id = p.order_id
    WHERE o.customer_id = #{customerId}
      AND o.status IN ('draft', 'pending', 'shipped')
    GROUP BY o.customer_id
""")
BigDecimal getCustomerDebt(Integer customerId);

// 系统总欠款
@Select("""
    SELECT
        COALESCE(SUM(o.total_amount), 0) - COALESCE(SUM(p.amount), 0) AS total_debt
    FROM orders o
    LEFT JOIN payments p ON o.id = p.order_id
    WHERE o.status IN ('draft', 'pending', 'shipped')
""")
BigDecimal getTotalDebt();
```

### 7.4 Vue3脚手架

```bash
# 创建项目
npm create vue@latest erp-frontend
# 选择 Vue Router + Pinia，TypeScript可选

cd erp-frontend
npm install element-plus axios unplugin-auto-import unplugin-vue-components
```

**vite.config.js**

```js
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

export default defineConfig({
  plugins: [
    vue(),
    AutoImport({ resolvers: [ElementPlusResolver()] }),
    Components({ resolvers: [ElementPlusResolver()] }),
  ]
})
```

**src/api/order.js**

```javascript
import request from '@/utils/request'

export const orderApi = {
  getList(params) {
    return request.get('/orders', { params })
  },
  create(data) {
    return request.post('/orders', data)
  },
  ship(orderId, data) {
    return request.post(`/orders/${orderId}/ship`, data)
  },
  confirmShip(orderId) {
    return request.post(`/orders/${orderId}/confirm-ship`)
  },
  pay(orderId, data) {
    return request.post(`/orders/${orderId}/pay`, data)
  },
  cancel(orderId) {
    return request.post(`/orders/${orderId}/cancel`)
  }
}
```

**订单列表页面 order-list.vue**

```vue
<template>
  <div class="order-list">
    <!-- 筛选区 -->
    <el-form :model="queryParams" inline>
      <el-form-item label="状态">
        <el-select v-model="queryParams.status" placeholder="全部">
          <el-option label="录入" value="draft" />
          <el-option label="待出货" value="pending" />
          <el-option label="待付款" value="shipped" />
          <el-option label="已完成" value="paid" />
          <el-option label="已取消" value="cancelled" />
        </el-select>
      </el-form-item>
      <el-form-item label="客户">
        <el-select v-model="queryParams.customerId" filterable placeholder="选择客户">
          <el-option v-for="c in customerList" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="loadData">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
        <el-button type="success" @click="showCreateDialog">新增订单</el-button>
      </el-form-item>
    </el-form>

    <!-- 表格 -->
    <el-table :data="tableData" border>
      <el-table-column prop="orderNo" label="订单编号" width="180" />
      <el-table-column prop="customerName" label="客户" />
      <el-table-column prop="totalAmount" label="订单金额" width="120" />
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="statusTagMap[row.status]">{{ statusMap[row.status] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="360" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="viewDetail(row)">详情</el-button>
          <el-button size="small" type="primary" @click="handleShip(row)" v-if="row.status === 'draft'">生成出货</el-button>
          <el-button size="small" type="warning" @click="handleConfirmShip(row)" v-if="row.status === 'pending'">确认发货</el-button>
          <el-button size="small" type="success" @click="handlePay(row)" v-if="row.status === 'shipped'">收款</el-button>
          <el-button size="small" type="danger" @click="handleCancel(row)" v-if="['draft','pending'].includes(row.status)">取消</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <el-pagination v-model:current-page="pageNum" :total="total" @current-change="loadData" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { orderApi } from '@/api/order'

const queryParams = ref({ status: '', customerId: '' })
const tableData = ref([])
const pageNum = ref(1)
const total = ref(0)
const customerList = ref([])

const statusMap = {
  draft: '录入',
  pending: '待出货',
  shipped: '待付款',
  paid: '已完成',
  cancelled: '已取消'
}

const statusTagMap = {
  draft: 'info',
  pending: 'warning',
  shipped: 'primary',
  paid: 'success',
  cancelled: 'danger'
}

const loadData = async () => {
  const res = await orderApi.getList({
    page: pageNum.value,
    status: queryParams.value.status,
    customerId: queryParams.value.customerId
  })
  tableData.value = res.data.records
  total.value = res.data.total
}

onMounted(loadData)
</script>
```

### 7.5 Docker Compose部署脚本 docker-compose.yml

```yaml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: 123456
      MYSQL_DATABASE: erp_db
    ports:
      - "3306:3306"
    volumes:
      - ./mysql-data:/var/lib/mysql
      - ./init-sql:/docker-entrypoint-initdb.d
    restart: always

  backend:
    build: ./backend
    ports:
      - "8080:8080"
    depends_on:
      - mysql
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/erp_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: 123456
    restart: always

  frontend:
    build: ./frontend
    ports:
      - "80:80"
    depends_on:
      - backend
    restart: always
```

---

## 8 风险分析与应对措施

| 风险 | 概率 | 影响 | 应对措施 |
| ---- | ---- | ---- | ---- |
| 订单状态流转逻辑出错 | 中 | 高 | 编写单元测试覆盖全部状态流转分支；禁止非法状态跳转；数据库enum约束兜底 |
| 欠款统计计算结果不准 | 中 | 高 | 核心统计SQL充分测试；提供手工对账功能；数据库事务保证收款-欠款计算原子性 |
| 前端开发学习成本 | 低 | 中 | 基于Element-Plus组件库，复用模板代码；页面业务逻辑尽量下沉后端 |
| 生产环境数据丢失 | 低 | 高 | 配置MySQL定时自动备份脚本；docker数据卷持久化；定期导出备份文件 |
| 并发下超收款（收款金额大于订单总额） | 低 | 中 | 业务层增加校验，累计收款不可超过订单总额 |
| 日期时区问题 | 低 | 中 | 数据库、后端、docker统一Asia/Shanghai时区配置 |

---

## 9 验收标准

1. **订单完整业务流程**：录入 → 生成出货单 → 确认发货 → 录入收款 → 订单已完成，整套流程可顺畅执行；
2. **订单取消业务**：draft/pending订单可取消；shipped/paid不可取消；取消订单不计入欠款；
3. **欠款统计**：系统总欠款、客户欠款计算结果和手工台账一致；
4. **对账单**：客户订单、出货、收款明细完整，支持导出Excel；
5. **月度报表**：统计数据与底层业务明细数据完全匹配，支持导出；
6. **异常拦截**：非法状态操作系统给出明确提示，不会产生脏数据。

---

## 10 下一步行动

1. 执行建表SQL初始化MySQL数据库；
2. 创建SpringBoot + Vue3空白项目，完成数据库连通；
3. 开发客户CRUD模块、订单CRUD模块；
4. 完成迭代1，交付可运行基础版本。

---

## 附录：原版文档主要错误与遗漏汇总

1. **ER图关系错误**：orders与payments写成1对1；实际是一对多，一笔订单多笔收款。
2. **业务重大漏洞**：缺少 pending → shipped（确认发货）状态动作。原代码shipOrder只做draft→pending，没有pending转shipped逻辑，状态机断裂。
3. **收款校验条件bug**：原代码允许pending状态录入收款；业务逻辑应该只有shipped待付款才能收款。
4. **缺少订单编号自动生成逻辑**，业务不能人工填写order_no。
5. **缺少Excel导出能力**（对账单、月报），中小企业刚需。
6. **缺少订单编辑约束**：仅draft状态允许修改订单核心信息。
7. **MySQL连接字符串缺少时区参数**，部署会出现时间bug。
8. **缺少并发风险：超收款校验**（收款总和大于订单总额）。
9. Docker-compose没有挂载初始化SQL目录，首次启动不会自动导入表结构。
10. 非功能需求缺少数据导出相关指标。
