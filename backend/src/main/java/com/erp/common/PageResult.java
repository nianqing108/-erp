package com.erp.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * 统一分页响应体，屏蔽 MyBatis-Plus 分页对象对外的暴露。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<T> records;
    private long total;
    private long page;
    private long size;

    public static <T> PageResult<T> of(IPage<T> page) {
        return new PageResult<>(
                page.getRecords() == null ? Collections.emptyList() : page.getRecords(),
                page.getTotal(),
                page.getCurrent(),
                page.getSize()
        );
    }

    /**
     * 分页记录类型转换（Entity → VO）。
     */
    public static <S, T> PageResult<T> of(IPage<S> page, Function<S, T> converter) {
        List<T> records = page.getRecords() == null
                ? Collections.emptyList()
                : page.getRecords().stream().map(converter).toList();
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }
}
