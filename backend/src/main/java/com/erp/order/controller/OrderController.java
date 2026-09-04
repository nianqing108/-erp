package com.erp.order.controller;

import com.erp.common.PageResult;
import com.erp.common.Result;
import com.erp.order.dto.ConfirmShipDTO;
import com.erp.order.dto.OrderQueryDTO;
import com.erp.order.dto.OrderSaveDTO;
import com.erp.order.dto.PaymentDTO;
import com.erp.order.dto.ShipDTO;
import com.erp.order.entity.Order;
import com.erp.order.service.OrderService;
import com.erp.order.vo.OrderCreateVO;
import com.erp.order.vo.OrderDetailVO;
import com.erp.order.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单接口。
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "订单管理")
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    @Operation(summary = "订单分页列表", description = "支持状态、客户、订单号、客户订单号、下单日期区间筛选")
    public Result<PageResult<OrderVO>> page(@Valid OrderQueryDTO query) {
        return Result.success(orderService.page(query));
    }

    @GetMapping("/{id}")
    @Operation(summary = "订单详情", description = "含客户信息、出货信息、收款流水与可执行动作")
    public Result<OrderDetailVO> detail(@PathVariable Integer id) {
        return Result.success(orderService.detail(id));
    }

    @PostMapping
    @Operation(summary = "创建订单", description = "内部订单号由系统自动生成，初始状态为录入(draft)")
    public Result<OrderCreateVO> create(@RequestBody @Valid OrderSaveDTO dto) {
        Order order = orderService.create(dto);
        return Result.success(new OrderCreateVO(order.getId(), order.getOrderNo()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "编辑订单", description = "仅录入(draft)状态允许编辑核心字段")
    public Result<Void> update(@PathVariable Integer id, @RequestBody @Valid OrderSaveDTO dto) {
        orderService.update(id, dto);
        return Result.success();
    }

    @PostMapping("/{id}/ship")
    @Operation(summary = "生成出货单", description = "draft → pending，登记计划发货日")
    public Result<Void> ship(@PathVariable Integer id, @RequestBody @Valid ShipDTO dto) {
        orderService.ship(id, dto);
        return Result.success();
    }

    @PostMapping("/{id}/confirm-ship")
    @Operation(summary = "确认发货", description = "pending → shipped，登记实际发货日，此时形成应收")
    public Result<Void> confirmShip(@PathVariable Integer id, @RequestBody @Valid ConfirmShipDTO dto) {
        orderService.confirmShip(id, dto);
        return Result.success();
    }

    @PostMapping("/{id}/pay")
    @Operation(summary = "录入收款", description = "仅待付款订单可收款；支持分批；累计达额自动完成；禁止超收")
    public Result<Void> pay(@PathVariable Integer id, @RequestBody @Valid PaymentDTO dto) {
        orderService.pay(id, dto);
        return Result.success();
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消订单", description = "仅录入/待出货状态可取消；已发货、已完成不可取消")
    public Result<Void> cancel(@PathVariable Integer id) {
        orderService.cancel(id);
        return Result.success();
    }

    @GetMapping("/check-po")
    @Operation(summary = "校验客户订单号是否重复", description = "同一客户下重复时返回 true，仅作软提示不阻断录入")
    public Result<Boolean> checkPo(@RequestParam Integer customerId,
                                   @RequestParam String customerOrderNo,
                                   @RequestParam(required = false) Integer excludeOrderId) {
        return Result.success(orderService.isCustomerPoDuplicated(customerId, customerOrderNo, excludeOrderId));
    }
}
