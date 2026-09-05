package com.erp.dashboard.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.common.MoneyUtil;
import com.erp.customer.entity.Customer;
import com.erp.customer.mapper.CustomerMapper;
import com.erp.dashboard.vo.AgingBucketVO;
import com.erp.dashboard.vo.DashboardVO;
import com.erp.order.entity.Order;
import com.erp.order.enums.OrderStatus;
import com.erp.order.mapper.OrderMapper;
import com.erp.order.vo.DebtRowVO;
import com.erp.order.vo.StatusAmountVO;
import com.erp.payment.mapper.PaymentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 仪表盘统计服务。
 *
 * <p>欠款口径：仅「已实际发货（shipped）」的未结余额计入应收欠款；
 * 在录金额（draft）单独展示，不混入应收。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    /** 欠款 Top 榜展示条数 */
    private static final int TOP_CUSTOMER_LIMIT = 10;

    /** 逾期账龄分档定义：编码 + 中文名（第一档为未到期，不参与逾期） */
    private static final List<String[]> AGING_BUCKETS = List.of(
            new String[]{"not-due", "未到期"},
            new String[]{"0-30", "逾期 0-30 天"},
            new String[]{"31-60", "逾期 31-60 天"},
            new String[]{"61-90", "逾期 61-90 天"},
            new String[]{"90+", "逾期 90 天以上"}
    );

    private final OrderMapper orderMapper;
    private final CustomerMapper customerMapper;
    private final PaymentMapper paymentMapper;

    /**
     * 仪表盘总览。
     */
    public DashboardVO overview() {
        LocalDate today = LocalDate.now();

        // 应收行：已发货未结清订单，欠款/账龄/到期金额共用同一数据源
        List<DebtRowVO> debtRows = orderMapper.selectShippedDebtRows();

        YearMonth currentMonth = YearMonth.from(today);
        BigDecimal receivable = BigDecimal.ZERO;
        BigDecimal monthDueAmount = BigDecimal.ZERO;
        BigDecimal notDueAmount = BigDecimal.ZERO;
        long receivableOrderCount = 0;
        for (DebtRowVO row : debtRows) {
            BigDecimal balance = MoneyUtil.nonNegative(row.getBalance());
            if (!MoneyUtil.isPositive(balance)) {
                continue;
            }
            receivable = MoneyUtil.add(receivable, balance);
            receivableOrderCount++;

            // 本月到期金额：约定收款日落在本月的未收余额（未填约定收款日的订单无法判定到期，不计入）
            if (row.getDueDate() != null && YearMonth.from(row.getDueDate()).equals(currentMonth)) {
                monthDueAmount = MoneyUtil.add(monthDueAmount, balance);
            }
            // 未到期金额：基准日（约定收款日优先，发货日兜底）尚未到达的未收余额
            LocalDate base = row.getDueDate() != null ? row.getDueDate() : row.getShipmentDate();
            if (base != null && base.isAfter(today)) {
                notDueAmount = MoneyUtil.add(notDueAmount, balance);
            }
        }

        Map<String, StatusAmountVO> byStatus = new LinkedHashMap<>();
        for (StatusAmountVO item : orderMapper.selectAmountGroupByStatus()) {
            byStatus.put(item.getStatus(), item);
        }

        // 本月订单额：本月下单（不含已取消）金额合计
        LocalDate monthStart = today.withDayOfMonth(1);
        BigDecimal monthOrderAmount = orderMapper.selectList(
                        Wrappers.<Order>lambdaQuery().between(Order::getOrderDate, monthStart, today))
                .stream()
                .filter(o -> !OrderStatus.CANCELLED.getCode().equals(o.getStatus()))
                .map(o -> MoneyUtil.norm(o.getTotalAmount()))
                .reduce(MoneyUtil.zero(), MoneyUtil::add);

        DashboardVO vo = new DashboardVO();
        vo.setDraftAmount(amountOf(byStatus, "draft"));
        vo.setMonthOrderAmount(MoneyUtil.norm(monthOrderAmount));
        vo.setReceivable(MoneyUtil.norm(receivable));
        vo.setMonthDueAmount(MoneyUtil.norm(monthDueAmount));
        vo.setNotDueAmount(MoneyUtil.norm(notDueAmount));
        vo.setReceivableOrderCount(receivableOrderCount);
        vo.setTotalPaid(MoneyUtil.norm(paymentMapper.sumAll()));
        vo.setMonthReceived(MoneyUtil.norm(
                paymentMapper.sumBetween(today.withDayOfMonth(1), today)));
        vo.setCustomerCount(customerMapper.selectCount(null));
        vo.setAging(bucketize(debtRows, today));
        vo.setTopDebtCustomers(customerMapper.selectTopDebt(TOP_CUSTOMER_LIMIT));
        vo.setStatDate(today);
        return vo;
    }

    /**
     * 逾期账龄分档：按<b>约定收款日</b>起算逾期天数，未填约定收款日的订单回退用实际发货日兜底。
     *
     * <p>约定收款日未到（days &lt; 0）归入「未到期」档，不参与逾期分档；
     * 各档金额合计恒等于应收欠款总额。在 Java 侧分档而非使用数据库日期函数，
     * 保证 MySQL / H2 行为一致且便于单测。
     */
    private List<AgingBucketVO> bucketize(List<DebtRowVO> rows, LocalDate today) {
        Map<String, BigDecimal> amounts = new LinkedHashMap<>();
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String[] bucket : AGING_BUCKETS) {
            amounts.put(bucket[0], MoneyUtil.zero());
            counts.put(bucket[0], 0L);
        }

        for (DebtRowVO row : rows) {
            BigDecimal balance = MoneyUtil.nonNegative(row.getBalance());
            if (!MoneyUtil.isPositive(balance)) {
                continue;
            }
            // 基准日优先级：约定收款日 > 实际发货日
            LocalDate base = row.getDueDate() != null ? row.getDueDate() : row.getShipmentDate();
            long days = 0;
            if (base != null) {
                days = ChronoUnit.DAYS.between(base, today);
            }
            String key = days < 0 ? "not-due" : bucketOf(days);
            amounts.merge(key, balance, MoneyUtil::add);
            counts.merge(key, 1L, Long::sum);
        }

        List<AgingBucketVO> result = new ArrayList<>(AGING_BUCKETS.size());
        for (String[] bucket : AGING_BUCKETS) {
            result.add(new AgingBucketVO(bucket[0], bucket[1],
                    MoneyUtil.norm(amounts.get(bucket[0])), counts.get(bucket[0])));
        }
        return result;
    }

    private String bucketOf(long overdueDays) {
        if (overdueDays <= 30) {
            return "0-30";
        }
        if (overdueDays <= 60) {
            return "31-60";
        }
        if (overdueDays <= 90) {
            return "61-90";
        }
        return "90+";
    }

    private BigDecimal amountOf(Map<String, StatusAmountVO> byStatus, String status) {
        StatusAmountVO item = byStatus.get(status);
        return item == null ? MoneyUtil.zero() : MoneyUtil.norm(item.getAmount());
    }
}
