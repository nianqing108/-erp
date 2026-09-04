package com.erp.customer.dto;

import lombok.Data;

/**
 * 客户列表查询条件。
 */
@Data
public class CustomerQueryDTO {

    /** 关键字：公司名称 / 联系人 / 联系电话 模糊匹配 */
    private String keyword;

    /** 仅查询存在应收欠款的客户 */
    private Boolean onlyDebt;

    /** 页码，从 1 开始 */
    private Integer pageNum = 1;

    /** 每页条数 */
    private Integer pageSize = 20;
}
