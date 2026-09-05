package com.erp.order.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.BusinessException;
import com.erp.common.MoneyUtil;
import com.erp.common.PageResult;
import com.erp.customer.entity.Customer;
import com.erp.customer.mapper.CustomerMapper;
import com.erp.order.dto.OrderQueryDTO;
import com.erp.order.dto.OrderSaveDTO;
import com.erp.order.dto.PaymentDTO;
import com.erp.order.dto.ShipDTO;
import com.erp.order.entity.Order;
import com.erp.order.enums.OrderStatus;
import com.erp.order.mapper.OrderMapper;
import com.erp.order.vo.OrderDetailVO;
import com.erp.order.vo.OrderVO;
import com.erp.payment.entity.Payment;
import com.erp.payment.mapper.PaymentMapper;
import com.erp.shipment.entity.Shipment;
import com.erp.shipment.mapper.ShipmentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单业务服务。
 *
 * <p><b>本类是唯一的事务边界</b>：所有状态流转、金额校验、关联记录写入均在此完成，
 * 禁止在 Controller 中组合调用多个 Service 完成一个业务动作。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final OrderNoGenerator orderNoGenerator;
    private final CustomerMapper customerMapper;
    private final ShipmentMapper shipmentMapper;
    private final PaymentMapper paymentMapper;

    // ==================== 查询 ====================

    public PageResult<OrderVO> page(OrderQueryDTO query) {
        normalize(query);
        int pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null || query.getPageSize() < 1 ? 20 : query.getPageSize();

        Page<OrderVO> page = orderMapper.selectOrderPage(new Page<>(pageNum, pageSize), query);
        page.getRecords().forEach(this::fillDerivedFields);
        return PageResult.of(page);
    }

    public OrderDetailVO detail(Integer id) {
        Order order = requireOrder(id);
        OrderStatus status = OrderStatus.of(order.getStatus());

        OrderDetailVO vo = new OrderDetailVO();
        BeanUtils.copyProperties(order, vo);
        vo.setStatusLabel(status.getLabel());

        Customer customer = customerMapper.selectById(order.getCustomerId());
        if (customer != null) {
            vo.setCustomerName(customer.getName());
            vo.setCustomerContact(customer.getContact());
            vo.setCustomerPhone(customer.getPhone());
        }

        BigDecimal total = MoneyUtil.norm(order.getTotalAmount());
        BigDecimal paid = MoneyUtil.norm(paymentMapper.sumByOrderId(id));
        vo.setPaidAmount(paid);
        vo.setBalance(MoneyUtil.nonNegative(MoneyUtil.subtract(total, paid)));
        vo.setPaidRatio(ratio(paid, total));

        vo.setShipment(findShipment(id));
        vo.setPayments(paymentMapper.selectByOrderIds(List.of(id)));
        vo.setAvailableActions(availableActions(status));
        return vo;
    }

    /**
     * 校验同一客户下客户订单号是否重复（软提示，不阻断录入）。
     */
    public boolean isCustomerPoDuplicated(Integer customerId, String customerOrderNo, Integer excludeOrderId) {
        if (customerId == null || !StringUtils.hasText(customerOrderNo)) {
            return false;
        }
        var wrapper = Wrappers.<Order>lambdaQuery()
                .eq(Order::getCustomerId, customerId)
                .eq(Order::getCustomerOrderNo, customerOrderNo.trim());
        if (excludeOrderId != null) {
            wrapper.ne(Order::getId, excludeOrderId);
        }
        return orderMapper.selectCount(wrapper) > 0;
    }

    // ==================== 写操作 ====================

    /**
     * 创建订单：内部订单号自动生成，初始状态为 draft。
     */
    @Transactional
    public Order create(OrderSaveDTO dto) {
        Customer customer = requireCustomer(dto.getCustomerId());
        validateAmount(dto.getTotalAmount());
        validateOrderDate(dto.getOrderDate());

        Order order = new Order();
        order.setOrderNo(orderNoGenerator.next());
        order.setCustomerOrderNo(trimToNull(dto.getCustomerOrderNo()));
        order.setCustomerId(customer.getId());
        order.setOrderDate(dto.getOrderDate());
        order.setTotalAmount(MoneyUtil.norm(dto.getTotalAmount()));
        order.setStatus(OrderStatus.DRAFT.getCode());
        order.setExpectedDelivery(dto.getExpectedDelivery());
        order.setDueDate(dto.getDueDate());
        order.setRemark(trimToNull(dto.getRemark()));
        orderMapper.insert(order);

        log.info("创建订单：orderNo={}, customerId={}, amount={}",
                order.getOrderNo(), order.getCustomerId(), order.getTotalAmount());
        return order;
    }

    /**
     * 编辑订单：仅 draft 状态允许修改核心字段。
     */
    @Transactional
    public void update(Integer id, OrderSaveDTO dto) {
        Order order = requireOrder(id);
        OrderStatus status = OrderStatus.of(order.getStatus());
        if (!status.canEdit()) {
            throw new BusinessException("仅「待发货」状态订单允许编辑，当前状态：" + status.getLabel());
        }
        requireCustomer(dto.getCustomerId());
        validateAmount(dto.getTotalAmount());
        validateOrderDate(dto.getOrderDate());

        Order update = new Order();
        update.setId(id);
        update.setCustomerId(dto.getCustomerId());
        update.setOrderDate(dto.getOrderDate());
        update.setTotalAmount(MoneyUtil.norm(dto.getTotalAmount()));
        update.setExpectedDelivery(dto.getExpectedDelivery());
        update.setDueDate(dto.getDueDate());
        update.setCustomerOrderNo(trimToNull(dto.getCustomerOrderNo()));
        update.setRemark(trimToNull(dto.getRemark()));
        orderMapper.updateById(update);

        log.info("编辑订单：orderNo={}", order.getOrderNo());
    }

    /**
     * 直接发货：draft → shipped，登记实际发货日与物流单号，此时形成应收。
     *
     * <p>流程已简化，不再有「计划发货 → 确认发货」两步；
     * PENDING 仅为历史数据兼容保留的枚举值。
     */
    @Transactional
    public void ship(Integer id, ShipDTO dto) {
        Order order = requireOrder(id);
        OrderStatus status = OrderStatus.of(order.getStatus());
        if (!status.canShip()) {
            throw new BusinessException("仅「待发货」状态订单可发货，当前状态：" + status.getLabel());
        }
        validateShipmentDate(dto.getShipmentDate(), order);

        Shipment shipment = findShipment(id);
        if (shipment == null) {
            shipment = new Shipment();
            shipment.setOrderId(id);
            shipment.setShipmentDate(dto.getShipmentDate());
            shipment.setTrackingNo(trimToNull(dto.getTrackingNo()));
            shipment.setRemark(trimToNull(dto.getRemark()));
            shipment.setConfirmed(1);
            shipmentMapper.insert(shipment);
        } else {
            // 历史数据兜底：该单已有出货记录时直接更新为实际发货
            shipment.setShipmentDate(dto.getShipmentDate());
            if (StringUtils.hasText(dto.getTrackingNo())) {
                shipment.setTrackingNo(dto.getTrackingNo().trim());
            }
            if (StringUtils.hasText(dto.getRemark())) {
                shipment.setRemark(dto.getRemark().trim());
            }
            shipment.setConfirmed(1);
            shipmentMapper.updateById(shipment);
        }

        changeStatus(order, status, OrderStatus.SHIPPED);
        log.info("订单 {} 已发货，实际发货日 {}", order.getOrderNo(), dto.getShipmentDate());
    }

    /**
     * 录入收款：仅 shipped 允许，支持分批多次；累计达额自动完成，禁止超收。
     */
    @Transactional
    public void pay(Integer id, PaymentDTO dto) {
        Order order = requireOrder(id);
        OrderStatus status = OrderStatus.of(order.getStatus());
        if (!status.canPay()) {
            throw new BusinessException("仅「待付款」状态订单可录入收款，当前状态：" + status.getLabel());
        }
        BigDecimal amount = MoneyUtil.norm(dto.getAmount());
        if (!MoneyUtil.isPositive(amount)) {
            throw new BusinessException("收款金额必须大于 0");
        }
        BigDecimal total = MoneyUtil.norm(order.getTotalAmount());
        BigDecimal paid = MoneyUtil.norm(paymentMapper.sumByOrderId(id));

        // 前置校验：给出友好提示
        if (MoneyUtil.gt(MoneyUtil.add(paid, amount), total)) {
            throw new BusinessException(String.format(
                    "累计收款不能超过订单总额 %.2f 元，已收 %.2f 元，本次最多可收 %.2f 元",
                    total, paid, MoneyUtil.subtract(total, paid)));
        }

        Payment payment = new Payment();
        payment.setOrderId(id);
        payment.setAmount(amount);
        payment.setReceivedDate(dto.getReceivedDate());
        payment.setNote(trimToNull(dto.getNote()));
        paymentMapper.insert(payment);

        // 后置复核：并发下二次确认，超限直接回滚事务
        BigDecimal newPaid = MoneyUtil.norm(paymentMapper.sumByOrderId(id));
        if (MoneyUtil.gt(newPaid, total)) {
            throw new BusinessException("累计收款超过订单总额，本次收款已撤销，请刷新后重试");
        }

        if (MoneyUtil.gte(newPaid, total)) {
            changeStatus(order, status, OrderStatus.PAID);
            log.info("订单 {} 收款 {} 元，已结清", order.getOrderNo(), amount);
        } else {
            log.info("订单 {} 收款 {} 元，剩余未收 {} 元",
                    order.getOrderNo(), amount, MoneyUtil.subtract(total, newPaid));
        }
    }

    /**
     * 取消订单：仅 draft / pending 允许；已取消不再参与欠款核算。
     */
    @Transactional
    public void cancel(Integer id) {
        Order order = requireOrder(id);
        OrderStatus status = OrderStatus.of(order.getStatus());
        if (!status.canCancel()) {
            throw new BusinessException("已发货或已完成订单不可取消，当前状态：" + status.getLabel());
        }
        changeStatus(order, status, OrderStatus.CANCELLED);
        log.info("订单 {} 已取消（原状态 {}）", order.getOrderNo(), status.getLabel());
    }

    // ==================== 私有方法 ====================

    /**
     * 状态流转唯一出口：使用带前置条件的条件更新，防止并发下状态被覆盖。
     */
    private void changeStatus(Order order, OrderStatus from, OrderStatus to) {
        int rows = orderMapper.updateStatusCascade(order.getId(), from.getCode(), to.getCode());
        if (rows == 0) {
            throw new BusinessException("订单状态已变更，请刷新后重试");
        }
        order.setStatus(to.getCode());
    }

    private Order requireOrder(Integer id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        return order;
    }

    private Customer requireCustomer(Integer customerId) {
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw new BusinessException("客户不存在");
        }
        return customer;
    }

    private Shipment findShipment(Integer orderId) {
        List<Shipment> shipments = shipmentMapper.selectList(
                Wrappers.<Shipment>lambdaQuery()
                        .eq(Shipment::getOrderId, orderId)
                        .orderByDesc(Shipment::getId));
        return shipments.isEmpty() ? null : shipments.get(0);
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || !MoneyUtil.isPositive(amount)) {
            throw new BusinessException("订单金额必须大于 0");
        }
    }

    private void validateOrderDate(LocalDate orderDate) {
        if (orderDate == null) {
            throw new BusinessException("下单日期不能为空");
        }
        if (orderDate.isAfter(LocalDate.now())) {
            throw new BusinessException("下单日期不能晚于今天");
        }
    }

    private void validateShipmentDate(LocalDate shipmentDate, Order order) {
        if (shipmentDate == null) {
            throw new BusinessException("发货日期不能为空");
        }
        if (order.getOrderDate() != null && shipmentDate.isBefore(order.getOrderDate())) {
            throw new BusinessException("发货日期不能早于下单日期 " + order.getOrderDate());
        }
    }

    /**
     * 回款比例（百分比，保留 1 位小数）。
     */
    private BigDecimal ratio(BigDecimal paid, BigDecimal total) {
        if (!MoneyUtil.isPositive(total)) {
            return BigDecimal.ZERO;
        }
        return MoneyUtil.nonNegative(MoneyUtil.norm(paid))
                .multiply(BigDecimal.valueOf(100))
                .divide(total, 1, RoundingMode.HALF_UP);
    }

    private void fillDerivedFields(OrderVO vo) {
        BigDecimal total = MoneyUtil.norm(vo.getTotalAmount());
        BigDecimal paid = MoneyUtil.nonNegative(vo.getPaidAmount());
        vo.setPaidAmount(paid);
        vo.setBalance(MoneyUtil.nonNegative(MoneyUtil.subtract(total, paid)));
        vo.setPaidRatio(ratio(paid, total));
        OrderStatus status = OrderStatus.of(vo.getStatus());
        vo.setStatusLabel(status.getLabel());
        vo.setAvailableActions(availableActions(status));
    }

    private List<String> availableActions(OrderStatus status) {
        List<String> actions = new ArrayList<>();
        if (status.canEdit()) {
            actions.add("edit");
        }
        if (status.canShip()) {
            actions.add("ship");
        }
        if (status.canPay()) {
            actions.add("pay");
        }
        if (status.canCancel()) {
            actions.add("cancel");
        }
        return actions;
    }

    /**
     * 模糊查询字段统一在此拼装 % 通配符，避免依赖数据库字符串拼接函数，
     * 保证 MySQL / H2 行为一致。
     */
    private void normalize(OrderQueryDTO q) {
        if (q == null) {
            return;
        }
        if (StringUtils.hasText(q.getOrderNo())) {
            q.setOrderNo("%" + q.getOrderNo().trim() + "%");
        }
        if (StringUtils.hasText(q.getCustomerOrderNo())) {
            q.setCustomerOrderNo("%" + q.getCustomerOrderNo().trim() + "%");
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
