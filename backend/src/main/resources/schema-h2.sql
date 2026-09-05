-- =============================================================
-- H2 兼容 schema（MySQL 模式）
-- 用途：本机未安装 MySQL 时的免安装试运行（h2 profile）与集成测试
-- 说明：与 sql/init.sql 结构保持一致，仅去除 MySQL 专有语法
--       - status 由 ENUM 改为 VARCHAR + CHECK 约束
--       - 索引使用独立 CREATE INDEX（H2 不支持表内 INDEX 子句）
--       - 去除 ENGINE / CHARSET / ON UPDATE 语法
-- =============================================================

CREATE TABLE IF NOT EXISTS customers (
    id           INT PRIMARY KEY AUTO_INCREMENT,
    name         VARCHAR(100) NOT NULL,
    contact      VARCHAR(50),
    phone        VARCHAR(20),
    credit_limit DECIMAL(12,2) DEFAULT 0,
    remark       VARCHAR(255),
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_customer_name ON customers (name);
CREATE INDEX IF NOT EXISTS idx_customer_phone ON customers (phone);

CREATE TABLE IF NOT EXISTS orders (
    id                 INT PRIMARY KEY AUTO_INCREMENT,
    order_no           VARCHAR(20) UNIQUE NOT NULL,
    customer_order_no  VARCHAR(64),
    customer_id        INT      NOT NULL,
    order_date         DATE     NOT NULL,
    total_amount       DECIMAL(12,2) NOT NULL,
    status             VARCHAR(20) DEFAULT 'draft',
    expected_delivery  DATE,
    due_date           DATE,
    remark             VARCHAR(255),
    created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_orders_status CHECK (status IN ('draft','pending','shipped','paid','cancelled')),
    CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES customers (id)
);
CREATE INDEX IF NOT EXISTS idx_orders_customer ON orders (customer_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders (status);
CREATE INDEX IF NOT EXISTS idx_orders_order_date ON orders (order_date);
CREATE INDEX IF NOT EXISTS idx_orders_customer_order_no ON orders (customer_order_no);
CREATE INDEX IF NOT EXISTS idx_orders_cust_po ON orders (customer_id, customer_order_no);

CREATE TABLE IF NOT EXISTS shipments (
    id             INT PRIMARY KEY AUTO_INCREMENT,
    order_id       INT NOT NULL,
    shipment_date  DATE NOT NULL,
    tracking_no    VARCHAR(50),
    confirmed      TINYINT DEFAULT 0,
    remark         VARCHAR(255),
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_shipments_order UNIQUE (order_id),
    CONSTRAINT fk_shipments_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_shipments_date ON shipments (shipment_date);

CREATE TABLE IF NOT EXISTS payments (
    id            INT PRIMARY KEY AUTO_INCREMENT,
    order_id      INT NOT NULL,
    amount        DECIMAL(12,2) NOT NULL,
    received_date DATE NOT NULL,
    note          VARCHAR(255),
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_payments_order ON payments (order_id);
CREATE INDEX IF NOT EXISTS idx_payments_received_date ON payments (received_date);

CREATE TABLE IF NOT EXISTS order_items (
    id           INT PRIMARY KEY AUTO_INCREMENT,
    order_id     INT NOT NULL,
    product_name VARCHAR(100) NOT NULL,
    qty          INT NOT NULL,
    unit_price   DECIMAL(12,2) NOT NULL,
    amount       DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_order_items_order ON order_items (order_id);

CREATE TABLE IF NOT EXISTS order_no_sequence (
    biz_date    DATE PRIMARY KEY,
    current_val INT NOT NULL DEFAULT 0,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_user (
    id         INT PRIMARY KEY AUTO_INCREMENT,
    username   VARCHAR(32) NOT NULL,
    password   VARCHAR(100) NOT NULL,
    real_name  VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_user_username ON sys_user (username);
