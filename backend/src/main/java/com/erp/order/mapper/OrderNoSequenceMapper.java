package com.erp.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.order.entity.OrderNoSequence;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;

/**
 * 订单号按日序列表数据访问。
 *
 * <p>使用标准 UPDATE / INSERT / SELECT 而非 {@code INSERT ... ON DUPLICATE KEY UPDATE}，
 * 保证 MySQL 与 H2 行为一致。自增由行锁保证原子性。
 */
public interface OrderNoSequenceMapper extends BaseMapper<OrderNoSequence> {

    /**
     * 已有记录时自增，返回受影响行数。
     */
    @Update("UPDATE order_no_sequence SET current_val = current_val + 1 WHERE biz_date = #{bizDate}")
    int increment(@Param("bizDate") LocalDate bizDate);

    /**
     * 首次发放：初始化当日流水为 1。
     */
    @Insert("INSERT INTO order_no_sequence (biz_date, current_val) VALUES (#{bizDate}, 1)")
    int init(@Param("bizDate") LocalDate bizDate);

    @Select("SELECT current_val FROM order_no_sequence WHERE biz_date = #{bizDate}")
    Integer selectValue(@Param("bizDate") LocalDate bizDate);
}
