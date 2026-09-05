package com.erp.shipment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.shipment.entity.Shipment;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 出货单数据访问。
 */
public interface ShipmentMapper extends BaseMapper<Shipment> {

    /**
     * 查询指定订单的出货记录，按订单 ID 分组返回时取最新一条即可。
     */
    List<Shipment> selectByOrderIds(@Param("orderIds") List<Integer> orderIds);

    /**
     * 查询指定日期区间内已确认发货的出货记录（月度出货量统计用）。
     */
    List<Shipment> selectConfirmedBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
