-- =============================================================
-- 简易ERP 数据库初始化脚本（MySQL 8.0）
-- 用途：生产/开发环境建库建表；Docker 首次启动自动挂载执行
-- 说明：orders.status 使用 ENUM 作为数据层兜底约束
-- =============================================================

CREATE DATABASE IF NOT EXISTS erp_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE erp_db;

-- ----------------------------
-- 客户表
-- ----------------------------
CREATE TABLE IF NOT EXISTS customers (
    id           INT PRIMARY KEY AUTO_INCREMENT,
    name         VARCHAR(100) NOT NULL COMMENT '公司名称',
    contact      VARCHAR(50)           COMMENT '联系人',
    phone        VARCHAR(20)           COMMENT '联系电话',
    credit_limit DECIMAL(12,2) DEFAULT 0 COMMENT '信用额度',
    remark       VARCHAR(255)          COMMENT '备注',
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_name (name),
    INDEX idx_phone (phone)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '客户表';

-- ----------------------------
-- 订单表
-- ----------------------------
CREATE TABLE IF NOT EXISTS orders (
    id                 INT PRIMARY KEY AUTO_INCREMENT,
    order_no           VARCHAR(20) UNIQUE NOT NULL COMMENT '内部订单号（系统生成，不可手改）',
    customer_order_no  VARCHAR(64)        COMMENT '客户订单号/PO号（选填，对账与检索用）',
    customer_id        INT      NOT NULL  COMMENT '客户ID',
    order_date         DATE     NOT NULL  COMMENT '下单日期',
    total_amount       DECIMAL(12,2) NOT NULL COMMENT '订单总额',
    status             ENUM('draft','pending','shipped','paid','cancelled') DEFAULT 'draft' COMMENT '订单状态',
    expected_delivery  DATE              COMMENT '期望发货日',
    due_date           DATE              COMMENT '约定付款到期日（选填，仅展示）',
    remark             VARCHAR(255)      COMMENT '备注',
    created_at         DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers (id),
    INDEX idx_customer (customer_id),
    INDEX idx_status (status),
    INDEX idx_order_date (order_date),
    INDEX idx_customer_order_no (customer_order_no),
    INDEX idx_cust_po (customer_id, customer_order_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单表';

-- ----------------------------
-- 出货单表（一个订单一条）
-- ----------------------------
CREATE TABLE IF NOT EXISTS shipments (
    id             INT PRIMARY KEY AUTO_INCREMENT,
    order_id       INT NOT NULL COMMENT '订单ID',
    shipment_date  DATE NOT NULL COMMENT '发货日期（生成时为计划发货日，确认发货后为实际发货日）',
    tracking_no    VARCHAR(50) COMMENT '物流单号',
    confirmed      TINYINT DEFAULT 0 COMMENT '是否已确认发货 0-计划 1-实际',
    remark         VARCHAR(255) COMMENT '备注',
    created_at     DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_order (order_id),
    FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    INDEX idx_shipment_date (shipment_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '出货单表';

-- ----------------------------
-- 收款记录表（一个订单可多笔分批收款）
-- ----------------------------
CREATE TABLE IF NOT EXISTS payments (
    id            INT PRIMARY KEY AUTO_INCREMENT,
    order_id      INT NOT NULL COMMENT '订单ID',
    amount        DECIMAL(12,2) NOT NULL COMMENT '收款金额',
    received_date DATE NOT NULL COMMENT '到账日',
    note          VARCHAR(255) COMMENT '备注（银行回单号等）',
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    INDEX idx_order (order_id),
    INDEX idx_received_date (received_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '收款记录表';

-- ----------------------------
-- 订单明细表（本期扩展预留，仅建表）
-- ----------------------------
CREATE TABLE IF NOT EXISTS order_items (
    id           INT PRIMARY KEY AUTO_INCREMENT,
    order_id     INT NOT NULL COMMENT '订单ID',
    product_name VARCHAR(100) NOT NULL COMMENT '产品名称',
    qty          INT NOT NULL COMMENT '数量',
    unit_price   DECIMAL(12,2) NOT NULL COMMENT '单价',
    amount       DECIMAL(12,2) NOT NULL COMMENT '金额（qty * unit_price，应用层计算）',
    FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    INDEX idx_order (order_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单明细表';

-- ----------------------------
-- 订单号按日序列表
-- ----------------------------
CREATE TABLE IF NOT EXISTS order_no_sequence (
    biz_date    DATE PRIMARY KEY COMMENT '业务日期',
    current_val INT NOT NULL DEFAULT 0 COMMENT '当日已发放最大流水号',
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '内部订单号按日流水序列表';

-- ----------------------------
-- 系统用户表（注册/登录）
-- ----------------------------
CREATE TABLE IF NOT EXISTS sys_user (
    id         INT PRIMARY KEY AUTO_INCREMENT,
    username   VARCHAR(32) NOT NULL COMMENT '登录名（唯一）',
    password   VARCHAR(100) NOT NULL COMMENT 'BCrypt 哈希',
    real_name  VARCHAR(50)  COMMENT '姓名',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '系统用户表';
