package com.erp.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求。
 */
@Data
public class RegisterDTO {

    @NotBlank(message = "登录名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{3,32}$", message = "登录名须为 3-32 位字母、数字或下划线")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度须为 6-32 位")
    private String password;

    @Size(max = 50, message = "姓名最长 50 字")
    private String realName;

    /** 注册邀请码：服务端配置了 erp.auth.invite-code 时必填 */
    private String inviteCode;
}
