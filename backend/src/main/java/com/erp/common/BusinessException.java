package com.erp.common;

/**
 * 业务异常：可预期的规则违反（状态非法、金额超限、数据重复等）。
 *
 * <p>由 {@link GlobalExceptionHandler} 捕获并以 WARN 级别记录、返回 code=400，
 * 不打印堆栈，避免日志噪音。
 */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
