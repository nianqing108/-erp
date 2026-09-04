package com.erp.shipment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 出货单实体（一个订单对应一条）。
 *
 * <p>{@code confirmed = 0} 时 {@code shipmentDate} 为计划发货日；
 * 确认发货后置 1，{@code shipmentDate} 更新为实际发货日。
 */
@Data
@TableName("shipments")
public class Shipment implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Integer id;

    private Integer orderId;

    private LocalDate shipmentDate;

    private String trackingNo;

    /** 是否已确认发货：0-计划发货 1-实际发货 */
    private Integer confirmed;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
