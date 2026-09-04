package com.erp.customer.controller;

import com.erp.common.PageResult;
import com.erp.common.Result;
import com.erp.customer.dto.CustomerQueryDTO;
import com.erp.customer.dto.CustomerSaveDTO;
import com.erp.customer.entity.Customer;
import com.erp.customer.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 客户接口。
 */
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Tag(name = "客户管理")
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    @Operation(summary = "客户分页列表", description = "支持关键字检索，返回实时应收欠款与有效订单数")
    public Result<PageResult<Customer>> page(@Valid CustomerQueryDTO query) {
        return Result.success(customerService.page(query));
    }

    @GetMapping("/all")
    @Operation(summary = "全量客户", description = "下拉选择用，不分页")
    public Result<List<Customer>> all() {
        return Result.success(customerService.listAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "客户详情")
    public Result<Customer> detail(@PathVariable Integer id) {
        return Result.success(customerService.detail(id));
    }

    @PostMapping
    @Operation(summary = "新增客户", description = "公司名称全局唯一")
    public Result<Integer> create(@RequestBody @Valid CustomerSaveDTO dto) {
        return Result.success(customerService.create(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "修改客户")
    public Result<Void> update(@PathVariable Integer id, @RequestBody @Valid CustomerSaveDTO dto) {
        customerService.update(id, dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除客户", description = "已产生订单的客户禁止删除")
    public Result<Void> delete(@PathVariable Integer id) {
        customerService.delete(id);
        return Result.success();
    }
}
