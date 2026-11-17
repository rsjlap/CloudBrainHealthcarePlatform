package com.neusoft.cloudbrain.service;

import com.neusoft.cloudbrain.dto.request.prescription.PrescriptionItemRequest;
import com.neusoft.cloudbrain.entity.Drug;
import com.neusoft.cloudbrain.mapper.DrugMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DrugCatalogService 药品目录匹配")
class DrugCatalogServiceTest {

    @Mock
    private DrugMapper drugMapper;

    private DrugCatalogService drugCatalogService;

    @BeforeEach
    void setUp() {
        drugCatalogService = new DrugCatalogService(drugMapper);
    }

    @Test
    @DisplayName("正常：构建目�?Prompt 文本")
    void buildCatalogPromptText_withDrugs() {
        when(drugMapper.selectList(any())).thenReturn(List.of(activeDrug(1L, "D001", "阿莫西林")));

        String text = drugCatalogService.buildCatalogPromptText();

        assertTrue(text.contains("阿莫西林"));
        assertTrue(text.contains("D001"));
    }

    @Test
    @DisplayName("边界：空目录提示")
    void buildCatalogPromptText_empty() {
        when(drugMapper.selectList(any())).thenReturn(List.of());

        String text = drugCatalogService.buildCatalogPromptText();

        assertTrue(text.contains("无在售药�?));
    }

    @Test
    @DisplayName("正常：按 ID 获取在售药品")
    void getActiveDrug_found() {
        Drug drug = activeDrug(1L, "D001", "阿莫西林");
        when(drugMapper.selectById(1L)).thenReturn(drug);

        assertNotNull(drugCatalogService.getActiveDrug(1L));
    }

    @Test
    @DisplayName("边界：停用药品返�?null")
    void getActiveDrug_inactive() {
        Drug drug = activeDrug(1L, "D001", "阿莫西林");
        drug.setStatus(0);
        when(drugMapper.selectById(1L)).thenReturn(drug);

        assertNull(drugCatalogService.getActiveDrug(1L));
    }

    @Test
    @DisplayName("正常：按编码匹配�?enrich")
    void enrichRecommendedItems_byCode() {
        Drug drug = activeDrug(1L, "D001", "阿莫西林");
        when(drugMapper.selectList(any())).thenReturn(List.of(drug));

        PrescriptionItemRequest item = new PrescriptionItemRequest();
        item.setDrugCode("D001");
        drugCatalogService.enrichRecommendedItems(List.of(item));

        assertTrue(item.getCatalogMatched());
        assertEquals("阿莫西林", item.getDrugName());
    }

    @Test
    @DisplayName("正常：青霉素过敏冲突过滤")
    void recommendFromContext_allergyConflict() {
        Drug penicillin = activeDrug(1L, "D002", "阿莫西林");
        penicillin.setContraindication("青霉素过�?);
        when(drugMapper.selectList(any())).thenReturn(List.of(penicillin));

        List<PrescriptionItemRequest> result = drugCatalogService.recommendFromContext(
                "阿莫西林 感染", "青霉素过�?, 3);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("正常：retainCatalogMatched 仅保留匹配项")
    void retainCatalogMatched_filtersUnmatched() {
        Drug drug = activeDrug(1L, "D001", "布洛�?);
        when(drugMapper.selectList(any())).thenReturn(List.of(drug));

        PrescriptionItemRequest matched = new PrescriptionItemRequest();
        matched.setDrugCode("D001");
        PrescriptionItemRequest unmatched = new PrescriptionItemRequest();
        unmatched.setDrugName("未知药品XYZ");

        List<PrescriptionItemRequest> result = drugCatalogService.retainCatalogMatched(
                List.of(matched, unmatched));

        assertEquals(1, result.size());
        assertEquals("布洛�?, result.get(0).getDrugName());
    }

    @Test
    @DisplayName("正常：resolveForPrescription 优先�?drugId")
    void resolveForPrescription_byId() {
        Drug drug = activeDrug(5L, "D005", "头孢");
        when(drugMapper.selectById(5L)).thenReturn(drug);

        PrescriptionItemRequest req = new PrescriptionItemRequest();
        req.setDrugId(5L);

        Drug resolved = drugCatalogService.resolveForPrescription(req);

        assertEquals("头孢", resolved.getDrugName());
    }

    private static Drug activeDrug(Long id, String code, String name) {
        Drug drug = new Drug();
        drug.setId(id);
        drug.setDrugCode(code);
        drug.setDrugName(name);
        drug.setStatus(1);
        drug.setUnit("�?);
        drug.setUnitPrice(new BigDecimal("10"));
        drug.setStockQuantity(100);
        return drug;
    }
}
