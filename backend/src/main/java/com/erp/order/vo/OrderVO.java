package com.erp.order.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单列表视图对象。
 */
@Data
public class OrderVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String orderNo;
    private String customerOrderNo;
    private Integer customerId;
    private String customerName;
    private LocalDate orderDate;
    private BigDecimal totalAmount;
    private String status;
    /** 状态中文名，前端直出 */
    private String statusLabel;
    private LocalDate expectedDelivery;
    private LocalDate dueDate;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 累计已收金额 */
    private BigDecimal paidAmount;

    /** 未收余额 = 订单总额 − 已收金额 */
    private BigDecimal balance;

    /** 回款比例（0 ~ 100），前端进度条用 */
    private BigDecimal paidRatio;

    /** 当前可执行动作，前端据此显隐行内操作按钮 */
    private List<String> availableActions;
}
