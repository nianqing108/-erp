package com.erp.order.vo;

import com.erp.payment.entity.Payment;
import com.erp.shipment.entity.Shipment;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 订单详情视图对象（含出货与收款明细）。
 */
@Data
public class OrderDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String orderNo;
    private String customerOrderNo;

    private Integer customerId;
    private String customerName;
    private String customerContact;
    private String customerPhone;

    private LocalDate orderDate;
    private BigDecimal totalAmount;
    private String status;
    private String statusLabel;
    private LocalDate expectedDelivery;
    private LocalDate dueDate;
    private String remark;

    /** 累计已收金额 */
    private BigDecimal paidAmount;
    /** 未收余额 */
    private BigDecimal balance;
    /** 回款比例（0 ~ 100） */
    private BigDecimal paidRatio;

    /** 出货信息，未生成出货单时为 null */
    private Shipment shipment;

    /** 收款流水，按到账日倒序 */
    private List<Payment> payments;

    /** 当前可执行动作，前端据此显隐按钮 */
    private List<String> availableActions;
}
