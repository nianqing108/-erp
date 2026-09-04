package com.erp.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.order.dto.OrderQueryDTO;
import com.erp.order.entity.Order;
import com.erp.order.vo.DebtRowVO;
import com.erp.order.vo.OrderVO;
import com.erp.order.vo.StatusAmountVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单数据访问。
 */
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 订单分页列表（含客户名与已收金额）。
     *
     * <p>模糊匹配字段由调用方拼好 % 通配符后传入，避免依赖数据库字符串函数，
     * 保证 MySQL / H2 行为一致。
     */
    Page<OrderVO> selectOrderPage(Page<OrderVO> page, @Param("q") OrderQueryDTO q);

    /**
     * 查询已发货订单的应收行（欠款统计与账龄分布的统一数据源）。
     */
    List<DebtRowVO> selectShippedDebtRows();

    /**
     * 按状态聚合订单金额与笔数。
     */
    List<StatusAmountVO> selectAmountGroupByStatus();

    /**
     * 带状态前置条件的条件更新：仅当当前状态与 expected 一致时才更新为目标状态。
     *
     * <p>用于并发场景下防止状态被覆盖（乐观式状态机），返回受影响行数。
     */
    int updateStatusCascade(@Param("id") Integer id,
                            @Param("expected") String expected,
                            @Param("target") String target);
}
