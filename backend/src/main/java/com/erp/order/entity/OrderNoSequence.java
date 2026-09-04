package com.erp.order.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 内部订单号按日流水序列表。
 */
@Data
@TableName("order_no_sequence")
public class OrderNoSequence implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 业务日期，主键 */
    @TableId
    private LocalDate bizDate;

    /** 当日已发放的最大流水号 */
    private Integer currentVal;

    private LocalDateTime updatedAt;
}
