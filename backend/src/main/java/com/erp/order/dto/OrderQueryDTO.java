package com.erp.order.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 订单列表查询条件。
 */
@Data
public class OrderQueryDTO {

    /** 订单状态，取值见 OrderStatus */
    private String status;

    private Integer customerId;

    /** 内部订单号模糊匹配 */
    private String orderNo;

    /** 客户订单号 / PO 号模糊匹配 */
    private String customerOrderNo;

    /** 下单日期起（含） */
    private LocalDate dateFrom;

    /** 下单日期止（含） */
    private LocalDate dateTo;

    private Integer pageNum = 1;

    private Integer pageSize = 20;
}
