package com.erp.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.customer.entity.Customer;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 客户数据访问。
 */
public interface CustomerMapper extends BaseMapper<Customer> {

    /**
     * 分页查询客户，附带应收欠款与有效订单数。
     *
     * <p>欠款口径：仅 status='shipped' 的「订单总额 − 已收金额」计入。
     */
    Page<Customer> selectPageWithDebt(Page<Customer> page,
                                      @Param("keyword") String keyword,
                                      @Param("onlyDebt") Boolean onlyDebt);

    /**
     * 按应收欠款倒序取前 N 名客户（仪表盘 Top 榜）。
     */
    List<Customer> selectTopDebt(@Param("limit") int limit);

    /**
     * 查询单个客户的应收欠款与有效订单数。
     */
    Customer selectDebtByCustomerId(@Param("customerId") Integer customerId);
}
