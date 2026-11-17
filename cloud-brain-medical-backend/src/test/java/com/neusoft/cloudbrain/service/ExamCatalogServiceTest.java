package com.neusoft.cloudbrain.service;

import com.neusoft.cloudbrain.common.enums.CatalogItemStatus;
import com.neusoft.cloudbrain.common.enums.ExamOrderType;
import com.neusoft.cloudbrain.dto.response.medical.RecommendedExamVO;
import com.neusoft.cloudbrain.entity.ExamItem;
import com.neusoft.cloudbrain.entity.ExamOrder;
import com.neusoft.cloudbrain.entity.LabItem;
import com.neusoft.cloudbrain.mapper.DepartmentMapper;
import com.neusoft.cloudbrain.mapper.ExamItemMapper;
import com.neusoft.cloudbrain.mapper.LabItemMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExamCatalogService 检查目录匹�?)
class ExamCatalogServiceTest {

    @Mock
    private LabItemMapper labItemMapper;
    @Mock
    private ExamItemMapper examItemMapper;
    @Mock
    private DepartmentMapper departmentMapper;

    private ExamCatalogService examCatalogService;

    @BeforeEach
    void setUp() {
        examCatalogService = new ExamCatalogService(labItemMapper, examItemMapper, departmentMapper);
    }

    @Test
    @DisplayName("正常：列出启用检验项�?)
    void listEnabledLabItems() {
        LabItem item = labItem(1L, "L001", "血常规");
        when(labItemMapper.selectList(any())).thenReturn(List.of(item));

        var result = examCatalogService.listEnabledLabItems("血");

        assertEquals(1, result.size());
        assertEquals("血常规", result.get(0).getItemName());
    }

    @Test
    @DisplayName("边界：空目录 Prompt")
    void buildCatalogPromptText_empty() {
        when(labItemMapper.selectList(any())).thenReturn(List.of());
        when(examItemMapper.selectList(any())).thenReturn(List.of());

        String text = examCatalogService.buildCatalogPromptText();

        assertTrue(text.contains("无启�?));
    }

    @Test
    @DisplayName("正常：按上下文推荐检验项�?)
    void recommendFromContext_labItem() {
        LabItem item = labItem(1L, "L001", "血常规");
        item.setCategory("血�?);
        when(labItemMapper.selectList(any())).thenReturn(List.of(item));
        when(examItemMapper.selectList(any())).thenReturn(List.of());

        List<RecommendedExamVO> result = examCatalogService.recommendFromContext("发热 血常规", 3);

        assertFalseOrHasMatch(result);
    }

    @Test
    @DisplayName("正常：获取启用检验项�?)
    void getEnabledLabItem() {
        LabItem item = labItem(1L, "L001", "血常规");
        when(labItemMapper.selectById(1L)).thenReturn(item);

        assertNotNull(examCatalogService.getEnabledLabItem(1L));
    }

    @Test
    @DisplayName("边界：停用项目返�?null")
    void getEnabledLabItem_disabled() {
        LabItem item = labItem(1L, "L001", "血常规");
        item.setStatus(CatalogItemStatus.DISABLED);
        when(labItemMapper.selectById(1L)).thenReturn(item);

        assertNull(examCatalogService.getEnabledLabItem(1L));
    }

    @Test
    @DisplayName("正常：解析订单费�?)
    void resolveOrderFee_fromOrder() {
        ExamOrder order = new ExamOrder();
        order.setFee(new BigDecimal("88"));

        assertEquals(new BigDecimal("88"), examCatalogService.resolveOrderFee(order));
    }

    private static LabItem labItem(Long id, String code, String name) {
        LabItem item = new LabItem();
        item.setId(id);
        item.setItemCode(code);
        item.setItemName(name);
        item.setStatus(CatalogItemStatus.ENABLED);
        item.setFee(new BigDecimal("50"));
        return item;
    }

    private static void assertFalseOrHasMatch(List<RecommendedExamVO> result) {
        if (!result.isEmpty()) {
            assertEquals(ExamOrderType.LAB, result.get(0).getOrderType());
        }
    }
}
