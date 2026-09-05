package com.erp.dashboard.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 账龄分档。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgingBucketVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 分档编码：0-30 / 31-60 / 61-90 / 90+ */
    private String bucket;

    /** 分档中文名 */
    private String label;

    /** 该档位未结金额 */
    private BigDecimal amount;

    /** 该档位订单笔数 */
    private Long count;
}
