package com.erp.auth.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户信息（不含密码）。
 */
@Data
public class UserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;

    private String username;

    private String realName;
}
