package com.erp.common;

/**
 * 未登录或登录态失效（响应体 code=401，前端据此跳转登录页）。
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
