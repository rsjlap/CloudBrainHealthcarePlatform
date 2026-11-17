package com.neusoft.cloudbrain.common.util;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public final class PageUtil {

    private PageUtil() {
    }

    public static long normalizePage(Long page) {
        return page == null || page < 1 ? 1 : page;
    }

    public static long normalizeSize(Long size) {
        if (size == null || size < 1) {
            return 10;
        }
        return Math.min(size, 100);
    }

    public static <T> Page<T> of(Long page, Long size) {
        return new Page<>(normalizePage(page), normalizeSize(size));
    }
}
