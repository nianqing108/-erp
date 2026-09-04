package com.erp.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 订单实体。
 */
@Data
@TableName("orders")
public class Order implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 内部订单号：系统自动生成，不可手动修改 */
    private String orderNo;

    /** 客户订单号 / PO 号：选填，由客户提供，用于与客户对账 */
    private String customerOrderNo;

    private Integer customerId;

    private LocalDate orderDate;

    private BigDecimal totalAmount;

    /** 状态编码，取值见 {@link com.erp.order.enums.OrderStatus} */
    private String status;

    private LocalDate expectedDelivery;

    /** 约定付款到期日（选填，仅展示） */
    private LocalDate dueDate;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /** 客户名称，联查字段，非数据库列 */
    @TableField(exist = false)
    private String customerName;

    /** 累计已收金额，联查统计字段，非数据库列 */
    @TableField(exist = false)
    private BigDecimal paidAmount;
}
