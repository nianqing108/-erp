package com.erp.customer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.BusinessException;
import com.erp.common.MoneyUtil;
import com.erp.common.PageResult;
import com.erp.customer.dto.CustomerQueryDTO;
import com.erp.customer.dto.CustomerSaveDTO;
import com.erp.customer.entity.Customer;
import com.erp.customer.mapper.CustomerMapper;
import com.erp.order.entity.Order;
import com.erp.order.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

/**
 * 客户业务服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerMapper customerMapper;
    private final OrderMapper orderMapper;

    /**
     * 分页查询客户（含实时欠款与订单数）。
     */
    public PageResult<Customer> page(CustomerQueryDTO query) {
        int pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null || query.getPageSize() < 1 ? 20 : query.getPageSize();
        String keyword = StringUtils.hasText(query.getKeyword()) ? query.getKeyword().trim() : null;

        Page<Customer> page = customerMapper.selectPageWithDebt(
                new Page<>(pageNum, pageSize), keyword, query.getOnlyDebt());

        page.getRecords().forEach(this::fillDerivedFields);
        return PageResult.of(page);
    }

    /**
     * 下拉选择用：全量客户（不分页）。
     */
    public List<Customer> listAll() {
        return customerMapper.selectList(Wrappers.<Customer>lambdaQuery().orderByAsc(Customer::getName));
    }

    public Customer detail(Integer id) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null) {
            throw new BusinessException("客户不存在");
        }
        Customer stat = customerMapper.selectDebtByCustomerId(id);
        if (stat != null) {
            customer.setDebtAmount(stat.getDebtAmount());
            customer.setOrderCount(stat.getOrderCount());
        }
        fillDerivedFields(customer);
        return customer;
    }

    @Transactional
    public Integer create(CustomerSaveDTO dto) {
        assertNameUnique(dto.getName(), null);
        Customer customer = new Customer();
        applyDto(customer, dto);
        customerMapper.insert(customer);
        log.info("新增客户：id={}, name={}", customer.getId(), customer.getName());
        return customer.getId();
    }

    @Transactional
    public void update(Integer id, CustomerSaveDTO dto) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null) {
            throw new BusinessException("客户不存在");
        }
        assertNameUnique(dto.getName(), id);
        applyDto(customer, dto);
        customerMapper.updateById(customer);
        log.info("修改客户：id={}, name={}", id, customer.getName());
    }

    /**
     * 删除客户：已产生业务数据的客户禁止删除，避免账目失去归属。
     */
    @Transactional
    public void delete(Integer id) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null) {
            throw new BusinessException("客户不存在");
        }
        Long orderCount = orderMapper.selectCount(Wrappers.<Order>lambdaQuery().eq(Order::getCustomerId, id));
        if (orderCount != null && orderCount > 0) {
            throw new BusinessException("该客户已存在 " + orderCount + " 笔订单，不能删除");
        }
        customerMapper.deleteById(id);
        log.info("删除客户：id={}, name={}", id, customer.getName());
    }

    private void assertNameUnique(String name, Integer excludeId) {
        LambdaQueryWrapper<Customer> wrapper = Wrappers.<Customer>lambdaQuery().eq(Customer::getName, name.trim());
        if (excludeId != null) {
            wrapper.ne(Customer::getId, excludeId);
        }
        if (customerMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("公司名称「" + name + "」已存在");
        }
    }

    private void applyDto(Customer customer, CustomerSaveDTO dto) {
        customer.setName(dto.getName().trim());
        customer.setContact(dto.getContact());
        customer.setPhone(dto.getPhone());
        customer.setCreditLimit(MoneyUtil.norm(dto.getCreditLimit()));
        customer.setRemark(dto.getRemark());
    }

    /**
     * 派生字段：欠款非负归一、超额标记。
     *
     * <p>仅当设置了信用额度（&gt;0）时才做超额判定，未设额度的客户不标记。
     */
    private void fillDerivedFields(Customer customer) {
        customer.setDebtAmount(MoneyUtil.nonNegative(customer.getDebtAmount()));
        if (customer.getOrderCount() == null) {
            customer.setOrderCount(0);
        }
        BigDecimal creditLimit = MoneyUtil.norm(customer.getCreditLimit());
        customer.setOverCredit(MoneyUtil.isPositive(creditLimit)
                && MoneyUtil.gt(customer.getDebtAmount(), creditLimit));
    }
}
