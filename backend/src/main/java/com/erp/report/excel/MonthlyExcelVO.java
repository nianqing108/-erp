package com.erp.report.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 月度报表 Excel 行。
 */
@Data
public class MonthlyExcelVO {

    @ExcelProperty("月份")
    private String month;

    @ExcelProperty("订单量")
    private Long orderCount;

    @ExcelProperty("出货量")
    private Long shipmentCount;

    @ExcelProperty("回款金额")
    private BigDecimal receivedAmount;

    @ExcelProperty("完成率(%)")
    private BigDecimal completionRate;
}
