package com.erp.order.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 确认发货入参：pending → shipped，此处登记的是<b>实际发货日</b>，
 * 该日期同时作为应收形成日与账龄起算日。
 */
@Data
public class ConfirmShipDTO {

    @NotNull(message = "实际发货日不能为空")
    private LocalDate shipmentDate;

    @Size(max = 50, message = "物流单号长度不能超过 50 个字符")
    private String trackingNo;

    @Size(max = 255, message = "备注长度不能超过 255 个字符")
    private String remark;
}
