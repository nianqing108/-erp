package com.erp.report.vo;

import com.erp.payment.entity.Payment;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 客户对账单。
 *
 * <p><b>勾稽关系</b>：期末欠款 = 期初欠款 + 本期发货额 − 本期回款额。
 * 对账单仅含已实际发货订单（未发货不形成应收，已取消不参与对账）。
 */
@Data
public class StatementVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer customerId;
    private String customerName;
    private LocalDate from;
    private LocalDate to;

    /** 期初欠款：起始日之前已发货未结清余额 */
    private BigDecimal openingDebt;

    /** 本期发货额：本区间确认发货订单金额合计 */
    private BigDecimal periodShippedAmount;

    /** 本期回款额：本区间到账金额合计 */
    private BigDecimal periodReceivedAmount;

    /** 期末欠款 = 期初 + 本期发货 − 本期回款 */
    private BigDecimal closingDebt;

    /** 本期发货订单明细（含每单出货与收款流水） */
    private List<StatementOrderVO> orders;

    /** 本期其他回款：前期发货订单在本期的回款（不归属于本期发货订单） */
    private List<Payment> otherPayments;
}
