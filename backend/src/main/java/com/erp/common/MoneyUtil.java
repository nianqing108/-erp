package com.erp.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * 金额工具类：全链路金额计算必须经由本类，禁止使用 double / float。
 */
public final class MoneyUtil {

    /** 金额统一精度 */
    public static final int SCALE = 2;

    private MoneyUtil() {
    }

    public static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 归一化：null 视为 0，统一保留 2 位小数。
     */
    public static BigDecimal norm(BigDecimal value) {
        return value == null ? zero() : value.setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal add(BigDecimal a, BigDecimal b) {
        return norm(a).add(norm(b));
    }

    public static BigDecimal subtract(BigDecimal a, BigDecimal b) {
        return norm(a).subtract(norm(b));
    }

    /**
     * 判断是否大于 0（忽略精度差异）。
     */
    public static boolean isPositive(BigDecimal value) {
        return norm(value).compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 是否小于 0，用于识别超额收款导致的负数余额。
     */
    public static boolean isNegative(BigDecimal value) {
        return norm(value).compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * 取非负余额：超额收款场景下避免出现负欠款展示。
     */
    public static BigDecimal nonNegative(BigDecimal value) {
        BigDecimal v = norm(value);
        return v.compareTo(BigDecimal.ZERO) < 0 ? zero() : v;
    }

    public static boolean eq(BigDecimal a, BigDecimal b) {
        return norm(a).compareTo(norm(b)) == 0;
    }

    public static boolean gte(BigDecimal a, BigDecimal b) {
        return norm(a).compareTo(norm(b)) >= 0;
    }

    public static boolean gt(BigDecimal a, BigDecimal b) {
        return norm(a).compareTo(norm(b)) > 0;
    }

    /**
     * 金额相等比较（含 null 安全），用于「累计收款是否达到订单总额」判定。
     */
    public static boolean sameAmount(BigDecimal a, BigDecimal b) {
        return Objects.equals(norm(a), norm(b));
    }
}
