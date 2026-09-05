package com.erp.report.vo;

import com.erp.payment.entity.Payment;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 对账单中的订单行：订单信息 + 出货信息 + 收款流水。
 */
@Data
public class StatementOrderVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String orderNo;
    /** 客户订单号 / PO 号 */
    private String customerOrderNo;
    private LocalDate orderDate;
    private BigDecimal totalAmount;
    private String status;
    private String statusLabel;
    private LocalDate dueDate;
    private String remark;

    /** 实际发货日 */
    private LocalDate shipmentDate;
    /** 物流单号 */
    private String trackingNo;

    /** 已收金额 */
    private BigDecimal paidAmount;
    /** 未收余额 */
    private BigDecimal balance;

    /** 该订单的收款流水 */
    private List<Payment> payments;
}
