package com.erp.customer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 客户实体。
 */
@Data
@TableName("customers")
public class Customer implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 公司名称，全局唯一 */
    private String name;

    private String contact;

    private String phone;

    /** 信用额度 */
    private BigDecimal creditLimit;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /** 实时应收欠款（仅 shipped 未结清），联查统计字段，非数据库列 */
    @TableField(exist = false)
    private BigDecimal debtAmount;

    /** 有效订单数（不含已取消），联查统计字段，非数据库列 */
    @TableField(exist = false)
    private Integer orderCount;

    /** 是否超出信用额度，前端高亮用，非数据库列 */
    @TableField(exist = false)
    private Boolean overCredit;
}
