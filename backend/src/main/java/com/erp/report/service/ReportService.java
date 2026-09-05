package com.erp.report.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.erp.common.BusinessException;
import com.erp.common.MoneyUtil;
import com.erp.customer.entity.Customer;
import com.erp.customer.mapper.CustomerMapper;
import com.erp.order.entity.Order;
import com.erp.order.enums.OrderStatus;
import com.erp.order.mapper.OrderMapper;
import com.erp.payment.entity.Payment;
import com.erp.payment.mapper.PaymentMapper;
import com.erp.report.excel.MonthlyExcelVO;
import com.erp.report.excel.StatementExcelVO;
import com.erp.report.vo.MonthTrendVO;
import com.erp.report.vo.MonthlyReportVO;
import com.erp.report.vo.StatementOrderVO;
import com.erp.report.vo.StatementVO;
import com.erp.shipment.entity.Shipment;
import com.erp.shipment.mapper.ShipmentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * 报表服务：客户对账单、月度经营报表。
 *
 * <p>所有金额聚合在 Java 侧完成，SQL 仅负责过滤与按订单聚合收款，
 * 规避 MySQL 专有日期函数，保证 MySQL / H2 行为一致。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final OrderMapper orderMapper;
    private final PaymentMapper paymentMapper;
    private final ShipmentMapper shipmentMapper;
    private final CustomerMapper customerMapper;

    // ==================== 客户对账单 ====================

    /**
     * 生成客户对账单。
     *
     * <p>勾稽：期末欠款 = 期初欠款 + 本期发货额 − 本期回款额。
     */
    public StatementVO statement(Integer customerId, LocalDate from, LocalDate to) {
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw new BusinessException("客户不存在");
        }
        if (from == null || to == null) {
            throw new BusinessException("对账起止日期不能为空");
        }
        if (from.isAfter(to)) {
            throw new BusinessException("起始日期不能晚于结束日期");
        }
        LocalDate beforeFrom = from.minusDays(1);

        // 期初：发货日 < from 的已发/已完成订单
        List<StatementOrderVO> openingShipped = orderMapper.selectShippedByCustomer(customerId, null, beforeFrom);
        // 本期：发货日 in [from, to]
        List<StatementOrderVO> periodShipped = orderMapper.selectShippedByCustomer(customerId, from, to);

        // 期初订单在 from 之前的收款（用于扣减期初欠款）
        List<Payment> openingPayments = paymentMapper.selectByCustomerAndDateRange(customerId, null, beforeFrom);
        Map<Integer, BigDecimal> openingPaidMap = sumPaidByOrder(openingPayments);

        BigDecimal openingDebt = BigDecimal.ZERO;
        for (StatementOrderVO o : openingShipped) {
            BigDecimal total = MoneyUtil.norm(o.getTotalAmount());
            BigDecimal paid = MoneyUtil.nonNegative(openingPaidMap.get(o.getId()));
            openingDebt = MoneyUtil.add(openingDebt, MoneyUtil.nonNegative(MoneyUtil.subtract(total, paid)));
        }

        BigDecimal periodShippedAmount = periodShipped.stream()
                .map(o -> MoneyUtil.norm(o.getTotalAmount()))
                .reduce(BigDecimal.ZERO, MoneyUtil::add);

        // 本期回款（全部回款，含期初订单在本期的回款）
        List<Payment> periodPayments = paymentMapper.selectByCustomerAndDateRange(customerId, from, to);
        BigDecimal periodReceivedAmount = periodPayments.stream()
                .map(p -> MoneyUtil.norm(p.getAmount()))
                .reduce(BigDecimal.ZERO, MoneyUtil::add);

        BigDecimal closingDebt = MoneyUtil.nonNegative(MoneyUtil.subtract(
                MoneyUtil.add(openingDebt, periodShippedAmount), periodReceivedAmount));

        // 明细：本期发货订单 + 每单收款流水
        Set<Integer> periodOrderIds = periodShipped.stream().map(StatementOrderVO::getId).collect(Collectors.toSet());
        Map<Integer, List<Payment>> paymentsByOrder = periodOrderIds.isEmpty()
                ? Map.of()
                : groupPaymentsByOrder(paymentMapper.selectByOrderIds(new ArrayList<>(periodOrderIds)));

        List<StatementOrderVO> orders = new ArrayList<>(periodShipped.size());
        for (StatementOrderVO o : periodShipped) {
            List<Payment> ps = paymentsByOrder.getOrDefault(o.getId(), List.of());
            BigDecimal paid = ps.stream().map(p -> MoneyUtil.norm(p.getAmount()))
                    .reduce(BigDecimal.ZERO, MoneyUtil::add);
            o.setPayments(ps);
            o.setPaidAmount(paid);
            o.setBalance(MoneyUtil.nonNegative(MoneyUtil.subtract(MoneyUtil.norm(o.getTotalAmount()), paid)));
            o.setStatusLabel(OrderStatus.of(o.getStatus()).getLabel());
            orders.add(o);
        }

        // 本期其他回款：回款所属订单不在本期发货订单中（即前期发货订单的本期回款）
        List<Payment> otherPayments = periodPayments.stream()
                .filter(p -> !periodOrderIds.contains(p.getOrderId()))
                .collect(Collectors.toList());

        StatementVO vo = new StatementVO();
        vo.setCustomerId(customerId);
        vo.setCustomerName(customer.getName());
        vo.setFrom(from);
        vo.setTo(to);
        vo.setOpeningDebt(MoneyUtil.norm(openingDebt));
        vo.setPeriodShippedAmount(MoneyUtil.norm(periodShippedAmount));
        vo.setPeriodReceivedAmount(MoneyUtil.norm(periodReceivedAmount));
        vo.setClosingDebt(MoneyUtil.norm(closingDebt));
        vo.setOrders(orders);
        vo.setOtherPayments(otherPayments);
        return vo;
    }

    /**
     * 对账单转 Excel 行。
     */
    public List<StatementExcelVO> toStatementExcel(StatementVO vo) {
        List<StatementExcelVO> rows = new ArrayList<>(vo.getOrders().size());
        for (StatementOrderVO o : vo.getOrders()) {
            StatementExcelVO row = new StatementExcelVO();
            row.setOrderNo(o.getOrderNo());
            row.setCustomerOrderNo(o.getCustomerOrderNo());
            row.setOrderDate(o.getOrderDate());
            row.setShipmentDate(o.getShipmentDate());
            row.setTotalAmount(o.getTotalAmount());
            row.setPaidAmount(o.getPaidAmount());
            row.setBalance(o.getBalance());
            row.setStatusLabel(o.getStatusLabel());
            rows.add(row);
        }
        return rows;
    }

    // ==================== 月度经营报表 ====================

    /**
     * 月度经营报表（含近 12 月趋势）。
     */
    public MonthlyReportVO monthly(String month) {
        if (month == null || !month.matches("\\d{4}-\\d{2}")) {
            throw new BusinessException("月份格式应为 yyyy-MM，如 2026-09");
        }
        YearMonth ym = YearMonth.parse(month);
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();
        YearMonth start12 = ym.minusMonths(11);

        List<Order> monthOrders = orderMapper.selectList(
                Wrappers.<Order>lambdaQuery().between(Order::getOrderDate, from, to));
        long orderCount = monthOrders.stream()
                .filter(o -> !OrderStatus.CANCELLED.getCode().equals(o.getStatus()))
                .count();
        long completedCount = monthOrders.stream()
                .filter(o -> OrderStatus.PAID.getCode().equals(o.getStatus()))
                .count();
        long shipmentCount = shipmentMapper.selectConfirmedBetween(from, to).size();
        BigDecimal receivedAmount = MoneyUtil.norm(paymentMapper.sumBetween(from, to));
        BigDecimal completionRate = orderCount == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(completedCount).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(orderCount), 1, RoundingMode.HALF_UP);

        MonthlyReportVO vo = new MonthlyReportVO();
        vo.setMonth(month);
        vo.setOrderCount(orderCount);
        vo.setShipmentCount((long) shipmentCount);
        vo.setReceivedAmount(receivedAmount);
        vo.setCompletionRate(completionRate);
        vo.setTrend(trend(start12, ym));
        vo.setMonthOrders(monthOrderRows(monthOrders));
        return vo;
    }

    /**
     * 月度报表转 Excel 行（趋势明细）。
     */
    public List<MonthlyExcelVO> toMonthlyExcel(MonthlyReportVO vo) {
        return vo.getTrend().stream().map(t -> {
            MonthlyExcelVO row = new MonthlyExcelVO();
            row.setMonth(t.getMonth());
            row.setOrderCount(t.getOrderCount());
            row.setShipmentCount(t.getShipmentCount());
            row.setReceivedAmount(t.getReceivedAmount());
            row.setCompletionRate(t.getCompletionRate());
            return row;
        }).collect(Collectors.toList());
    }

    // ==================== 私有方法 ====================

    /**
     * 近 12 个月趋势：一次性取区间数据，在 Java 侧按 YearMonth 聚合。
     */
    private List<MonthTrendVO> trend(YearMonth start, YearMonth end) {
        LocalDate rangeFrom = start.atDay(1);
        LocalDate rangeTo = end.atEndOfMonth();

        List<Order> orders = orderMapper.selectList(
                Wrappers.<Order>lambdaQuery().between(Order::getOrderDate, rangeFrom, rangeTo));
        List<Shipment> shipments = shipmentMapper.selectConfirmedBetween(rangeFrom, rangeTo);
        List<Payment> payments = paymentMapper.selectBetween(rangeFrom, rangeTo);

        Map<YearMonth, List<Order>> ordersByMonth = bucketByOrderMonth(orders);
        Map<YearMonth, Long> shipmentByMonth = shipments.stream()
                .collect(Collectors.groupingBy(s -> YearMonth.from(s.getShipmentDate()),
                        Collectors.counting()));
        Map<YearMonth, BigDecimal> receivedByMonth = payments.stream()
                .collect(Collectors.groupingBy(p -> YearMonth.from(p.getReceivedDate()),
                        Collectors.reducing(BigDecimal.ZERO,
                                p -> MoneyUtil.norm(p.getAmount()), MoneyUtil::add)));

        List<MonthTrendVO> trend = new ArrayList<>();
        for (YearMonth m = start; !m.isAfter(end); m = m.plusMonths(1)) {
            List<Order> monthOs = ordersByMonth.getOrDefault(m, List.of());
            long orderCount = monthOs.stream()
                    .filter(o -> !OrderStatus.CANCELLED.getCode().equals(o.getStatus()))
                    .count();
            long completed = monthOs.stream()
                    .filter(o -> OrderStatus.PAID.getCode().equals(o.getStatus()))
                    .count();
            BigDecimal rate = orderCount == 0 ? BigDecimal.ZERO
                    : BigDecimal.valueOf(completed).multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(orderCount), 1, RoundingMode.HALF_UP);
            MonthTrendVO t = new MonthTrendVO();
            t.setMonth(m.toString());
            t.setOrderCount(orderCount);
            t.setShipmentCount(shipmentByMonth.getOrDefault(m, 0L));
            t.setReceivedAmount(MoneyUtil.norm(receivedByMonth.getOrDefault(m, BigDecimal.ZERO)));
            t.setCompletionRate(rate);
            trend.add(t);
        }
        return trend;
    }

    private Map<YearMonth, List<Order>> bucketByOrderMonth(List<Order> orders) {
        Map<YearMonth, List<Order>> map = new TreeMap<>();
        for (Order o : orders) {
            if (o.getOrderDate() == null) {
                continue;
            }
            map.computeIfAbsent(YearMonth.from(o.getOrderDate()), k -> new ArrayList<>()).add(o);
        }
        return map;
    }

    /**
     * 月度明细行：订单号、客户、金额、状态。
     */
    private List<Map<String, Object>> monthOrderRows(List<Order> orders) {
        Map<Integer, String> customerNames = new HashMap<>();
        List<Map<String, Object>> rows = new ArrayList<>(orders.size());
        for (Order o : orders) {
            String name = customerNames.computeIfAbsent(o.getCustomerId(), customerMapper::selectNameById);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("orderNo", o.getOrderNo());
            row.put("customerOrderNo", o.getCustomerOrderNo());
            row.put("customerName", name);
            row.put("orderDate", o.getOrderDate());
            row.put("totalAmount", o.getTotalAmount());
            row.put("status", o.getStatus());
            row.put("statusLabel", OrderStatus.of(o.getStatus()).getLabel());
            rows.add(row);
        }
        rows.sort(Comparator.comparing(r -> String.valueOf(r.get("orderNo"))));
        return rows;
    }

    private Map<Integer, BigDecimal> sumPaidByOrder(List<Payment> payments) {
        Map<Integer, BigDecimal> map = new HashMap<>();
        for (Payment p : payments) {
            map.merge(p.getOrderId(), MoneyUtil.norm(p.getAmount()), MoneyUtil::add);
        }
        return map;
    }

    private Map<Integer, List<Payment>> groupPaymentsByOrder(List<Payment> payments) {
        Map<Integer, List<Payment>> map = new HashMap<>();
        for (Payment p : payments) {
            map.computeIfAbsent(p.getOrderId(), k -> new ArrayList<>()).add(p);
        }
        return map;
    }
}
