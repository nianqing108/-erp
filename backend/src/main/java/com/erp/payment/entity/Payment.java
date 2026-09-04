package com.erp.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 收款记录实体（一个订单可有多笔，支持分批收款）。
 */
@Data
@TableName("payments")
public class Payment implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer orderId;

    private BigDecimal amount;

    private LocalDate receivedDate;

    /** 备注（银行回单号等） */
    private String note;

    private LocalDateTime createdAt;
}
