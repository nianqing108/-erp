package com.erp.order.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 生成出货单入参：draft → pending，此处登记的是<b>计划发货日</b>。
 */
@Data
public class ShipDTO {

    @NotNull(message = "计划发货日不能为空")
    private LocalDate shipmentDate;

    @Size(max = 50, message = "物流单号长度不能超过 50 个字符")
    private String trackingNo;

    @Size(max = 255, message = "备注长度不能超过 255 个字符")
    private String remark;
}
