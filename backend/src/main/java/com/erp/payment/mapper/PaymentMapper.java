package com.erp.payment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.payment.entity.Payment;
import com.erp.payment.vo.OrderPaidVO;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    /**
     * 全部收款金额合计。
     */
    BigDecimal sumAll();

    /**
     * 指定到账日期区间内的收款金额合计（区间两端均包含，可为 null 表示不限）。
     */
    BigDecimal sumBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /**
     * 指定到账日期区间内的全部收款记录（区间两端均包含，可为 null 表示不限）。
     */
    List<Payment> selectBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /**
     * 指定客户在到账日期区间内的收款记录（对账单用）。
     */
    List<Payment> selectByCustomerAndDateRange(@Param("customerId") Integer customerId,
                                               @Param("from") LocalDate from,
                                               @Param("to") LocalDate to);
}
