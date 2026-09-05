package com.erp.dashboard.vo;

import com.erp.customer.entity.Customer;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 仪表盘总览。
 *
 * <p>发货流程已简化为「录入 → 直接发货」，不再有 pending 档；
 * 在录金额（draft）不计应收，仅 shipped 未结清计入应收欠款。
 */
@Data
public class DashboardVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 在录金额（draft） */
    private BigDecimal draftAmount;

    /** 本月订单额（本月下单、不含已取消） */
    private BigDecimal monthOrderAmount;

    /** 应收欠款（仅 shipped 未结清） */
    private BigDecimal receivable;

    /** 本月到期金额：约定收款日落在本月的未收余额（未填约定收款日不计入） */
    private BigDecimal monthDueAmount;

    /** 未到期金额：约定收款日（未填按发货日）尚未到达的未收余额 */
    private BigDecimal notDueAmount;

    /** 累计已收金额（全部收款记录） */
    private BigDecimal totalPaid;

    /** 本月回款金额 */
    private BigDecimal monthReceived;

    /** 未结清的已发货订单笔数 */
    private Long receivableOrderCount;

    /** 客户总数 */
    private Long customerCount;

    /** 账龄分布（四档） */
    private List<AgingBucketVO> aging;

    /** 欠款金额 Top 客户 */
    private List<Customer> topDebtCustomers;

    /** 统计基准日 */
    private LocalDate statDate;
}
