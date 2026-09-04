package com.erp.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 订单新增 / 编辑入参。
 *
 * <p>内部订单号由系统生成，不接受入参；客户订单号（PO 号）选填。
 */
@Data
public class OrderSaveDTO {

    @NotNull(message = "客户不能为空")
    private Integer customerId;

    @NotNull(message = "下单日期不能为空")
    private LocalDate orderDate;

    @NotNull(message = "订单金额不能为空")
    @DecimalMin(value = "0.01", message = "订单金额必须大于 0")
    private BigDecimal totalAmount;

    /** 期望发货日 */
    private LocalDate expectedDelivery;

    /** 约定付款到期日（选填，仅展示） */
    private LocalDate dueDate;

    /** 客户订单号 / PO 号，选填 */
    @Size(max = 64, message = "客户订单号长度不能超过 64 个字符")
    private String customerOrderNo;

    @Size(max = 255, message = "备注长度不能超过 255 个字符")
    private String remark;
}
