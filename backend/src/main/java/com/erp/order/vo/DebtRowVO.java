package com.erp.order.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 已发货订单的应收行：欠款统计与账龄分布的统一数据来源。
 *
 * <p>balance 由 SQL 计算得出（订单总额 − 已收金额）。
 * 账龄起算基准：优先约定收款日（dueDate），未填时回退发货日。
 */
@Data
public class DebtRowVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer orderId;
    private String orderNo;
    private String customerOrderNo;
    private Integer customerId;
    private String customerName;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal balance;
    /** 实际发货日（账龄兜底基准） */
    private LocalDate shipmentDate;
    /** 约定收款日（账龄优先基准） */
    private LocalDate dueDate;
}
