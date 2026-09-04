package com.erp.order.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 按状态聚合的订单金额与笔数（仪表盘三档金额数据源）。
 */
@Data
public class StatusAmountVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String status;
    private BigDecimal amount;
    private Long cnt;
}
