package com.erp.payment.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单累计已收金额。
 */
@Data
public class OrderPaidVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer orderId;
    private BigDecimal paidAmount;
}
