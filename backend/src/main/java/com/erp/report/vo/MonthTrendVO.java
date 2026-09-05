package com.erp.report.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 月度趋势单月数据。
 */
@Data
public class MonthTrendVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 月份 yyyy-MM */
    private String month;

    /** 下单订单数（不含取消） */
    private Long orderCount;

    /** 出货量（确认发货笔数） */
    private Long shipmentCount;

    /** 回款金额 */
    private BigDecimal receivedAmount;

    /** 完成率（%） */
    private BigDecimal completionRate;
}
