package com.erp.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一接口响应体。
 *
 * <p>code 约定：200 成功 / 400 参数或业务校验失败 / 500 系统异常。
 */
@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private int code;
    private String msg;
    private T data;

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.msg = "success";
        r.data = data;
        return r;
    }

    /**
     * 业务校验失败（HTTP 语义上的参数/规则问题，统一使用 400）。
     */
    public static <T> Result<T> fail(String msg) {
        return fail(400, msg);
    }

    public static <T> Result<T> fail(int code, String msg) {
        Result<T> r = new Result<>();
        r.code = code;
        r.msg = msg;
        return r;
    }
}
