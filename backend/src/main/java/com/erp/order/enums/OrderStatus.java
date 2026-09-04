package com.erp.order.enums;

import com.erp.common.BusinessException;
import lombok.Getter;

import java.util.Arrays;

/**
 * 订单状态枚举。
 *
 * <p><b>设计意图</b>：将全部状态可用动作收敛为枚举方法，杜绝业务代码中散落
 * {@code if ("draft".equals(status))} 这类字符串比较，编译期即可约束，
 * 与数据库 ENUM / CHECK 约束形成双重保障。
 */
@Getter
public enum OrderStatus {

    /** 录入：刚录入，未排产 */
    DRAFT("draft", "录入"),

    /** 待出货：已生成出货单（计划发货），尚未实际发出 */
    PENDING("pending", "待出货"),

    /** 待付款：已实际发货，形成应收 */
    SHIPPED("shipped", "待付款"),

    /** 已完成：款项收齐 */
    PAID("paid", "已完成"),

    /** 已取消：作废，不参与任何欠款核算 */
    CANCELLED("cancelled", "已取消");

    private final String code;
    private final String label;

    OrderStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }

    /** 是否可生成出货单 */
    public boolean canShip() {
        return this == DRAFT;
    }

    /** 是否可确认发货 */
    public boolean canConfirmShip() {
        return this == PENDING;
    }

    /** 是否可录入收款 */
    public boolean canPay() {
        return this == SHIPPED;
    }

    /** 是否可取消 */
    public boolean canCancel() {
        return this == DRAFT || this == PENDING;
    }

    /** 是否可编辑核心字段 */
    public boolean canEdit() {
        return this == DRAFT;
    }

    /**
     * 是否计入应收欠款。
     *
     * <p>口径：仅「已实际发货（shipped）」形成应收；
     * draft 记为在录金额、pending 记为待发货金额，均不计入应收。
     */
    public boolean countAsDebt() {
        return this == SHIPPED;
    }

    /**
     * 解析状态编码，未知值抛业务异常。
     */
    public static OrderStatus of(String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException("订单状态不能为空");
        }
        return Arrays.stream(values())
                .filter(s -> s.code.equalsIgnoreCase(code.trim()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("未知的订单状态：" + code));
    }

    /**
     * 已知的状态编码集合，用于接口文档与前端字典。
     */
    public static String[] codes() {
        return Arrays.stream(values()).map(OrderStatus::getCode).toArray(String[]::new);
    }
}
