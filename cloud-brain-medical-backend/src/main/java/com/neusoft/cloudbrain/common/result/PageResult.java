package com.neusoft.cloudbrain.common.result;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {

    private List<T> records;
    private long total;
    private long page;
    private long size;

    public static <T> PageResult<T> of(IPage<T> page) {
        return PageResult.<T>builder()
                .records(page.getRecords())
                .total(page.getTotal())
                .page(page.getCurrent())
                .size(page.getSize())
                .build();
    }

    public static <E, V> PageResult<V> of(IPage<E> page, List<V> records) {
        return PageResult.<V>builder()
                .records(records)
                .total(page.getTotal())
                .page(page.getCurrent())
                .size(page.getSize())
                .build();
    }
}
