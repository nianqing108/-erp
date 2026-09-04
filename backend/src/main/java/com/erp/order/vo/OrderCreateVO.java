package com.erp.order.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 订单创建结果：返回新订单主键与系统生成的内部订单号。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String orderNo;
}
