package com.neusoft.cloudbrain.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("PageUtil 分页参数规范化")
class PageUtilTest {

    @Test
    @DisplayName("正常：合法页码与页大小")
    void normalize_validValues() {
        assertEquals(2L, PageUtil.normalizePage(2L));
        assertEquals(20L, PageUtil.normalizeSize(20L));
    }

    @Test
    @DisplayName("边界：页码 null/0/负数 归一为 1")
    void normalizePage_boundary() {
        assertEquals(1L, PageUtil.normalizePage(null));
        assertEquals(1L, PageUtil.normalizePage(0L));
        assertEquals(1L, PageUtil.normalizePage(-5L));
    }

    @Test
    @DisplayName("边界：页大小 null/0 归一为 10，超过 100 截断为 100")
    void normalizeSize_boundary() {
        assertEquals(10L, PageUtil.normalizeSize(null));
        assertEquals(10L, PageUtil.normalizeSize(0L));
        assertEquals(100L, PageUtil.normalizeSize(101L));
        assertEquals(100L, PageUtil.normalizeSize(999L));
    }

    @Test
    @DisplayName("正常：of 组合分页对象")
    void of_createsPageWithNormalizedValues() {
        var page = PageUtil.of(null, 200L);
        assertEquals(1L, page.getCurrent());
        assertEquals(100L, page.getSize());
    }
}
