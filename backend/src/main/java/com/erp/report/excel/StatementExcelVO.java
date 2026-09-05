package com.erp.report.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 对账单 Excel 行。
 */
@Data
public class StatementExcelVO {

    @ExcelProperty("订单号")
    private String orderNo;

    @ExcelProperty("客户订单号")
    private String customerOrderNo;

    @ExcelProperty("下单日期")
    @DateTimeFormat("yyyy-MM-dd")
    private LocalDate orderDate;

    @ExcelProperty("发货日期")
    @DateTimeFormat("yyyy-MM-dd")
    private LocalDate shipmentDate;

    @ExcelProperty("订单金额")
    private BigDecimal totalAmount;

    @ExcelProperty("已收金额")
    private BigDecimal paidAmount;

    @ExcelProperty("未收余额")
    private BigDecimal balance;

    @ExcelProperty("状态")
    private String statusLabel;
}
