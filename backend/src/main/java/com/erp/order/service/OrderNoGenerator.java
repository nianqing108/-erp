package com.erp.order.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.common.BusinessException;
import com.erp.order.entity.Order;
import com.erp.order.mapper.OrderMapper;
import com.erp.order.mapper.OrderNoSequenceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 内部订单号生成器。
 *
 * <p>规则：{@code 前缀 + yyyyMMdd + N 位当日流水}，如 {@code ORD202609040001}。
 *
 * <p><b>并发安全</b>：流水号取自序列表的行级自增（{@code UPDATE ... SET current_val = current_val + 1}），
 * 由数据库行锁保证原子性；再以 {@code orders.order_no} 唯一索引兜底，冲突时重试。
 *
 * <p>生成动作运行在创建订单的同一事务内，订单落库失败则流水随事务回滚。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderNoGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private final OrderNoSequenceMapper sequenceMapper;
    private final OrderMapper orderMapper;

    @Value("${erp.order-no.prefix:ORD}")
    private String prefix;

    @Value("${erp.order-no.seq-length:4}")
    private int seqLength;

    @Value("${erp.order-no.max-retry:3}")
    private int maxRetry;

    /**
     * 生成下一个订单号（使用当天业务日期）。
     */
    public String next() {
        return next(LocalDate.now());
    }

    /**
     * 生成指定业务日期的下一个订单号。
     */
    public String next(LocalDate bizDate) {
        LocalDate date = bizDate == null ? LocalDate.now() : bizDate;
        int retry = Math.max(1, maxRetry);
        for (int i = 1; i <= retry; i++) {
            String orderNo = generate(date);
            if (!exists(orderNo)) {
                return orderNo;
            }
            log.warn("订单号冲突，第 {} 次重试：{}", i, orderNo);
        }
        throw new BusinessException("订单号生成失败，请稍后重试");
    }

    private String generate(LocalDate date) {
        int seq = nextSeq(date);
        return prefix + date.format(DATE_FORMATTER) + String.format("%0" + seqLength + "d", seq);
    }

    /**
     * 取当日下一个流水号：先尝试自增，无记录则初始化为 1。
     */
    private int nextSeq(LocalDate date) {
        Integer val = tryIncrement(date);
        if (val != null) {
            return val;
        }
        try {
            sequenceMapper.init(date);
            return 1;
        } catch (DuplicateKeyException e) {
            // 并发场景下其他事务已初始化该日记录，回退为自增
            val = tryIncrement(date);
            if (val != null) {
                return val;
            }
        }
        throw new BusinessException("订单号生成失败，请稍后重试");
    }

    private Integer tryIncrement(LocalDate date) {
        if (sequenceMapper.increment(date) > 0) {
            return sequenceMapper.selectValue(date);
        }
        return null;
    }

    private boolean exists(String orderNo) {
        return orderMapper.selectCount(Wrappers.<Order>lambdaQuery().eq(Order::getOrderNo, orderNo)) > 0;
    }
}
