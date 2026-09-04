package com.erp.customer.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 客户新增 / 修改入参。
 */
@Data
public class CustomerSaveDTO {

    @NotBlank(message = "公司名称不能为空")
    @Size(max = 100, message = "公司名称长度不能超过 100 个字符")
    private String name;

    @Size(max = 50, message = "联系人长度不能超过 50 个字符")
    private String contact;

    @Pattern(regexp = "^[0-9+\\-()\\s]{0,20}$", message = "联系电话格式不正确")
    private String phone;

    @NotNull(message = "信用额度不能为空")
    @DecimalMin(value = "0.00", message = "信用额度不能为负数")
    private BigDecimal creditLimit;

    @Size(max = 255, message = "备注长度不能超过 255 个字符")
    private String remark;
}
