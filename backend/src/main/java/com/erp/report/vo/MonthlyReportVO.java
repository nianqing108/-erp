package com.erp.report.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 月度经营报表。
 */
@Data
public class MonthlyReportVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 查询月份 yyyy-MM */
    private String month;

    /** 本期下单订单数（不含取消） */
    private Long orderCount;

    /** 本期出货量 */
    private Long shipmentCount;

    /** 本期回款金额 */
    private java.math.BigDecimal receivedAmount;

    /** 订单完成率（%） */
    private java.math.BigDecimal completionRate;

    /** 近 12 个月趋势 */
    private List<MonthTrendVO> trend;

    /** 本期下单订单明细 */
    private List<java.util.Map<String, Object>> monthOrders;
}
