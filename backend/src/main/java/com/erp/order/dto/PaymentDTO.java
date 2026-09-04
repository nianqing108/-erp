package com.erp.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 录入收款入参。
 *
 * <p>支持分批多次收款；累计收款不得超过订单总额（超收校验在 Service 层）。
 */
@Data
public class PaymentDTO {

    @NotNull(message = "收款金额不能为空")
    @DecimalMin(value = "0.01", message = "收款金额必须大于 0")
    private BigDecimal amount;

    @NotNull(message = "到账日不能为空")
    private LocalDate receivedDate;

    /** 备注（银行回单号等） */
    @Size(max = 255, message = "备注长度不能超过 255 个字符")
    private String note;
}
