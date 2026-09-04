package com.erp.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.payment.entity.Payment;
import com.erp.payment.vo.OrderPaidVO;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 收款记录数据访问。
 */
public interface PaymentMapper extends BaseMapper<Payment> {

    /**
     * 按订单聚合已收金额（全量，供统计与报表复用）。
     */
    List<OrderPaidVO> selectSumGroupByOrder();

    /**
     * 某订单的累计已收金额。
     */
    BigDecimal sumByOrderId(@Param("orderId") Integer orderId);

    /**
     * 查询指定订单的全部收款流水，按到账日倒序。
     */
    List<Payment> selectByOrderIds(@Param("orderIds") List<Integer> orderIds);
}
